package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.model.Interaction;
import ru.practicum.model.Similarity;
import ru.practicum.repository.InteractionRepository;
import ru.practicum.repository.SimilarityRepository;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyzerService {

    private static final int RECENT_LIMIT = 20;
    private static final int NEIGHBORS_LIMIT = 20;

    private final InteractionRepository interactionRepository;
    private final SimilarityRepository similarityRepository;

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<Result> getInteractionsCount(List<Long> eventIds) {

        if (eventIds == null || eventIds.isEmpty()) {
            return List.of();
        }

        log.info("=== GET INTERACTIONS COUNT FOR: {}", eventIds);

        List<Interaction> interactions = interactionRepository.findByEventIdIn(eventIds);

        log.info("=== FOUND {} INTERACTIONS", interactions.size());
        interactions.forEach(i ->
                log.info("=== INTERACTION: event={}, user={}, rating={}",
                        i.getEventId(), i.getUserId(), i.getRating())
        );

        Map<Long, Double> sums = new HashMap<>();
        for (Interaction i : interactions) {
            sums.merge(i.getEventId(), i.getRating(), Double::sum);
        }

        log.info("=== SUMS: {}", sums);

        return eventIds.stream()
                .map(id -> new Result(id, sums.getOrDefault(id, 0.0)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Result> getRecommendationsForUser(long userId, int maxResults) {

        List<Interaction> recent = interactionRepository
                .findByUserIdOrderByTsDesc(userId, PageRequest.of(0, RECENT_LIMIT));

        if (recent.isEmpty()) {
            return List.of();
        }

        Set<Long> userEventIds = interactionRepository.findByUserId(userId).stream()
                .map(Interaction::getEventId)
                .collect(Collectors.toSet());

        Set<Long> recentEventIds = recent.stream()
                .map(Interaction::getEventId)
                .collect(Collectors.toSet());

        List<Similarity> similarities = similarityRepository.findAllForEvents(recentEventIds);

        Set<Long> candidates = new HashSet<>();
        for (Similarity s : similarities) {
            long other = userEventIds.contains(s.getEvent1()) ? s.getEvent2() : s.getEvent1();
            if (!userEventIds.contains(other)) {
                candidates.add(other);
            }
        }

        List<Similarity> allCandidateSimilarities =
                similarityRepository.findAllForEvents(candidates);

        List<Result> results = new ArrayList<>();


        for (Long candidateId : candidates) {

            List<Similarity> neighbors = allCandidateSimilarities.stream()
                    .filter(s -> s.getEvent1().equals(candidateId) ||
                                    s.getEvent2().equals(candidateId)
                    )
                    .filter(s -> {long other = s.getEvent1().equals(candidateId) ?
                            s.getEvent2() : s.getEvent1();
                        return userEventIds.contains(other);
                    })
                    .sorted((a, b) ->
                            Double.compare(b.getSimilarity(), a.getSimilarity()))
                    .limit(NEIGHBORS_LIMIT)
                    .toList();

            double weightedSum = 0.0;
            double simSum = 0.0;

            for (Similarity n : neighbors) {
                long otherId = n.getEvent1().equals(candidateId)
                        ? n.getEvent2()
                        : n.getEvent1();

                double rating = interactionRepository
                        .findByUserIdAndEventId(userId, otherId)
                        .map(Interaction::getRating)
                        .orElse(0.0);

                weightedSum += n.getSimilarity() * rating;
                simSum += n.getSimilarity();
            }

            if (simSum > 0.0) {
                results.add(new Result(candidateId, weightedSum / simSum));
            }
        }

        return results.stream()
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .limit(maxResults)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Result> getSimilarEvents(long eventId, long userId, int maxResults) {

        Set<Long> userEvents = interactionRepository.findByUserId(userId).stream()
                .map(Interaction::getEventId)
                .collect(Collectors.toSet());

        return similarityRepository.findAllForEvent(eventId).stream()
                .map(s -> {
                    long other = s.getEvent1().equals(eventId) ? s.getEvent2() : s.getEvent1();
                    return new Result(other, s.getSimilarity());
                })
                .filter(r -> !userEvents.contains(r.eventId()))
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .limit(maxResults)
                .toList();
    }

    public record Result(long eventId, double score) {}
}