package org.acme;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Map;
import java.util.Properties;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Branched;
import org.apache.kafka.streams.kstream.Named;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.acme.domain.*;
import org.acme.infra.*;
import org.junit.jupiter.api.Test;


/**
 * Filter out items with no store name or no sku
 */

public class TestTopologySecond {

    private  static TopologyTestDriver testDriver;
    //private  static TestInputTopic<String, ItemTransaction> inputTopic;
    private  static TestInputTopic<String, ClientOutput> inputTopic;
    //private  static TestOutputTopic<String, ItemTransaction> outputTopic;
    private  static TestOutputTopic<String, ClientOutput> outputTopicA;
    private  static TestOutputTopic<String, ClientOutput> outputTopicB;
    private static String inTopicName = "input-topic";
    private static String outTopicNameA = "lf-tx-a";
    private static String outTopicNameB = "lf-tx-b";
    private static String deadLetterTopicName = "lf-dlq";

    /**
     public static Topology buildTopologyFlow(){
     final StreamsBuilder builder = new StreamsBuilder();
     // 1- get the input stream
     KStream<Integer,Client> clients = builder.stream(c,
     Consumed.with(Serdes.Integer(),  ClientDetailsSerde.ClientSerde()));
     clients.peek((key, value) -> System.out.println("PRE-FILTER: key=" + key + ", value= {" + value + "}"))
     .filter((k,v) ->
     (Integer.valueOf(v.client_category_id) != null && v.client_category_id == 3))
     .peek((key, value) -> System.out.println("POST-FILTER: key=" + key + ", value= {" + value + "}"))
     .to(outTopicNameA);
     clients.filter((k,v) ->
     (Integer.valueOf(v.client_category_id) != null && v.client_category_id == 4))
     .peek((key, value) -> System.out.println("POST-FILTER: key=" + key + ", value= {" + value + "}"))
     .to(outTopicNameB);
     // 2 filter

     // Generate to output topic

     return builder.build();

     }**/

    public static Topology buildTopologyFlow() {

        final StreamsBuilder builder = new StreamsBuilder();
        // 1- get the input stream
        KStream<String, ClientOutput> clients = builder.stream(inTopicName,
                Consumed.with(Serdes.String(),  ClientDetailsSerdes.ClientOutputSerde()));
        // 2 build branches
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

        return builder.build();

    }

    @BeforeAll
    public static void setup() {
        Topology topology = buildTopologyFlow();
        System.out.println(topology.describe());
        testDriver = new TopologyTestDriver(topology, getStreamsConfig());
        inputTopic = testDriver.createInputTopic(inTopicName, new StringSerializer(), ClientDetailsSerdes.ClientOutputSerde().serializer());
        outputTopicA = testDriver.createOutputTopic(outTopicNameA, new StringDeserializer(),  ClientDetailsSerdes.ClientOutputSerde().deserializer());
        outputTopicB = testDriver.createOutputTopic(outTopicNameB, new StringDeserializer(),  ClientDetailsSerdes.ClientOutputSerde().deserializer());

    }

    public  static Properties getStreamsConfig() {
        final Properties props = new Properties();


        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "kstream-labs");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummmy:1234");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,   ClientDetailsSerdes.ClientOutputSerde().getClass());
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

     //@Test
     @Order(1)
     public void isEmpty() {
     assertThat(outputTopicA.isEmpty(), is(true));
     }

     //@Test
     @Order(2)
     public void sendValidRecord(){
         Person p = new Person( 1, "Poo1", "Bilquees", "Kawoosa" );
         Client c = new Client("c1","c001", p, 3);

         ClientOutput co = new ClientOutput(c, p);
         inputTopic.pipeInput(co.client_category_name, co);

         Person p1 = new Person( 2, "Poo2", "Jane", "Doe" );
         Client c1 = new Client("c2","c002", p1, 4);

         ClientOutput co1 = new ClientOutput(c1, p1);
         inputTopic.pipeInput(co1.client_category_name, co1);

         assertThat(outputTopicA.getQueueSize(), equalTo(1L) );
         ClientOutput filteredItem = outputTopicA.readValue();
         assertThat(filteredItem.client_category_name, equalTo("Business"));


        assertThat(outputTopicB.getQueueSize(), equalTo(1L) );
        filteredItem = outputTopicB.readValue();
        assertThat(filteredItem.client_category_name, equalTo("Personal"));

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
