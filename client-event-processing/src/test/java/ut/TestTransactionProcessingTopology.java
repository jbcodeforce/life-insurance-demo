package ut;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.ValueAndTimestamp;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;


/**
 * 
 */
@TestMethodOrder(OrderAnnotation.class)
public class TestTransactionProcessingTopology {

    private  static TopologyTestDriver testDriver;
    private  static TestInputTopic<String, TransactionEvent> inputTopic;
    private  static TestInputTopic<Integer, ClientCategory> categoriesTopic;

    private  static TestOutputTopic<String, ClientOutput> outputTopicA;
    private  static TestOutputTopic<String, TransactionEvent> dlqTopic;
    private static TransactionProcessingAgent agent;
    

   
    
    @BeforeAll
    public  static void setup() {
        agent = new TransactionProcessingAgent();
        agent.categoriesInputStreamName = "lf-categories";
        agent.transactionsInputStreamName = "lf-transactions";
        agent.transactionsToAOutputStreamName = "lf-tx-a";
        agent.dlqOutputStreamName = "lfdlq";
        Topology topology =  agent.buildProcessFlow();
        testDriver = new TopologyTestDriver(topology, getStreamsConfig());

        System.out.println(topology.describe());
        inputTopic = testDriver.createInputTopic(agent.transactionsInputStreamName, new StringSerializer(), TransactionEventSerdes.TransactionSerde().serializer());
        categoriesTopic = testDriver.createInputTopic(agent.categoriesInputStreamName, new IntegerSerializer(), TransactionEventSerdes.ClientCategorySerde().serializer());
       
        outputTopicA = testDriver.createOutputTopic(agent.transactionsToAOutputStreamName, new StringDeserializer(),  ClientDetailsSerdes.ClientOutputSerde().deserializer());
        dlqTopic = testDriver.createOutputTopic(agent.dlqOutputStreamName, new StringDeserializer(),  TransactionEventSerdes.TransactionSerde().deserializer());

    }

    public  static Properties getStreamsConfig() {
        final Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "kstream-labs");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummmy:1234");
        //props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
       // props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,   TransactionEventSerdes.TransactionSerde().getClass());
        return props;
    }

    @AfterAll
    public  static void tearDown() {
        try {
            testDriver.close();
        } catch (final Exception e) {
            System.out.println("Ignoring exception, test failing due this exception:" + e.getLocalizedMessage());
        }
    }

    /**
     * If we do not send message to the input topic there is no message to the output topic.
     */

     @Test
     @Order(1)
     public void isEmpty() {
        assertThat(outputTopicA.isEmpty(), is(true));
     }


     private void sendCategories(){
        ClientCategory c1 = new ClientCategory(1, "Personal");
        categoriesTopic.pipeInput(c1.id,c1);
        ClientCategory c2 = new ClientCategory(2, "Business");
        categoriesTopic.pipeInput(c2.id,c2);
     }

     @Test
     @Order(2)
     public void loadCategory() {
        sendCategories();
        assertThat(outputTopicA.isEmpty(), is(true));
        KeyValueStore<Integer,ValueAndTimestamp<ClientCategory>> store = testDriver.getTimestampedKeyValueStore(TransactionProcessingAgent.CATEGORY_STORE_NAME);
        Assertions.assertNotNull(store);

        ValueAndTimestamp<ClientCategory> record1 = store.get(Integer.valueOf(1));
        Assertions.assertNotNull(record1);
        Assertions.assertEquals("Personal", record1.value().category_name);
     }


     @Test
     @Order(3)
     public void shouldGetEnrichedAndTransformedTransaction() {
         sendCategories();
         Person p1 = new Person( 1, "Poo1", "Bilquees", "Kawoosa" );
         Client c1 = new Client("c1","c001", p1, 2);

         TransactionEvent tx1 = new TransactionEvent();
         tx1.txid = c1.id;
         tx1.payload = c1;
         tx1.type = TransactionEvent.TX_CLIENT_CREATED;
         inputTopic.pipeInput(tx1.txid, tx1);

         ClientOutput filteredItem = outputTopicA.readValue();
         Assertions.assertEquals("Bilquees",filteredItem.first_name);
    }

    @Test
    @Order(4)
    public void shouldHaveFaultyTransactionMovedToDLQ() {
        sendCategories();
        Person p2 = new Person( 2, "Poo2", "Jane", "Doe" );
        Client c2 = new Client("c2","c002", p2, -1);

        TransactionEvent tx2 = new TransactionEvent();
        tx2.txid = c2.id;
        tx2.payload = c2;
        tx2.type = TransactionEvent.TX_CLIENT_CREATED;
        inputTopic.pipeInput(tx2.txid, tx2);

        Assertions.assertEquals(1,dlqTopic.getQueueSize() );
        TransactionEvent txOut = dlqTopic.readValue();
        Assertions.assertEquals("c2",txOut.txid);
   }
  

}
