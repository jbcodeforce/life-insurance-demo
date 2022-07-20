package org.acme;

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
import org.apache.kafka.common.serialization.Serdes;
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
 * Filter out items with no store name or no sku
 */
//@QuarkusTest
@TestMethodOrder(OrderAnnotation.class)
public class TestTopologySecond {

    private  static TopologyTestDriver testDriver;
    //private  static TestInputTopic<String, ItemTransaction> inputTopic;
    private  static TestInputTopic<String, TransactionEvent> inputTopic;
    private  static TestInputTopic<Integer, ClientCategory> categoriesTopic;
    //private  static TestOutputTopic<String, ItemTransaction> outputTopic;
    private  static TestOutputTopic<String, ClientOutput> outputTopicA;
    private  static TestOutputTopic<String, ClientOutput> outputTopicB;

    private static TransactionProcessingAgent agent;
    
  

/**
 * 

        Map<String, KStream<String, ClientOutput>> branches = clients.split(Named.as("B-"))
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
    

    @BeforeAll
    public  static void setup() {
        agent = new TransactionProcessingAgent();
        Topology topology = agent.buildProcessFlow();
        testDriver = new TopologyTestDriver(topology, getStreamsConfig());

        System.out.println(topology.describe());
        testDriver = new TopologyTestDriver(topology, getStreamsConfig());
        inputTopic = testDriver.createInputTopic(agent.transactionsInputStreamName, new StringSerializer(), TransactionEventSerdes.TransactionSerde().serializer());
        outputTopicA = testDriver.createOutputTopic(agent.transactionsToAOutputStreamName, new StringDeserializer(),  ClientDetailsSerdes.ClientOutputSerde().deserializer());
        outputTopicB = testDriver.createOutputTopic(agent.transactionsToBOutputStreamName, new StringDeserializer(),  ClientDetailsSerdes.ClientOutputSerde().deserializer());

    }

    public  static Properties getStreamsConfig() {
        final Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "kstream-labs");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummmy:1234");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,   TransactionEventSerdes.TransactionSerde().getClass());
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


     @Test
     @Order(2)
     public void loadCategory() {
        ClientCategory c1 = new ClientCategory(1, "Person");
        categoriesTopic.pipeInput(c1.id,c1);
        ClientCategory c2 = new ClientCategory(2, "Business");
        categoriesTopic.pipeInput(c2.id,c2);
        assertThat(outputTopicA.isEmpty(), is(true));
        KeyValueStore<Integer,ValueAndTimestamp<ClientCategory>> store = testDriver.getTimestampedKeyValueStore(agent.CATEGORY_STORE_NAME);
        Assertions.assertNotNull(store);

        ValueAndTimestamp<ClientCategory> record1 = store.get(1);
        Assertions.assertNotNull(record1);
        Assertions.assertEquals("Person", record1.value());
        Assertions.assertEquals(2, store.approximateNumEntries());
     }


     @Test
     @Order(3)
     public void sendOneTransaction(){
         Person p = new Person( 1, "Poo1", "Bilquees", "Kawoosa" );
         Client c = new Client("c1","c001", p, 3);

         TransactionEvent tx = new TransactionEvent();
         tx.txid = c.id;
         tx.payload = c;
         inputTopic.pipeInput(tx.txid, tx);

         Person p1 = new Person( 2, "Poo2", "Jane", "Doe" );
         Client c1 = new Client("c2","c002", p1, 4);

         TransactionEvent tx2 = new TransactionEvent();
         tx2.txid = c1.id;
         tx2.payload = c1;
         inputTopic.pipeInput(tx2.txid, tx2);
/* 
         assertThat(outputTopicA.getQueueSize(), equalTo(1L) );
         ClientOutput filteredItem = outputTopicA.readValue();
         assertThat(filteredItem.client_category_name, equalTo("Business"));


        assertThat(outputTopicB.getQueueSize(), equalTo(1L) );
        filteredItem = outputTopicB.readValue();
        assertThat(filteredItem.client_category_name, equalTo("Personal"));
*/
         }

     /**
     @Test
     @Order(3)
     public void nullStoreNameRecordShouldGetNoOutputMessage() {
     Person p = new Person( 1, "Poo1", "Bilquees", "Kawoosa" );
     Client c = new ItemTransaction(null,"Item-1",ItemTransaction.RESTOCK,5,33.2);
     inputTopic.pipeInput(item.storeName, item);
     assertThat(outputTopic.isEmpty(), is(true));
     }

     @Test
     @Order(4)
     public void emptyStoreNameRecordShouldGetNoOutputMessage() {
     ItemTransaction item = new ItemTransaction("","Item-1",ItemTransaction.RESTOCK,5,33.2);
     inputTopic.pipeInput(item.storeName, item);
     assertThat(outputTopic.isEmpty(), is(true));
     }

     @Test
     @Order(5)
     public void nullSkuRecordShouldGetNoOutputMessage(){
     //assertThat(outputTopic.getQueueSize(), equalTo(0L) );

     ItemTransaction item = new ItemTransaction("Store-1",null,ItemTransaction.RESTOCK,5,33.2);
     inputTopic.pipeInput(item.storeName, item);
     assertThat(outputTopic.isEmpty(), is(true));
     }

     @Test
     @Order(6)
     public void emptySkuRecordShouldGetNoOutputMessage(){
     ItemTransaction item = new ItemTransaction("Store-1","",ItemTransaction.RESTOCK,5,33.2);
     inputTopic.pipeInput(item.storeName, item);
     assertThat(outputTopicA.isEmpty(), is(true));
     }**/

}
