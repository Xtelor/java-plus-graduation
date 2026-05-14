package ru.practicum.feign.events;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.events.EventFullDto;
import ru.practicum.dto.events.EventShortDto;
import ru.practicum.enums.SortEvents;

import java.util.List;

@FeignClient(name = "event-service", contextId = "PublicEventClient", path = "/events")
public interface PublicEventClient {

    // Получение событий через публичный эндпоинт
    @GetMapping
    List<EventShortDto> getEvents(
            @RequestParam(value = "text", required = false) String text,
            @RequestParam (value = "categories", required = false) Long[] categories,
            @RequestParam (value = "paid", required = false) Boolean paid,
            @RequestParam (value = "rangeStart", required = false) String rangeStart,
            @RequestParam (value = "rangeEnd", required = false) String rangeEnd,
            @RequestParam (value = "onlyAvailable", defaultValue = "false") Boolean onlyAvailable,
            @RequestParam (value = "sort", defaultValue = "EVENT_DATE") SortEvents sort,
            @RequestParam (value = "from", defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam (value = "size", defaultValue = "10") @Positive Integer size);

    // Получение полной информации о событии
    @GetMapping("/{eventId}")
    EventFullDto findById(@PathVariable("eventId") Long eventId);
}
