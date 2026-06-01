package ru.practicum;

import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.proto.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.RecommendationsControllerGrpc;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.UserPredictionsRequestProto;

import java.util.*;

@Slf4j
@Component
public class AnalyzerClient {

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub stub;

    public List<Long> getRecommendationIds(long userId, int maxResults) {
        try {
            UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                    .setUserId(userId)
                    .setMaxResults(maxResults)
                    .build();

            Iterator<RecommendedEventProto> iterator = stub.getRecommendationsForUser(request);

            List<Long> result = new ArrayList<>();
            iterator.forEachRemaining(item -> result.add(item.getEventId()));
            return result;
        } catch (Exception e) {
            log.error("Ошибка получения рекомендаций userId={}: {}", userId, e.getMessage(), e);
            return List.of();
        }
    }

    public Map<Long, Double> getRatings(List<Long> eventIds) {
        try {
            if (eventIds == null || eventIds.isEmpty()) {
                return Map.of();
            }

            InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                    .addAllEventId(eventIds)
                    .build();

            Iterator<RecommendedEventProto> iterator = stub.getInteractionsCount(request);

            Map<Long, Double> result = new HashMap<>();
            iterator.forEachRemaining(item ->
                    result.put(item.getEventId(), (double) item.getScore())
            );

            return result;
        } catch (Exception e) {
            log.error("Ошибка получения рейтингов: {}", e.getMessage(), e);
            return Map.of();
        }
    }
}
