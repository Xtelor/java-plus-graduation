package ru.practicum.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.shared.deserialization.EventSimilarityDeserializer;
import ru.practicum.shared.deserialization.UserActionDeserializer;

import java.util.Properties;
import java.util.UUID;

@Configuration
public class KafkaAnalyzerConfig {

    @Value("${analyzer.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${analyzer.kafka.consumer.user-actions.group-id}")
    private String userActionsGroupId;

    @Value("${analyzer.kafka.consumer.similarities.group-id}")
    private String similaritiesGroupId;


    @Bean
    public KafkaConsumer<String, UserActionAvro> userActionConsumer() {
        Properties config = new Properties();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, userActionsGroupId);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, UserActionDeserializer.class);
        config.put(ConsumerConfig.CLIENT_ID_CONFIG, "analyzer-actions-" + UUID.randomUUID());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);

        return new KafkaConsumer<>(config);
    }

    @Bean
    public KafkaConsumer<String, EventSimilarityAvro> similarityConsumer() {
        Properties config = new Properties();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, similaritiesGroupId);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, EventSimilarityDeserializer.class);
        config.put(ConsumerConfig.CLIENT_ID_CONFIG, "analyzer-similarity-" + UUID.randomUUID());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);

        return new KafkaConsumer<>(config);
    }
}
