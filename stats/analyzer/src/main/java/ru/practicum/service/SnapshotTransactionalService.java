package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.model.Interaction;
import ru.practicum.model.Similarity;
import ru.practicum.repository.InteractionRepository;
import ru.practicum.repository.SimilarityRepository;

@Service
@RequiredArgsConstructor
public class SnapshotTransactionalService {

    private final InteractionRepository interactionRepository;
    private final SimilarityRepository similarityRepository;
    private final ActionWeightResolver weightResolver;

    @Transactional
    public void processUserAction(UserActionAvro action) {

        double newRating = weightResolver.resolve(action.getActionType());

        Interaction interaction = interactionRepository
                .findByUserIdAndEventId(action.getUserId(), action.getEventId())
                .orElse(null);

        if (interaction == null) {
            interaction = new Interaction();
            interaction.setUserId(action.getUserId());
            interaction.setEventId(action.getEventId());
        }

        if (interaction.getRating() == null || newRating > interaction.getRating()) {
            interaction.setRating(newRating);
        }

        interaction.setTs(action.getTimestamp());

        interactionRepository.save(interaction);
    }

    @Transactional
    public void processSimilarity(EventSimilarityAvro message) {

        long event1 = Math.min(message.getEventA(), message.getEventB());
        long event2 = Math.max(message.getEventA(), message.getEventB());

        Similarity similarity = similarityRepository
                .findByEvent1AndEvent2(event1, event2)
                .orElse(null);

        if (similarity == null) {
            similarity = new Similarity();
            similarity.setEvent1(event1);
            similarity.setEvent2(event2);
        }

        similarity.setSimilarity(message.getScore());
        similarity.setTs(message.getTimestamp());

        similarityRepository.save(similarity);
    }
}