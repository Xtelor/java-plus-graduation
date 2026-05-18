package ru.practicum.feign.requests;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "request-service", contextId = "InternalRequestClient", path = "/internal/requests")
public interface InternalRequestClient {

    @GetMapping("/confirmed")
    Map<Long, Long> getConfirmedRequestsCount(@RequestParam("eventIds") List<Long> eventIds);
}
