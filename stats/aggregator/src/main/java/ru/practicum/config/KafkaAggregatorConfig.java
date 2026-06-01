package ru.practicum.config;

import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.shared.deserialization.UserActionDeserializer;
import ru.practicum.shared.serialization.EventSerializer;

import java.util.Properties;
import java.util.UUID;

@Configuration
public class KafkaAggregatorConfig {

    @Value("${aggregator.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${aggregator.kafka.consumer.group-id}")
    private String groupId;

    @Bean
    public KafkaConsumer<String, UserActionAvro> userActionConsumer() {
        Properties config = new Properties();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, UserActionDeserializer.class);
        config.put(ConsumerConfig.CLIENT_ID_CONFIG, "aggregator-consumer-" + UUID.randomUUID());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new KafkaConsumer<>(config);
    }

    @Bean
    public KafkaProducer<String, SpecificRecordBase> similarityProducer() {
        Properties config = new Properties();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, EventSerializer.class);
        config.put(ProducerConfig.CLIENT_ID_CONFIG, "aggregator-producer-" + UUID.randomUUID());
        return new KafkaProducer<>(config);
    }
}
