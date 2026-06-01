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

import java.math.BigDecimal;
import java.math.RoundingMode;
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

        BigDecimal newWeight = BigDecimal.valueOf(weightResolver.resolve(action.getActionType()));
        BigDecimal oldWeight = BigDecimal.valueOf(state.getCurrentWeight(eventId, userId));

        if (newWeight.compareTo(oldWeight) <= 0) {
            return;
        }


        state.putUserWeight(eventId, userId, newWeight.doubleValue());
        state.updateWeightSum(eventId, oldWeight.doubleValue(), newWeight.doubleValue());

        for (Long otherEventId : state.getAllEventIds()) {
            if (otherEventId.equals(eventId)) {
                continue;
            }

            BigDecimal otherWeight = BigDecimal.valueOf(
                    state.getCurrentWeight(otherEventId, userId)
            );

            if (otherWeight.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            BigDecimal oldMin = oldWeight.min(otherWeight);
            BigDecimal newMin = newWeight.min(otherWeight);
            BigDecimal deltaMin = newMin.subtract(oldMin);

            state.updatePairMinSum(eventId, otherEventId, deltaMin.doubleValue());

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

        BigDecimal minSum = BigDecimal.valueOf(state.getPairMinSum(eventA, eventB));
        BigDecimal normA = BigDecimal.valueOf(state.getNorm(eventA));
        BigDecimal normB = BigDecimal.valueOf(state.getNorm(eventB));

        if (normA.compareTo(BigDecimal.ZERO) == 0 || normB.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }

        BigDecimal denominator = normA.multiply(normB);

        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }

        return minSum
                .divide(denominator, 10, RoundingMode.HALF_UP)
                .doubleValue();
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
