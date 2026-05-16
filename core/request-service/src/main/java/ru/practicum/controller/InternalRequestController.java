package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.service.RequestEventService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/requests")
@RequiredArgsConstructor
@Slf4j
public class InternalRequestController {

    private final RequestEventService requestEventService;

    @GetMapping("/confirmed")
    public Map<Long, Long> getConfirmedRequestsCount(@RequestParam("eventIds") List<Long> eventIds) {

        log.info("GET /internal/requests/confirmed для событий: {}", eventIds);
        return requestEventService.getConfirmedRequestsCountByEvents(eventIds);
    }
}
