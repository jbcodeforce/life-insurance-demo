package ut;

import java.util.Properties;

import org.acme.domain.TransactionProcessingAgent;
import org.acme.infra.events.Client;
import org.acme.infra.events.ClientDetailsSerdes;
import org.acme.infra.events.ClientOutput;
import org.acme.infra.events.Person;
import org.acme.infra.events.TransactionEvent;
import org.acme.infra.events.TransactionEventSerdes;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestTopoToChangeDataAndKey {
    private  static TopologyTestDriver testDriver;
    private  static TestInputTopic<String, TransactionEvent> inputTopic;
    private  static TestOutputTopic<Integer, ClientOutput> outputTopicA;
    private static TransactionProcessingAgent agent;
    
    public  static Properties getStreamsConfig() {
        final Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "kstream-labs");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummmy:1234");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,   TransactionEventSerdes.TransactionSerde().getClass());
        return props;
    }

    public static Topology buildTopologyFlow(){
        final StreamsBuilder builder = new StreamsBuilder();     
        builder.stream(agent.transactionsInputStreamName, Consumed.with(Serdes.String(),  TransactionEventSerdes.TransactionSerde()))
                .mapValues(v ->  agent.buildClientOutputFromTransaction(v) )
                // lookup the category id with the category table - so use the category.id as key
                .selectKey( (k,v) -> v.client_category_id)
                .to(agent.transactionsToAOutputStreamName, Produced.with(Serdes.Integer(), ClientDetailsSerdes.ClientOutputSerde()));
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
       
        outputTopicA = testDriver.createOutputTopic(agent.transactionsToAOutputStreamName, new IntegerDeserializer(),  ClientDetailsSerdes.ClientOutputSerde().deserializer());
    }
    
    @Test
    public void changeKeyAndType(){
        Person p1 = new Person( 1, "Poo1", "Bilquees", "Kawoosa" );
        Client c1 = new Client("c1","c001", p1, 2);

        TransactionEvent tx1 = new TransactionEvent();
        tx1.txid = c1.id;
        tx1.payload = c1;
        tx1.type = TransactionEvent.TX_CLIENT_CREATED;
        inputTopic.pipeInput(tx1.txid, tx1);

        KeyValue<Integer,ClientOutput> clientOutKV = outputTopicA.readKeyValue();
        // the catergory id is now the key
        Assertions.assertEquals(2,clientOutKV.key);
        // data is transformed and mapped
        Assertions.assertEquals("Kawoosa",clientOutKV.value.last_name );
        Assertions.assertEquals("Bilquees",clientOutKV.value.first_name);
    }
}
