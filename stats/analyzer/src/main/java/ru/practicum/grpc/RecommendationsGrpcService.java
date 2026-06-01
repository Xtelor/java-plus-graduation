package ru.practicum.grpc;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.stats.proto.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.RecommendationsControllerGrpc;
import ru.practicum.ewm.stats.proto.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.proto.UserPredictionsRequestProto;
import ru.practicum.service.AnalyzerService;

import java.util.ArrayList;
import java.util.List;

@GrpcService
@RequiredArgsConstructor
public class RecommendationsGrpcService extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final AnalyzerService analyzerService;

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                          StreamObserver<RecommendedEventProto> responseObserver) {
        analyzerService.getRecommendationsForUser(
                        request.getUserId(),
                        Math.toIntExact(request.getMaxResults())
                )
                .forEach(result -> responseObserver.onNext(
                        RecommendedEventProto.newBuilder()
                                .setEventId(result.eventId())
                                .setScore(result.score())
                                .build()
                ));

        responseObserver.onCompleted();
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> responseObserver) {
        analyzerService.getSimilarEvents(
                        request.getEventId(),
                        request.getUserId(),
                        Math.toIntExact(request.getMaxResults())
                )
                .forEach(result -> responseObserver.onNext(
                        RecommendedEventProto.newBuilder()
                                .setEventId(result.eventId())
                                .setScore(result.score())
                                .build()
                ));

        responseObserver.onCompleted();
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> responseObserver) {
        List<Long> eventIds = new ArrayList<>(request.getEventIdList());

        analyzerService.getInteractionsCount(eventIds)
                .forEach(result -> responseObserver.onNext(
                        RecommendedEventProto.newBuilder()
                                .setEventId(result.eventId())
                                .setScore(result.score())
                                .build()
                ));

        responseObserver.onCompleted();
    }
}