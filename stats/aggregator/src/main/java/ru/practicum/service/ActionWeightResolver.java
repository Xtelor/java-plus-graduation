package ru.practicum.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;

@Component
public class ActionWeightResolver {

    @Value("${stats.action-weights.view}")
    private double viewWeight;

    @Value("${stats.action-weights.register}")
    private double registerWeight;

    @Value("${stats.action-weights.like}")
    private double likeWeight;

    public double resolve(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> viewWeight;
            case REGISTER -> registerWeight;
            case LIKE -> likeWeight;
        };
    }
}
