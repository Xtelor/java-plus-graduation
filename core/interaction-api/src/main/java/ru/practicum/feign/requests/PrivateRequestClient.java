package ru.practicum.feign.requests;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.requests.ParticipationRequestDto;

import java.util.List;

@FeignClient(name = "request-service", contextId = "PrivateRequestClient")
public interface PrivateRequestClient {

    // Получение запросов пользователя
    @GetMapping("/users/{userId}/requests")
    List<ParticipationRequestDto> getUserRequests(@PathVariable("userId") Long userId);

    // Создание запроса
    @PostMapping("/users/{userId}/requests")
    ParticipationRequestDto addRequest(@PathVariable("userId") Long userId,
                                       @RequestParam(value = "eventId") Long eventId);

    // Отмена запроса
    @PatchMapping("/users/{userId}/requests/{requestId}/cancel")
    ParticipationRequestDto cancelRequest(@PathVariable("userId") Long userId,
                                          @PathVariable("requestId") Long requestId);
}
