package ru.practicum.feign.events;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.events.EventFullDto;
import ru.practicum.dto.events.EventShortDto;
import ru.practicum.dto.events.NewEventDto;
import ru.practicum.dto.events.UpdateEventUserRequest;

import java.util.List;

@FeignClient(name = "event-service", contextId = "PrivateEventClient")
public interface PrivateEventClient {

    // Создание события
    @PostMapping("/users/{userId}/events")
    EventFullDto createEvent(@Valid @RequestBody NewEventDto newEventDto,
                             @PathVariable("userId") Long initiatorId);

    // Обновление события по ID
    @PatchMapping("/users/{userId}/events/{eventId}")
    EventFullDto updateEvent(@Valid @RequestBody UpdateEventUserRequest updateEventUserRequest,
                             @PathVariable("userId") Long initiatorId,
                             @PathVariable("eventId") Long eventId);

    // Получение событий, добавленных текущим пользователем
    @GetMapping("/users/{userId}/events")
    List<EventShortDto> findByInitiatorId(@PathVariable("userId") Long initiatorId,
                                          @RequestParam(value = "from", defaultValue = "0") @PositiveOrZero int from,
                                          @RequestParam (value = "size", defaultValue = "10") @Positive int size);

    // "Получение подробной информации о событии, добавленном текущим пользователем"
    @GetMapping("/users/{userId}/events/{eventId}")
    EventFullDto findByIdAndInitiatorId(@PathVariable("userId") Long initiatorId,
                                        @PathVariable("eventId") Long eventId);
}
