package ru.practicum.config;

import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.shared.serialization.EventSerializer;

import java.util.Properties;
import java.util.UUID;

@Configuration
public class KafkaProducerConfig {

    @Value("${collector.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public KafkaProducer<String, SpecificRecordBase> kafkaProducer() {
        Properties config = new Properties();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, EventSerializer.class);
        config.put(ProducerConfig.CLIENT_ID_CONFIG, "collector-producer-" + UUID.randomUUID());
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 10);
        config.put(ProducerConfig.LINGER_MS_CONFIG, 0);
        return new KafkaProducer<>(config);
    }
}
