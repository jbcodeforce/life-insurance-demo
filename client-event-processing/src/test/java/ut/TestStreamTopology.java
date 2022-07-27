package ut;

import java.util.Properties;

import javax.inject.Inject;

import org.acme.domain.TopologyProducer;
import org.acme.infra.events.Client;
import org.acme.infra.events.ClientCategory;
import org.acme.infra.events.ClientCategorySerdes;
import org.acme.infra.events.ClientOutput;
import org.acme.infra.events.ClientOutputSerdes;
import org.acme.infra.events.Person;
import org.acme.infra.events.TransactionEvent;
import org.acme.infra.events.TransactionSerdes;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
//@TestMethodOrder(OrderAnnotation.class)
public class TestStreamTopology {
    private static TopologyTestDriver testDriver;

    private TestInputTopic<String, TransactionEvent> inputTopic;
    private TestOutputTopic<String,TransactionEvent> dqlTopic;
    private TestInputTopic<Integer,ClientCategory> categoryTopic;
    private TestOutputTopic<String,ClientOutput> toATopic;
    private TestOutputTopic<String,ClientOutput> toBTopic;

    @Inject
    TopologyProducer topologyProducer;

    public  Properties getStreamsConfig() {
        final Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "client-event-processing");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummmy:1234");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        return props;
    }
    
    @BeforeEach
    public void setupTest(){
        Topology topology = topologyProducer.buildProcessFlow();
        testDriver = new TopologyTestDriver(topology, getStreamsConfig());
        inputTopic = testDriver.createInputTopic(TopologyProducer.TRANSACTION_TOPIC,   new StringSerializer(), TransactionSerdes.TransactionEventSerde().serializer());
        dqlTopic = testDriver.createOutputTopic(TopologyProducer.DLQ_TOPIC, new StringDeserializer(), TransactionSerdes.TransactionEventSerde().deserializer());
        categoryTopic = testDriver.createInputTopic(TopologyProducer.CATEGORY_TOPIC, new IntegerSerializer(), ClientCategorySerdes.ClientCategorySerde().serializer());
        toATopic = testDriver.createOutputTopic(TopologyProducer.OUT_A_TOPIC, new StringDeserializer(), ClientOutputSerdes.ClientOutputSerde().deserializer());
        toBTopic = testDriver.createOutputTopic(TopologyProducer.OUT_B_TOPIC, new StringDeserializer(), ClientOutputSerdes.ClientOutputSerde().deserializer());
    }

    @AfterEach
    public void tearDown() {
        try {
            testDriver.close();
        } catch (final Exception e) {
             System.out.println("Ignoring exception, test failing due this exception:" + e.getLocalizedMessage());
        } 
    }


    private TransactionEvent createATransaction(int category) {

        Person p1 = new Person( 1, "Poo1", "Bilquees", "Kawoosa" );
        Client cl1 = new Client("c1","c001", p1, category);

        TransactionEvent tx1 = new TransactionEvent();
        tx1.txid = cl1.id;
        tx1.payload = cl1;
        tx1.type = TransactionEvent.TX_CLIENT_CREATED;
        System.out.println(tx1.toString());
        return tx1;
    }
    
    private void sendCategories(){
        categoryTopic.pipeInput(1, new ClientCategory(1,"Personal"));
        categoryTopic.pipeInput(2, new ClientCategory(2,"VIP"));
        categoryTopic.pipeInput(3, new ClientCategory(3,"Employee"));
        categoryTopic.pipeInput(4, new ClientCategory(4,"Business"));
    }

    @Test
    public void shouldRouteBadTxToDLQ(){
        TransactionEvent txe = createATransaction(-1);
        inputTopic.pipeInput(txe.txid, txe);
        Assertions.assertFalse(dqlTopic.isEmpty());
        Assertions.assertEquals("c001",dqlTopic.readKeyValue().value.payload.code);
    }

    @Test
    public void shouldGetTransactionEnrichedAndTransformed(){
        sendCategories();
        TransactionEvent txe = createATransaction(1);
        inputTopic.pipeInput(txe.txid, txe);
        ClientOutput result = toATopic.readKeyValue().value;
        Assertions.assertEquals("c001",result.client_code); 
        Assertions.assertEquals("Personal",result.client_category_name); 
    }

    @Test
    public void shouldGetTransactionEnrichedAndTransformedRoutedToBsubscriber(){
        sendCategories();
        TransactionEvent txe = createATransaction(4);
        inputTopic.pipeInput(txe.txid, txe);
        ClientOutput result = toBTopic.readKeyValue().value;
        Assertions.assertEquals("c001",result.client_code); 
        Assertions.assertEquals("Business",result.client_category_name); 
    }


}
