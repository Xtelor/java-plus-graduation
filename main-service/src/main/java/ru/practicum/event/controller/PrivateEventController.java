package ru.practicum.event.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.EventService;
import ru.practicum.event.dto.UpdateEventUserRequest;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.NewEventDto;
import ru.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request.dto.ParticipationRequestDto;

import java.util.List;

@RestController
@RequestMapping(path = "/users/{userId}/events")
@RequiredArgsConstructor
@Slf4j
@Validated
public class PrivateEventController {
    private final EventService eventService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto createEvent(@Valid @RequestBody NewEventDto newEventDto,
                                    @PathVariable("userId") Long initiatorId) {
        log.info("POST-запрос на создание события: {}", newEventDto);

        return eventService.createEvent(newEventDto, initiatorId);
    }

    // Получение полной информации о событии
    @GetMapping("/{eventId}")
    public EventFullDto getEventByInitiator(@PathVariable Long userId,
                                            @PathVariable Long eventId) {
        log.info("GET /users/{}/events/{} - получение события", userId, eventId);

        return eventService.getEventByInitiator(userId, eventId);
    }

    // Изменение события
    @PatchMapping("/{eventId}")
    public EventFullDto updateEventByInitiator(@PathVariable Long userId,
                                               @PathVariable Long eventId,
                                               @Valid @RequestBody UpdateEventUserRequest request) {
        log.info("PATCH /users/{}/events/{} - обновление события", userId, eventId);

        return eventService.updateEventByInitiator(userId, eventId, request);
    }

    // Получение заявок на участие в событии
    @GetMapping("/{eventId}/requests")
    public List<ParticipationRequestDto> getEventRequests(@PathVariable Long userId,
                                                          @PathVariable Long eventId) {
        log.info("GET /users/{}/events/{}/requests - получение заявок", userId, eventId);

        return eventService.getEventRequests(userId, eventId);
    }

    // Одобрение/Отклонение заявок
    @PatchMapping("/{eventId}/requests")
    public EventRequestStatusUpdateResult changeRequestStatus(@PathVariable Long userId,
                                                              @PathVariable Long eventId,
                                                              @RequestBody EventRequestStatusUpdateRequest dto) {
        log.info("PATCH /users/{}/events/{}/requests - изменение статуса заявок", userId, eventId);

        return eventService.changeRequestStatus(userId, eventId, dto);
    }
}
