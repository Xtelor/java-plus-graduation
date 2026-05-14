package ru.practicum.feign.requests;

import org.springframework.stereotype.Component;
import ru.practicum.dto.requests.EventRequestStatusUpdateRequest;
import ru.practicum.dto.requests.EventRequestStatusUpdateResult;
import ru.practicum.dto.requests.ParticipationRequestDto;

import java.util.Collections;
import java.util.List;

@Component
public class PrivateEventRequestClientFallback implements PrivateEventRequestClient {

    @Override
    public List<ParticipationRequestDto> getEventParticipants(Long userId, Long eventId) {
        return Collections.emptyList();
    }

    @Override
    public EventRequestStatusUpdateResult changeRequestStatus(Long userId,
                                                              Long eventId,
                                                              EventRequestStatusUpdateRequest request) {
        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();
        result.setConfirmedRequests(Collections.emptyList());
        result.setRejectedRequests(Collections.emptyList());
        return result;
    }
}
