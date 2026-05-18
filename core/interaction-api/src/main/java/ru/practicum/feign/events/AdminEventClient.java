package ru.practicum.feign.events;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.events.EventFullDto;
import ru.practicum.dto.events.UpdateEventAdminRequest;
import ru.practicum.enums.State;

import java.util.List;

@FeignClient(name = "event-service", contextId = "AdminEventClient", path = "/admin/events")
public interface AdminEventClient {

    // Редактирование данных события и его статуса администратором
    @GetMapping
    List<EventFullDto> searchEventsByAdmin(
            @RequestParam(value = "users", required = false) Long[] users,
            @RequestParam(value = "states", required = false) State[] states,
            @RequestParam (value = "categories", required = false) Long[] categories,
            @RequestParam (value = "paid", required = false) Boolean paid,
            @RequestParam (value = "rangeStart", required = false) String rangeStart,
            @RequestParam (value = "rangeEnd", required = false) String rangeEnd,
            @RequestParam (value = "from", defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam (value = "size", defaultValue = "10") @Positive Integer size);

    // Редактирование события администратором
    @PatchMapping("/{eventId}")
    EventFullDto updateEventByAdmin(@PathVariable("eventId") Long eventId,
                                    @Valid @RequestBody UpdateEventAdminRequest request);
}
