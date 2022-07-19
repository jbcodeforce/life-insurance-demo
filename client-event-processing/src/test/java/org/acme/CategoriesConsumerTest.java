package org.acme;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.UUID;

import javax.inject.Inject;


import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.common.annotation.Identifier;
import org.acme.domain.ClientCategory;
import org.acme.domain.*;


@QuarkusTest
public class CategoriesConsumerTest {

    @Inject
    @Identifier("default-kafka-broker")
    Map<String, Object> kafkaConfig;

    Map<Integer, String> clientCategoriesMap = new HashMap<>();

    KafkaConsumer<String, ClientCategory> quoteConsumer;
    KafkaProducer<String, ClientCategory> quoteRequestProducer;

    @BeforeEach
    void setUp() {
       quoteConsumer = new KafkaConsumer<>(consumerConfig(), new StringDeserializer(), new ObjectMapperDeserializer(ClientCategory.class));
       quoteRequestProducer = new KafkaProducer<>(kafkaConfig, new StringSerializer(), new ObjectMapperSerializer());
    }

    @AfterEach
    void tearDown() {
        quoteRequestProducer.close();
        quoteConsumer.close();
    }

    Properties consumerConfig() {
        Properties properties = new Properties();
        properties.putAll(kafkaConfig);
        //Always read from the begining
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return properties;
    }

   //@Test
    void testProcessor() {
        quoteConsumer.subscribe(Collections.singleton("lf-categories"));
        ClientCategory c = new ClientCategory(1, "Business" );
        quoteRequestProducer.send(new ProducerRecord<>("lf-categories", c));

        System.out.println("strating Messages receive");

        ClientCategory cat = null;

        ConsumerRecords<String, ClientCategory> records = quoteConsumer.poll(Duration.ofMillis(10000));
            for (ConsumerRecord<String, ClientCategory> record : records) {

                cat = record.value();

                System.out.println("Message " + cat.id);

               if (!clientCategoriesMap.containsKey(cat.id)) {
                    clientCategoriesMap.put(cat.id, cat.category_name);
                }


            }

        assertEquals(cat.id,1);

        System.out.println("After Messages comsumed " + clientCategoriesMap.size());


    }
}
