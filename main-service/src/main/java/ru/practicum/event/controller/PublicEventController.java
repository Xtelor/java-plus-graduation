package ru.practicum.event.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.EventService;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.params.PublicEventsParam;

import java.util.List;

@RestController
@RequestMapping(path = "/events")
@RequiredArgsConstructor
@Slf4j
@Validated
public class PublicEventController {
    private final EventService eventService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<EventShortDto> getEvents(
            @RequestParam(required = false) String text,
            @RequestParam (required = false) Long[] categories,
            @RequestParam (required = false) Boolean paid,
            @RequestParam (required = false) String rangeStart,
            @RequestParam (required = false) String rangeEnd,
            @RequestParam (defaultValue = "false") Boolean onlyAvailable,
            @RequestParam String sort,
            @RequestParam (defaultValue = "0") Integer from,
            @RequestParam (defaultValue = "10") Integer size) {

        log.info("Получение событий через публичный эндпоинт");
        PublicEventsParam publicEventsParam = new PublicEventsParam(text, categories, paid,  rangeStart, rangeEnd, onlyAvailable, sort, from, size);
        return eventService.getEventsPublic(publicEventsParam);
    }
}
