package ut;

import java.util.Properties;

import org.acme.infra.events.Client;
import org.acme.infra.events.ClientCategory;
import org.acme.infra.events.ClientOutput;
import org.acme.infra.events.JSONSerde;
import org.acme.infra.events.Person;
import org.acme.infra.events.TransactionEvent;
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

import io.quarkus.kafka.client.serialization.ObjectMapperSerde;


public class TestTopoCategoryJoin {
    private  static TopologyTestDriver testDriver;
    private  static TestInputTopic<String, TransactionEvent> inputTopic;
    private  static TestOutputTopic<Integer, ClientOutput> outputTopicA;
    private  static TestInputTopic<Integer, ClientCategory> categoriesTopic;
    final static ObjectMapperSerde<ClientOutput> clientOutputSerder = new ObjectMapperSerde<ClientOutput>(ClientOutput.class);
    final static ObjectMapperSerde<TransactionEvent> transactionEventSerder = new ObjectMapperSerde<TransactionEvent>(TransactionEvent.class);
    final static ObjectMapperSerde<ClientCategory> categorySerder = new ObjectMapperSerde<ClientCategory>(ClientCategory.class);

    public  static Properties getStreamsConfig() {
        final Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "kstream-labs");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummmy:1234");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.Integer().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,  JSONSerde.class);
        return props;
    }

    public static Topology buildTopologyFlow(){
        final StreamsBuilder builder = new StreamsBuilder();    
        KeyValueBytesStoreSupplier storeSupplier = Stores.persistentKeyValueStore("Store");

        KTable<Integer, ClientCategory> categories = builder.table("categories",
                                Consumed.with(Serdes.Integer(),  
                                new JSONSerde<>()),  Materialized.as(storeSupplier));
                
        KStream<Integer, ClientOutput> inStream = builder.stream("transactions", Consumed.with(Serdes.String(), new JSONSerde<>()))
                .mapValues(v ->  {ClientOutput co = new ClientOutput();
                                TransactionEvent tx = (TransactionEvent)v;
                                if (tx.type.equals(TransactionEvent.TX_CLIENT_CREATED) || tx.type.equals(TransactionEvent.TX_CLIENT_UPDATED)) {
                                    Client c = (Client)tx.payload;
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
                                return co;}
                ).selectKey( (k,v) -> v.client_category_id);

                inStream.join(categories,
                        //When you join a stream and a table, you get a new stream
                        //  The key of the result record is the same as for both joining input records
                        (oldOutput,matchingCategory) -> new ClientOutput(oldOutput,matchingCategory.category_name)
                ) 
                .to("output", Produced.with(Serdes.Integer(),new JSONSerde<>()));
        return builder.build();  
    }
    

    @BeforeAll
    public  static void setup() {
        Topology topology = buildTopologyFlow();
        testDriver = new TopologyTestDriver(topology, getStreamsConfig());

        System.out.println(topology.describe());
        inputTopic = testDriver.createInputTopic("transactions", new StringSerializer(), transactionEventSerder.serializer());
        categoriesTopic = testDriver.createInputTopic("categories", new IntegerSerializer(), categorySerder.serializer());
     
        outputTopicA = testDriver.createOutputTopic("output", Serdes.Integer().deserializer(),  clientOutputSerder.deserializer());
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
