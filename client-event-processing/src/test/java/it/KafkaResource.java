package it;

import java.util.Collections;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.strimzi.test.container.StrimziKafkaContainer;

public class KafkaResource implements QuarkusTestResourceLifecycleManager {

    private final static  StrimziKafkaContainer strimziKafkaContainer = new StrimziKafkaContainer()
            .withBrokerId(1)
            .withPort(9092)
            .withKafkaVersion("3.2.0")
            .waitForRunning();

   
    @Override
    public Map<String, String> start() {
        strimziKafkaContainer.start();
        return Collections.singletonMap("kafka.bootstrap.servers", strimziKafkaContainer.getBootstrapServers());
    }

    @Override
    public void stop() {
        strimziKafkaContainer.close();
    }

    public static String getBootstrapServers() {
        return strimziKafkaContainer.getBootstrapServers();
    }
}
