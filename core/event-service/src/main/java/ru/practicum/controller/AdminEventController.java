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
import ru.practicum.dto.events.UpdateEventAdminRequest;
import ru.practicum.enums.State;
import ru.practicum.feign.events.AdminEventClient;
import ru.practicum.params.AdminEventsParam;
import ru.practicum.service.EventService;

import java.util.List;

@RestController
@RequestMapping("/admin/events")
@RequiredArgsConstructor
@Slf4j
@Validated
public class AdminEventController implements AdminEventClient {

    private final EventService eventService;

    @Override
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<EventFullDto> searchEventsByAdmin(
            @RequestParam(required = false) Long[] users,
            @RequestParam(required = false) State[] states,
            @RequestParam (required = false) Long[] categories,
            @RequestParam (required = false) Boolean paid,
            @RequestParam (required = false) String rangeStart,
            @RequestParam (required = false) String rangeEnd,
            @RequestParam (defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam (defaultValue = "10") @Positive Integer size) {

        log.info("Поиск событий администратором");

        AdminEventsParam adminEventsParam = new AdminEventsParam(users, states, categories, rangeStart,
                rangeEnd, from, size);

        return eventService.searchEventsByAdmin(adminEventsParam);
    }

    @Override
    @PatchMapping("/{eventId}")
    public EventFullDto updateEventByAdmin(@PathVariable Long eventId,
                                           @Valid @RequestBody UpdateEventAdminRequest request) {

        log.info("PATCH /admin/events/{} - администратор редактирует событие", eventId);

        return eventService.updateEventByAdmin(request, eventId);
    }
}