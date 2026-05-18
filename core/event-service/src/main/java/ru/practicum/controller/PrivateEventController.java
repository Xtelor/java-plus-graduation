package ru.practicum.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.events.EventFullDto;
import ru.practicum.dto.events.EventShortDto;
import ru.practicum.dto.events.NewEventDto;
import ru.practicum.dto.events.UpdateEventUserRequest;
import ru.practicum.feign.events.PrivateEventClient;
import ru.practicum.service.EventService;

import java.util.List;

@RestController
@RequestMapping(path = "/users/{userId}/events")
@RequiredArgsConstructor
@Slf4j
@Validated
public class PrivateEventController implements PrivateEventClient {
    private final EventService eventService;

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto createEvent(@Valid @RequestBody NewEventDto newEventDto,
                                    @PathVariable("userId") Long initiatorId) {
        log.info("POST-запрос на создание события: {}", newEventDto);

        return eventService.createEvent(newEventDto, initiatorId);
    }

    @Override
    @PatchMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto updateEvent(@Valid @RequestBody UpdateEventUserRequest updateEventUserRequest,
                                    @PathVariable("userId") Long initiatorId,
                                    @PathVariable Long eventId) {
        log.info("PATCH запрос на обновление события с id: , добавленного текущим пользователем {}", eventId);
        return eventService.updateEventUser(updateEventUserRequest, initiatorId, eventId);
    }

    @Override
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<EventShortDto> findByInitiatorId(@PathVariable("userId") Long initiatorId,
                                                 @RequestParam(defaultValue = "0") @PositiveOrZero int from,
                                                 @RequestParam (defaultValue = "10") @Positive int size) {
        log.info("Получение событий, добавленных текущим пользователем");
        return eventService.findByInitiatorId(initiatorId, from, size);
    }

    @Override
    @GetMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto findByIdAndInitiatorId(@PathVariable("userId") Long initiatorId,
                                               @PathVariable Long eventId) {
        log.info("Получение подробной информации о событии, добавленном текущим пользователем");
        return eventService.findByIdAndInitiatorId(initiatorId, eventId);
    }
}
