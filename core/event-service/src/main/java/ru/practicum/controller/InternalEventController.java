package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.dto.events.EventFullDto;
import ru.practicum.service.EventService;

@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class InternalEventController {

    private final EventService eventService;

    @GetMapping("/{eventId}")
    public EventFullDto getById(@PathVariable Long eventId) {
        return eventService.getInternalEventById(eventId);
    }

    @GetMapping("/category/{categoryId}/exists")
    public Boolean existsByCategoryId(@PathVariable Long categoryId) {
        return eventService.existsByCategoryId(categoryId);
    }
}
