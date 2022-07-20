package org.acme.domain;

import javax.inject.Singleton;

import org.acme.infra.events.TransactionEvent;
import org.acme.infra.events.TransactionEventSerdes;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
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

        KStream<String, TransactionEvent> transactions = builder.stream(transactionsInputStreamName,
                                                        Consumed.with(Serdes.String(),  
                                                        TransactionEventSerdes.TransactionSerde()));

        KTable<String, ClientCategory> categories = builder.table(categoriesInputStreamName,
                                                        Consumed.with(Serdes.String(),  
                                                        TransactionEventSerdes.ClientCategorySerde()),  Materialized.as(storeSupplier));
        
        
        /* 
        KStream<String, ClientOutput> clientOut = transactions.flatMapValues(mapper);
        Map<String, KStream<String, TransactionEvent>> branches = transactions.split(Named.as("B-"))
        .branch((k,v) ->
                        (v.client_category_name != null && v.client_category_name.equals("Business")),
                Branched.as("categorya-tx")
        )
        .branch((k,v) ->
                        (v.client_category_name != null && v.client_category_name.equals("Personal")),
                Branched.as("categoryb-tx")
        ).defaultBranch(Branched.as("wrong-tx"));
            branches.get("B-categorya-tx").to(outTopicNameA);
            branches.get("B-categoryb-tx").to(outTopicNameB);
            branches.get("B-wrong-tx").to(deadLetterTopicName);
        */
        return builder.build();
    }
}

