package ru.practicum.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SimilarityState {

    private final Map<Long, Map<Long, Double>> eventUserWeights = new HashMap<>();
    private final Map<Long, Double> eventWeightSums = new HashMap<>();
    private final Map<Long, Map<Long, Double>> pairMinSums = new HashMap<>();

    public double getCurrentWeight(long eventId, long userId) {
        return eventUserWeights.getOrDefault(eventId, Map.of()).getOrDefault(userId, 0.0);
    }

    public void putUserWeight(long eventId, long userId, double weight) {
        eventUserWeights.computeIfAbsent(eventId, k -> new HashMap<>()).put(userId, weight);
    }

    public Set<Long> getAllEventIds() {
        return eventUserWeights.keySet();
    }

    public void updateWeightSum(long eventId, double oldWeight, double newWeight) {
        double current = eventWeightSums.getOrDefault(eventId, 0.0);
        current = current - oldWeight + newWeight;
        eventWeightSums.put(eventId, current);
    }

    public double getNorm(long eventId) {
        return Math.sqrt(eventWeightSums.getOrDefault(eventId, 0.0));
    }

    public void updatePairMinSum(long eventA, long eventB, double delta) {
        long a = Math.min(eventA, eventB);
        long b = Math.max(eventA, eventB);

        pairMinSums.computeIfAbsent(a, k -> new HashMap<>());
        double current = pairMinSums.get(a).getOrDefault(b, 0.0);
        pairMinSums.get(a).put(b, current + delta);
    }

    public double getPairMinSum(long eventA, long eventB) {
        long a = Math.min(eventA, eventB);
        long b = Math.max(eventA, eventB);
        return pairMinSums.getOrDefault(a, Map.of()).getOrDefault(b, 0.0);
    }
}