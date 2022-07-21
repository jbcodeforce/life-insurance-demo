package ut;

import java.util.Properties;

import org.acme.domain.ClientCategory;
import org.acme.domain.TransactionProcessingAgent;
import org.acme.infra.events.Client;
import org.acme.infra.events.ClientDetailsSerdes;
import org.acme.infra.events.ClientOutput;
import org.acme.infra.events.Person;
import org.acme.infra.events.TransactionEvent;
import org.acme.infra.events.TransactionEventSerdes;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueBytesStoreSupplier;
import org.apache.kafka.streams.state.Stores;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestTopoCategoryJoin {
    private  static TopologyTestDriver testDriver;
    private  static TestInputTopic<String, TransactionEvent> inputTopic;
    private  static TestOutputTopic<Integer, ClientOutput> outputTopicA;
    private  static TestInputTopic<Integer, ClientCategory> categoriesTopic;
    private static TransactionProcessingAgent agent;
    
    public  static Properties getStreamsConfig() {
        final Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "kstream-labs");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummmy:1234");
       // props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.Integer().getClass());
        //props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,   TransactionEventSerdes.TransactionSerde().getClass());
        //props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, JSONSerde.class);
        return props;
    }

    public static Topology buildTopologyFlow(){
        final StreamsBuilder builder = new StreamsBuilder();    
        
        KeyValueBytesStoreSupplier storeSupplier = Stores.persistentKeyValueStore(TransactionProcessingAgent.CATEGORY_STORE_NAME);

        KTable<Integer, ClientCategory> categories = builder.table(agent.categoriesInputStreamName,
                                Consumed.with(Serdes.Integer(),  
                                TransactionEventSerdes.ClientCategorySerde()),  Materialized.as(storeSupplier));
                
        KStream<Integer, ClientOutput> inStream =builder.stream(agent.transactionsInputStreamName, Consumed.with(Serdes.String(),  TransactionEventSerdes.TransactionSerde()))
                .mapValues(transaction ->  agent.buildClientOutputFromTransaction(transaction))
                .selectKey( (k,v) -> v.client_category_id);

                inStream.join(categories,
                        //When you join a stream and a table, you get a new stream
                        //  The key of the result record is the same as for both joining input records
                        (oldOutput,matchingCategory) -> new ClientOutput(oldOutput,matchingCategory.category_name)
                )   
                .to(agent.transactionsToAOutputStreamName, Produced.with(Serdes.Integer(),ClientDetailsSerdes.ClientOutputSerde()));
        return builder.build();  
    }
    

    @BeforeAll
    public  static void setup() {
        agent = new TransactionProcessingAgent();
        agent.categoriesInputStreamName = "lf-categories";
        agent.transactionsInputStreamName = "lf-transactions";
        agent.transactionsToAOutputStreamName = "lf-tx-a";
        agent.transactionsToBOutputStreamName = "lf-tx-b";
        agent.dlqOutputStreamName = "lfdlq";
        Topology topology = buildTopologyFlow();
        testDriver = new TopologyTestDriver(topology, getStreamsConfig());

        System.out.println(topology.describe());
        inputTopic = testDriver.createInputTopic(agent.transactionsInputStreamName, new StringSerializer(), TransactionEventSerdes.TransactionSerde().serializer());
        categoriesTopic = testDriver.createInputTopic(agent.categoriesInputStreamName, new IntegerSerializer(), TransactionEventSerdes.ClientCategorySerde().serializer());
     
        outputTopicA = testDriver.createOutputTopic(agent.transactionsToAOutputStreamName, Serdes.Integer().deserializer(),  ClientDetailsSerdes.ClientOutputSerde().deserializer());
    }
    
    @Test
    public void changeKeyAndType(){
        ClientCategory c1 = new ClientCategory(1, "Personal");
        categoriesTopic.pipeInput(c1.id,c1);
        ClientCategory c2 = new ClientCategory(2, "Business");
        categoriesTopic.pipeInput(c2.id,c2);
        ClientCategory c3 = new ClientCategory(3, "VIP");
        categoriesTopic.pipeInput(c3.id,c3);


        Person p1 = new Person( 1, "Poo1", "Bilquees", "Kawoosa" );
        Client cl1 = new Client("c1","c001", p1, 2);

        TransactionEvent tx1 = new TransactionEvent();
        tx1.txid = cl1.id;
        tx1.payload = cl1;
        tx1.type = TransactionEvent.TX_CLIENT_CREATED;
        inputTopic.pipeInput(tx1.txid, tx1);

        ClientOutput clientOut = outputTopicA.readValue();
        Assertions.assertEquals("Kawoosa",clientOut.last_name );
        Assertions.assertEquals("Bilquees",clientOut.first_name);
        Assertions.assertEquals(2,clientOut.client_category_id);
        Assertions.assertEquals("Business",clientOut.client_category_name);
    }
}
