package org.acme.domain;

import java.util.Map;

import javax.inject.Singleton;

import org.acme.infra.events.Client;
import org.acme.infra.events.ClientOutput;
import org.acme.infra.events.TransactionEvent;
import org.acme.infra.events.TransactionEventSerdes;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Branched;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.ValueJoinerWithKey;
import org.apache.kafka.streams.state.KeyValueBytesStoreSupplier;
import org.apache.kafka.streams.state.Stores;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Singleton
public class TransactionProcessingAgent {
    public static String CATEGORY_STORE_NAME = "CategoryStore";

    @ConfigProperty(name="app.categories.topic", defaultValue = "lf-categories")
    public String categoriesInputStreamName;

    @ConfigProperty(name="app.transactions.topic", defaultValue = "lf-transactions")
    public String transactionsInputStreamName;

    @ConfigProperty(name="app.transactions.fora.topic", defaultValue = "lf-tx-a")
    public String transactionsToAOutputStreamName;

    @ConfigProperty(name="app.transactions.forb.topic", defaultValue = "lf-tx-b")
    public String transactionsToBOutputStreamName;

    @ConfigProperty(name="app.transactions.dlq.topic", defaultValue = "lf-dlq")
    public String dlqOutputStreamName;

    public Topology buildProcessFlow() {
        final StreamsBuilder builder = new StreamsBuilder();
        // Adding a state store is a simple matter of creating a StoreSupplier
        // instance with one of the static factory methods on the Stores class.
        // all persistent StateStore instances provide local storage using RocksDB
        KeyValueBytesStoreSupplier storeSupplier = Stores.persistentKeyValueStore(CATEGORY_STORE_NAME);
        CategoryClientJoiner joiner = new CategoryClientJoiner();
        // key: transaction_id, client transaction as value
        KStream<String, TransactionEvent> transactions = builder.stream(transactionsInputStreamName,
                                                        Consumed.with(Serdes.String(),  
                                                        TransactionEventSerdes.TransactionSerde()));

        // key the category id and the value is the description of the category
        KTable<Integer, ClientCategory> categories = builder.table(categoriesInputStreamName,
                                                        Consumed.with(Serdes.Integer(),  
                                                        TransactionEventSerdes.ClientCategorySerde()),  Materialized.as(storeSupplier));
        
        // First validate we have good data in the input transaction if not route to dlq
        Map<String, KStream<String, TransactionEvent>> branches = transactions.split(Named.as("A-"))
        .branch((k,v) -> transactionNotValid(v), Branched.as("error"))
        .defaultBranch(Branched.as("good-data"));
        
        // error records go to dead letter queue
        branches.get("A-error")
        .to(dlqOutputStreamName);

        // process good record
        Map<String, KStream<String, ClientOutput>> clientOut = branches.get("A-good-data")
        // transform to the target event model
        .mapValues(v ->  buildClientOutputFromTransaction(v) )
        // lookup the category id with the category table - so use the category.id as key
        .selectKey( (k,v) -> v.client_category_id)
        .leftJoin(categories,
              //When you join a stream and a table, you get a new stream
              (oldOutput,matchingCategory) -> new ClientOutput(oldOutput,matchingCategory.category_name)
            )
        .selectKey( (k,v) -> v.client_id)     
        .split(Named.as("B-"))
        .branch( (k,v) -> v.client_category_name != null && v.client_category_name.equals("Personal"), Branched.as("category-b"))
        .defaultBranch(Branched.as("category-a"));

        clientOut.get("B-category-a").to(transactionsToAOutputStreamName);
        clientOut.get("B-category-b").to(transactionsToBOutputStreamName);
        return builder.build();
    }


    public ClientOutput buildClientOutputFromTransaction(TransactionEvent v) {
        ClientOutput co = new ClientOutput();
        if (v.type.equals(TransactionEvent.TX_CLIENT_CREATED) || v.type.equals(TransactionEvent.TX_CLIENT_UPDATED)) {
            Client c = (Client)v.payload;
            co.client_code = c.code;
            co.client_id = c.id;
            co.client_category_id = c.client_category_id;
            co.email = c.insuredPerson.email;
            co.first_name = c.insuredPerson.first_name;
            co.last_name = c.insuredPerson.last_name;
            co.mobile = c.insuredPerson.mobile;
            co.phone = c.insuredPerson.phone;
            co.address=c.insuredPerson.address;
        }
        return co;
    }

    private boolean transactionNotValid(TransactionEvent v) {
        if (v.type == null) return false;
        if (v.type.equals(TransactionEvent.TX_CLIENT_CREATED) || v.type.equals(TransactionEvent.TX_CLIENT_UPDATED)) {
            Client c = (Client)v.payload;
            if (c.client_category_id <= 0) {
                return true;
            }
        }
        return false;
    }

    public class CategoryClientJoiner implements ValueJoinerWithKey<Integer, ClientOutput,ClientCategory,ClientOutput> {

        @Override
        public ClientOutput apply(Integer k, ClientOutput clientIN, ClientCategory category) {
            clientIN.client_category_name = category.category_name;
            return clientIN;
        }
        
    }
}

