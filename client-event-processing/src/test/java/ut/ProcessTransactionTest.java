package ut;

import static org.acme.domain.TopologyProducer.CATEGORY_TOPIC;
import static org.acme.domain.TopologyProducer.OUT_A_TOPIC;
import static org.acme.domain.TopologyProducer.OUT_B_TOPIC;
import static org.acme.domain.TopologyProducer.TRANSACTION_TOPIC;
import static org.acme.domain.TopologyProducer.DLQ_TOPIC;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.acme.infra.events.Client;
import org.acme.infra.events.ClientCategory;
import org.acme.infra.events.ClientOutput;
import org.acme.infra.events.Person;
import org.acme.infra.events.TransactionEvent;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;


@QuarkusTest
@QuarkusTestResource(KafkaResource.class)
public class ProcessTransactionTest {
    
    KafkaProducer<Integer,ClientCategory> categoryProducer;
    KafkaProducer<String,TransactionEvent> transactionProducer;
    KafkaConsumer<String,ClientOutput> clientOutputConsumerA;
    KafkaConsumer<String,TransactionEvent> dlqConsumer;

    @BeforeAll
    public static void createTopic() throws InterruptedException, ExecutionException, TimeoutException{
        AdminClient adminClient = AdminClient.create(
            ImmutableMap.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaResource.getBootstrapServers())
        );
        List<NewTopic> topics = new ArrayList<>();
        topics.add(new NewTopic(CATEGORY_TOPIC, 1, (short) 1));
        topics.add(new NewTopic(TRANSACTION_TOPIC, 1, (short) 1));
        topics.add(new NewTopic(OUT_A_TOPIC, 1, (short) 1));
        topics.add(new NewTopic(OUT_B_TOPIC, 1, (short) 1));
        topics.add(new NewTopic(DLQ_TOPIC, 1, (short) 1));
        
        
        adminClient.createTopics(topics).all().get(10, TimeUnit.SECONDS);
    }

    @BeforeEach
    public void setup() {
        categoryProducer = new KafkaProducer<>(producerProps(),new IntegerSerializer(),new ObjectMapperSerializer<>());
        transactionProducer = new KafkaProducer<>(producerProps(),new StringSerializer(),new ObjectMapperSerializer<>());
        clientOutputConsumerA =  new KafkaConsumer(consumerProps(), new IntegerDeserializer(), new ObjectMapperDeserializer<>(ClientOutput.class));
        dlqConsumer = new KafkaConsumer<>(consumerProps(), new StringDeserializer(), new ObjectMapperDeserializer<>(TransactionEvent.class));
    }

    @AfterEach
    public void tearDown() {
        categoryProducer.close();
        transactionProducer.close();
        clientOutputConsumerA.close();
    }

    @Test
    public void shouldGetTransactionEnrichedAndTransformed(){
        sendCategories();
        clientOutputConsumerA.subscribe(Collections.singletonList(OUT_A_TOPIC));
        sendAtransaction(1);
        List<ConsumerRecord<String,ClientOutput>> results = poll(clientOutputConsumerA,1);
        Assertions.assertEquals("c001",results.get(0).value().client_code); 
    }


    private void sendCategories(){
        categoryProducer.send(new ProducerRecord<Integer,ClientCategory>(CATEGORY_TOPIC, 1, new ClientCategory(1,"Personal")));
        categoryProducer.send(new ProducerRecord<Integer,ClientCategory>(CATEGORY_TOPIC, 2, new ClientCategory(2,"VIP")));
        categoryProducer.send(new ProducerRecord<Integer,ClientCategory>(CATEGORY_TOPIC, 3, new ClientCategory(3,"Employee")));
        categoryProducer.send(new ProducerRecord<Integer,ClientCategory>(CATEGORY_TOPIC,4, new ClientCategory(4,"Business")));
    }

    private void sendAtransaction(int category) {

        Person p1 = new Person( 1, "Poo1", "Bilquees", "Kawoosa" );
        Client cl1 = new Client("c1","c001", p1, category);

        TransactionEvent tx1 = new TransactionEvent();
        tx1.txid = cl1.id;
        tx1.payload = cl1;
        tx1.type = TransactionEvent.TX_CLIENT_CREATED;
        transactionProducer.send(new ProducerRecord<String,TransactionEvent>(TRANSACTION_TOPIC, tx1.txid, tx1));

    }

    private Properties producerProps() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaResource.getBootstrapServers());
        return props;
    }

    private Properties consumerProps() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaResource.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-id");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }

    private List<ConsumerRecord<String,ClientOutput>> poll(Consumer<String,ClientOutput> consumer,int expectedRecordCount) {
        int fetched = 0;
        List<ConsumerRecord<String,ClientOutput>> outList = new ArrayList<>();
        while (fetched < expectedRecordCount) {
            ConsumerRecords<String,ClientOutput> records = consumer.poll(Duration.ofSeconds(4));
            records.forEach(outList::add);
            fetched = outList.size();
            System.out.print(".");
        }
        System.out.println(fetched);
        return outList;
    }
}
