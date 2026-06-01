package ru.practicum.service;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;

import java.time.Instant;

@GrpcService
@RequiredArgsConstructor
public class UserActionGrpcService extends UserActionControllerGrpc.UserActionControllerImplBase {

    private final KafkaProducer<String, SpecificRecordBase> kafkaProducer;

    @Value("${collector.kafka.topic.user-actions}")
    private String topic;

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {

        UserActionAvro message = UserActionAvro.newBuilder()
                .setUserId(request.getUserId())
                .setEventId(request.getEventId())
                .setActionType(mapActionType(request.getActionType()))
                .setTimestamp(toInstant(request))
                .build();

        try {
            kafkaProducer.send(new ProducerRecord<>(topic, null, message)).get();
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Ошибка Kafka: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    private ActionTypeAvro mapActionType(ActionTypeProto type) {
        return switch (type) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            case UNRECOGNIZED -> throw new IllegalArgumentException("Unknown action type");
        };
    }

    private Instant toInstant(UserActionProto request) {
        if (!request.hasTimestamp()) {
            return Instant.now();
        }
        return Instant.ofEpochSecond(
                request.getTimestamp().getSeconds(),
                request.getTimestamp().getNanos()
        );
    }
}
