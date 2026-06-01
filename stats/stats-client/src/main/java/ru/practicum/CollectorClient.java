package ru.practicum;

import com.google.protobuf.Timestamp;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;

import java.time.Instant;

@Slf4j
@Component
public class CollectorClient {

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub stub;

    public void sendView(long userId, long eventId) {
        sendAction(userId, eventId, ActionTypeProto.ACTION_VIEW);
    }

    public void sendRegister(long userId, long eventId) {
        sendAction(userId, eventId, ActionTypeProto.ACTION_REGISTER);
    }

    public void sendLike(long userId, long eventId) {
        sendAction(userId, eventId, ActionTypeProto.ACTION_LIKE);
    }

    private void sendAction(long userId, long eventId, ActionTypeProto actionType) {
        try {
            Instant now = Instant.now();

            UserActionProto request = UserActionProto.newBuilder()
                    .setUserId(userId)
                    .setEventId(eventId)
                    .setActionType(actionType)
                    .setTimestamp(Timestamp.newBuilder()
                            .setSeconds(now.getEpochSecond())
                            .setNanos(now.getNano())
                            .build())
                    .build();

            stub.collectUserAction(request);
        } catch (Exception e) {
            log.error("Ошибка отправки действия в Collector: {}", e.getMessage(), e);
        }
    }
}
