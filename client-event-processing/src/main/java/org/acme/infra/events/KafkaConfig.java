package org.acme.infra.events;

import java.util.Optional;
import java.util.Properties;

import javax.inject.Singleton;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.annotations.ConfigItem;

/**
 * Centralize all the properties for Kafka and Kafka Streams
 */
@Singleton
public class KafkaConfig {
    private static final String KAFKA_OPTION_PREFIX = "kafka.";
    private static final String QUARKUS_KAFKA_OPTION_PREFIX = "quarkus." + KAFKA_OPTION_PREFIX;


    /**
     * A unique identifier for this Kafka Streams application.
     * If not set, defaults to quarkus.application.name.
     */
    @ConfigItem(defaultValue = "${quarkus.application.name}")
    public String applicationId;

    @ConfigProperty(name = "kafka.bootstrap.servers")
    protected String bootstrapServers;
    @ConfigProperty(name = "application.id", defaultValue = "ClientProcessing")
    protected Optional<String> applicationID;
    @ConfigItem(defaultValue = "schema.registry.url")
    public String schemaRegistryKey;

    /**
     * The schema registry URL.
     */
    @ConfigItem
    public Optional<String> schemaRegistryUrl;

    /**
     * A unique identifier of this application instance, typically in the form
     * host:port.
     */
    @ConfigItem
    public Optional<String> applicationServer;

    public KafkaConfig() {
    }

    public Properties getKafkaProperties() {
        Properties properties = getKafkaPropertiesFromFile();
        properties.putAll(getQuarkusKafkaPropertiesFromFile());
        properties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        properties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        return properties;
    }

    public Properties getKafkaPropertiesFromFile() {
        return defineKafkaPropertiesUsingPrefix(KAFKA_OPTION_PREFIX);
    }

    public Properties getQuarkusKafkaPropertiesFromFile() {
        return defineKafkaPropertiesUsingPrefix(QUARKUS_KAFKA_OPTION_PREFIX);
    }

    /**
     * From the load properties, look at the one starting by the expected prefix
     * 
     * @param prefix
     * @return
     */
    private Properties defineKafkaPropertiesUsingPrefix(String prefix) {
        Properties kafkaStreamsProperties = new Properties();
        Config config = ConfigProvider.getConfig();
        for (String property : config.getPropertyNames()) {
            if (isPropertyNameStartsByPrefix(prefix, property)) {
                includeKafkaStreamsProperty(config, kafkaStreamsProperties, prefix, property);
            }
        }

        return kafkaStreamsProperties;
    }

    private boolean isPropertyNameStartsByPrefix(String prefix, String property) {
        return property.startsWith(prefix);
    }

    private  void includeKafkaStreamsProperty(Config config, Properties kafkaStreamsProperties, String prefix,
            String property) {
        Optional<String> value = config.getOptionalValue(property, String.class);
        if (value.isPresent()) {
            kafkaStreamsProperties.setProperty(property.substring(prefix.length()), value.get());
        }
    }
}
