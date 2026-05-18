package ru.practicum.feign.requests;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.dto.requests.EventRequestStatusUpdateRequest;
import ru.practicum.dto.requests.EventRequestStatusUpdateResult;
import ru.practicum.dto.requests.ParticipationRequestDto;

import java.util.List;

@FeignClient(
        name = "request-service",
        contextId = "PrivateEventRequestClient",
        fallback = PrivateEventRequestClientFallback.class
)
public interface PrivateEventRequestClient {

    // Получение запросов на участие в событии
    @GetMapping("/users/{userId}/events/{eventId}/requests")
    List<ParticipationRequestDto> getEventParticipants(@PathVariable("userId") Long userId,
                                                       @PathVariable("eventId") Long eventId);

    // Изменение статуса запросов
    @PatchMapping("/users/{userId}/events/{eventId}/requests")
    EventRequestStatusUpdateResult changeRequestStatus(@PathVariable("userId") Long userId,
                                                       @PathVariable("eventId") Long eventId,
                                                       @RequestBody EventRequestStatusUpdateRequest request);
}
