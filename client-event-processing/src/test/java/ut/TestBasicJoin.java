package ut;

import java.util.Properties;

import org.acme.domain.ClientCategory;
import org.acme.infra.events.ClientDetailsSerdes;
import org.acme.infra.events.TransactionEventSerdes;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.Serdes;
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

public class TestBasicJoin {
    public static String TOPIC_LOOKUP = "topicForLookup";
    public static String IN_TOPIC = "firstTopic";
    public static String OUT_TOPIC = "outTopic";
    private  static TopologyTestDriver testDriver;
    private  static TestInputTopic<Integer, ClientCategory> inputTopic;
    private  static TestOutputTopic<Integer, ClientCategory> outputTopicA;
    private  static TestInputTopic<Integer, ClientCategory> categoriesTopic;
 
    public  static Properties getStreamsConfig() {
        final Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "kstream");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummmy:1234");
        return props;
    }

    public static Topology buildTopologyFlow(){
        final StreamsBuilder builder = new StreamsBuilder();
        KeyValueBytesStoreSupplier storeSupplier = Stores.persistentKeyValueStore("CategoryTable");

        KTable<Integer, ClientCategory> categories = builder.table(TOPIC_LOOKUP,
                                Consumed.with(Serdes.Integer(),  
                                TransactionEventSerdes.ClientCategorySerde()),  Materialized.as(storeSupplier));
        KStream<Integer, ClientCategory> inStream = builder.stream(IN_TOPIC, Consumed.with(Serdes.Integer(),  TransactionEventSerdes.ClientCategorySerde()));

        inStream.join(categories, (oldCategory,newCategory) -> new ClientCategory(oldCategory.id,newCategory.category_name))
        .to(OUT_TOPIC,Produced.with(Serdes.Integer(),TransactionEventSerdes.ClientCategorySerde()));
        return builder.build();  
    }

    @BeforeAll
    public  static void setup() {
        Topology topology = buildTopologyFlow();
        testDriver = new TopologyTestDriver(topology, getStreamsConfig());

        System.out.println(topology.describe());
        inputTopic = testDriver.createInputTopic(IN_TOPIC, new IntegerSerializer(), TransactionEventSerdes.ClientCategorySerde().serializer());
        categoriesTopic = testDriver.createInputTopic(TOPIC_LOOKUP, new IntegerSerializer(), TransactionEventSerdes.ClientCategorySerde().serializer());
        outputTopicA = testDriver.createOutputTopic(OUT_TOPIC, Serdes.Integer().deserializer(),  TransactionEventSerdes.ClientCategorySerde().deserializer());
    }

    @Test
    public void updateCategoryNameFromTable(){
        ClientCategory c1 = new ClientCategory(1, "Personal");
        categoriesTopic.pipeInput(c1.id,c1);
        ClientCategory c2 = new ClientCategory(2, "Business");
        categoriesTopic.pipeInput(c2.id,c2);
        ClientCategory c3 = new ClientCategory(3, "VIP");
        categoriesTopic.pipeInput(c3.id,c3);

        ClientCategory streamData1 = new ClientCategory(2, "OldValue");
        inputTopic.pipeInput(streamData1.id,streamData1);

        ClientCategory modifiedData = outputTopicA.readValue();
        Assertions.assertEquals("Business",modifiedData.category_name);
    }
}
