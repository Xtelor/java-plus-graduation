package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.requests.EventRequestStatusUpdateRequest;
import ru.practicum.dto.requests.EventRequestStatusUpdateResult;
import ru.practicum.dto.requests.ParticipationRequestDto;
import ru.practicum.feign.requests.PrivateEventRequestClient;
import ru.practicum.service.RequestEventService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/events/{eventId}/requests")
@RequiredArgsConstructor
@Slf4j
public class PrivateEventRequestController implements PrivateEventRequestClient {

    private final RequestEventService requestEventService;

    @Override
    @GetMapping
    public List<ParticipationRequestDto> getEventParticipants(@PathVariable Long userId,
                                                              @PathVariable Long eventId) {
        log.info("GET /users/{}/events/{}/requests - получение запросов на участие в событии", userId, eventId);
        return requestEventService.getEventParticipants(userId, eventId);
    }

    @Override
    @PatchMapping
    public EventRequestStatusUpdateResult changeRequestStatus(@PathVariable Long userId,
                                                              @PathVariable Long eventId,
                                                              @RequestBody EventRequestStatusUpdateRequest request) {
        log.info("PATCH /users/{}/events/{}/requests - изменение статуса запросов", userId, eventId);
        return requestEventService.changeRequestStatus(userId, eventId, request);
    }
}