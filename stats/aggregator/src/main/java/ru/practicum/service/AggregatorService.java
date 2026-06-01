package ru.practicum.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class AggregatorService {

    private final KafkaConsumer<String, UserActionAvro> consumer;
    private final KafkaProducer<String, SpecificRecordBase> producer;
    private final ActionWeightResolver weightResolver;

    private final SimilarityState state = new SimilarityState();
    private volatile boolean running = true;
    private Thread workerThread;

    @Value("${aggregator.kafka.consumer.user-actions-topic}")
    private String userActionsTopic;

    @Value("${aggregator.kafka.producer.similarity-topic}")
    private String similarityTopic;

    @PostConstruct
    public void start() {
        workerThread = new Thread(this::runLoop, "aggregator-worker");

        workerThread.start();
        log.info("Aggregator worker thread started");
    }

    private void runLoop() {
        try {
            consumer.subscribe(Collections.singletonList(userActionsTopic));
            log.info("Подписка на топик {}", userActionsTopic);

            while (running) {
                var records = consumer.poll(Duration.ofMillis(500));
                records.forEach(record -> process(record.value()));
            }
        } catch (WakeupException e) {
            if (running) {
                throw e;
            }
            log.info("Consumer wakeup received for shutdown");
        } catch (Exception e) {
            log.error("Ошибка в цикле агрегатора", e);
        } finally {
            try {
                consumer.close();
            } catch (Exception e) {
                log.warn("Ошибка при закрытии consumer", e);
            }
        }
    }

    private void process(UserActionAvro action) {
        long userId = action.getUserId();
        long eventId = action.getEventId();
        Instant timestamp = action.getTimestamp();

        double newWeight = weightResolver.resolve(action.getActionType());
        double oldWeight = state.getCurrentWeight(eventId, userId);

        if (newWeight <= oldWeight) {
            return;
        }

        state.putUserWeight(eventId, userId, newWeight);
        state.updateWeightSum(eventId, oldWeight, newWeight);

        for (Long otherEventId : state.getAllEventIds()) {
            if (otherEventId.equals(eventId)) {
                continue;
            }

            double otherWeight = state.getCurrentWeight(otherEventId, userId);
            if (otherWeight == 0.0) {
                continue;
            }

            double oldMin = Math.min(oldWeight, otherWeight);
            double newMin = Math.min(newWeight, otherWeight);
            state.updatePairMinSum(eventId, otherEventId, newMin - oldMin);

            double similarity = calculateSimilarity(eventId, otherEventId);

            long eventA = Math.min(eventId, otherEventId);
            long eventB = Math.max(eventId, otherEventId);

            EventSimilarityAvro message = EventSimilarityAvro.newBuilder()
                    .setEventA(eventA)
                    .setEventB(eventB)
                    .setScore(similarity)
                    .setTimestamp(timestamp)
                    .build();

            producer.send(new ProducerRecord<>(
                    similarityTopic,
                    eventA + "_" + eventB,
                    message
            ));
        }
    }

    private double calculateSimilarity(long eventA, long eventB) {
        double minSum = state.getPairMinSum(eventA, eventB);
        double normA = state.getNorm(eventA);
        double normB = state.getNorm(eventB);

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return minSum / (normA * normB);
    }

    @PreDestroy
    public void stop() {
        log.info("Остановка агрегатора...");
        running = false;

        try {
            consumer.wakeup();
        } catch (Exception e) {
            log.warn("Ошибка wakeup", e);
        }

        if (workerThread != null) {
            try {
                workerThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        try {
            producer.close();
        } catch (Exception e) {
            log.warn("Ошибка при закрытии producer", e);
        }
    }
}
