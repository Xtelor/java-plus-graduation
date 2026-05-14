package ru.practicum.feign.events;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.dto.events.EventFullDto;

@FeignClient(name = "event-service", contextId = "InternalEventClient", path = "/internal/events")
public interface InternalEventClient {

    @GetMapping("/{eventId}")
    EventFullDto getById(@PathVariable("eventId") Long eventId);

    @GetMapping("/category/{categoryId}/exists")
    Boolean existsByCategoryId(@PathVariable("categoryId") Long categoryId);
}
