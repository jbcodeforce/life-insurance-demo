package org.acme.domain;

import java.util.Map;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.acme.infra.events.Client;
import org.acme.infra.events.ClientCategory;
import org.acme.infra.events.ClientCategorySerdes;
import org.acme.infra.events.ClientOutput;
import org.acme.infra.events.ClientOutputSerdes;
import org.acme.infra.events.TransactionEvent;
import org.acme.infra.events.TransactionSerdes;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Branched;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.GlobalKTable;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueBytesStoreSupplier;
import org.apache.kafka.streams.state.Stores;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class TopologyProducer {
    private static final Logger LOG = Logger.getLogger(TopologyProducer.class.getName()); 
    public static String CATEGORY_STORE_NAME = "CategoryStore";
    public static String CATEGORY_TOPIC = "lf-categories";
    public static String TRANSACTION_TOPIC = "lf-raw-tx";
    public static String OUT_A_TOPIC = "lf-tx-a";
    public static String OUT_B_TOPIC = "lf-tx-b";
    public static String DLQ_TOPIC = "lf-dlq";

    @ConfigProperty(name="app.categories.topic")
    public String categoriesInputStreamName;

    @ConfigProperty(name="app.transactions.topic", defaultValue = "lf-transactions")
    public String transactionsInputStreamName;

    @ConfigProperty(name="app.transactions.fora.topic", defaultValue = "lf-tx-a")
    public String transactionsToAOutputStreamName;

    @ConfigProperty(name="app.transactions.forb.topic", defaultValue = "lf-tx-b")
    public String transactionsToBOutputStreamName;

    @ConfigProperty(name="app.transactions.dlq.topic", defaultValue = "lf-dlq")
    public String dlqOutputStreamName;

   
    public TopologyProducer(){
        LOG.info("TopologyProducer created produces to " + transactionsToAOutputStreamName + " or " + transactionsToBOutputStreamName);
    }

    /**
     * The streaming topology listens to two input streams and does
     * 1- get continuous update of the category reference data
     * 2- validate the data, and any transaction in error goes to dead letter queue
     * 3- Transform the input transaction hierarchical model into a flat model
     * 4- Enrich with the category name by doing a join with the categories table
     * 5- Route based on category name content to different target.
     * @return a Stream topology as bean to be processed by quarkus
     */
    @Produces
    public Topology buildProcessFlow() {
        final StreamsBuilder builder = new StreamsBuilder();
        final  Serde<ClientOutput> clientOutputSerder = ClientOutputSerdes.ClientOutputSerde();
        final  Serde<TransactionEvent> transactionEventSerder = TransactionSerdes.TransactionEventSerde();
        final  Serde<ClientCategory> categorySerder = ClientCategorySerdes.ClientCategorySerde();
        // Adding a state store is a simple matter of creating a StoreSupplier
        // instance with one of the static factory methods on the Stores class.
        // all persistent StateStore instances provide local storage using RocksDB
        KeyValueBytesStoreSupplier storeSupplier = Stores.persistentKeyValueStore(CATEGORY_STORE_NAME);
        // key: transaction_id, client transaction as value
        KStream<String, TransactionEvent> transactions = builder.stream(transactionsInputStreamName,
                                                        Consumed.with(Serdes.String(),  
                                                                     transactionEventSerder));
        transactions.peek((K,V) -> System.out.println("@@@@ " + V));

       
        // 1- Keep categories in a Table: key the category id and the value is the description of the category
        GlobalKTable<Integer, ClientCategory> categories = builder.globalTable(categoriesInputStreamName,
                                                        Consumed.with(Serdes.Integer(),  
                                                        categorySerder),  Materialized.as(storeSupplier));
       


        // 2- validate we have good data in the input transaction if not route to dlq
        Map<String, KStream<String, TransactionEvent>> branches = transactions
        .split(Named.as("A-"))
        .branch((k,v) -> transactionNotValid(v), Branched.as("error"))
        .defaultBranch(Branched.as("good-data"));
        
        // error records go to dead letter queue
        branches.get("A-error")
        .to(dlqOutputStreamName);
        // process good record
        Map<String, KStream<String, ClientOutput>> clientOut = branches.get("A-good-data")
        // 3-  Transform the input transaction hierarchical model into a flat model
        .mapValues(v ->  buildClientOutputFromTransaction(v))
        // 4- Enrich with the category name by doing a join with the categories table
        .join(categories,
            (txid,co) -> co.client_category_id,
            //When you join a stream and a table, you get a new stream
            (oldOutput,matchingCategory) -> new ClientOutput(oldOutput,matchingCategory.category_name)
        )  
        .selectKey( (k,v) -> v.client_id)
        .peek((K,V) -> System.out.println("@@@@ " + V))  
        .split(Named.as("B-"))
        // 5- Route based on category name content to different target
        .branch( (k,v) -> v.client_category_name != null && v.client_category_name.equals("Business"), Branched.as("category-b"))
        .defaultBranch(Branched.as("category-a"));

        clientOut.get("B-category-a").to(transactionsToAOutputStreamName,Produced.with(Serdes.String(), clientOutputSerder));
        clientOut.get("B-category-b").to(transactionsToBOutputStreamName,Produced.with(Serdes.String(), clientOutputSerder));
 
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
    
}

