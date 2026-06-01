package ru.practicum.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Duration;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotProcessor {

    private final KafkaConsumer<String, UserActionAvro> userActionConsumer;
    private final KafkaConsumer<String, EventSimilarityAvro> similarityConsumer;
    private final SnapshotTransactionalService txService;

    private volatile boolean running = true;
    private Thread actionThread;
    private Thread similarityThread;

    @Value("${analyzer.kafka.consumer.user-actions.topic}")
    private String userActionsTopic;

    @Value("${analyzer.kafka.consumer.similarities.topic}")
    private String similaritiesTopic;

    @PostConstruct
    public void start() {
        actionThread = new Thread(this::consumeActions);
        similarityThread = new Thread(this::consumeSimilarities);
        actionThread.start();
        similarityThread.start();
    }

    private void consumeActions() {
        try {
            userActionConsumer.subscribe(Collections.singletonList(userActionsTopic));

            while (running) {
                var records = userActionConsumer.poll(Duration.ofMillis(200));

                for (var record : records) {
                    txService.processUserAction(record.value());
                }
            }

        } catch (WakeupException ignored) {
        } finally {
            userActionConsumer.close();
        }
    }

    private void consumeSimilarities() {
        try {
            similarityConsumer.subscribe(Collections.singletonList(similaritiesTopic));

            while (running) {
                var records = similarityConsumer.poll(Duration.ofMillis(200));

                for (var record : records) {
                    txService.processSimilarity(record.value());
                }

                if (!records.isEmpty()) {
                    similarityConsumer.commitSync();
                }
            }

        } catch (WakeupException ignored) {
        } finally {
            similarityConsumer.close();
        }
    }

    @PreDestroy
    public void stop() {
        running = false;
        userActionConsumer.wakeup();
        similarityConsumer.wakeup();
    }
}