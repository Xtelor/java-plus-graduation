package ru.practicum.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import ru.practicum.dto.events.EventFullDto;
import ru.practicum.dto.events.EventShortDto;
import ru.practicum.enums.SortEvents;
import ru.practicum.feign.events.PublicEventClient;
import ru.practicum.params.PublicEventsParam;
import ru.practicum.service.EventService;

import java.util.List;

@RestController
@RequestMapping(path = "/events")
@RequiredArgsConstructor
@Slf4j
@Validated
public class PublicEventController implements PublicEventClient {
    private final EventService eventService;

    @Override
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<EventShortDto> getEvents(
            @RequestParam(required = false) String text,
            @RequestParam (required = false) Long[] categories,
            @RequestParam (required = false) Boolean paid,
            @RequestParam (required = false) String rangeStart,
            @RequestParam (required = false) String rangeEnd,
            @RequestParam (defaultValue = "false") Boolean onlyAvailable,
            @RequestParam (defaultValue = "EVENT_DATE") SortEvents sort,
            @RequestParam (defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam (defaultValue = "10") @Positive Integer size) {

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                .currentRequestAttributes())
                .getRequest();

        log.info("Получение событий через публичный эндпоинт");

        PublicEventsParam publicEventsParam = new PublicEventsParam(text, categories, paid,  rangeStart,
                rangeEnd, onlyAvailable, sort, from, size);

        return eventService.getEventsPublic(publicEventsParam, request.getRemoteAddr(), request.getRequestURI());
    }

    @Override
    @GetMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto findById(@PathVariable Long eventId) {

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                .currentRequestAttributes())
                .getRequest();

        log.info("Получение полной информации о событии");

        return eventService.findById(eventId, request.getRemoteAddr(), request.getRequestURI());
    }
}