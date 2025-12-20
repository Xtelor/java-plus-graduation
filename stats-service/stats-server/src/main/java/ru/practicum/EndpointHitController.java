package ru.practicum;

import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

@RestController
public class EndpointHitController {
    private final EndpointHitService endpointHitService;

    public EndpointHitController(EndpointHitService endpointHitService) {
        this.endpointHitService = endpointHitService;
    }

    @GetMapping("/stats")
    public List<ViewStatsDto> getAll(
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam String[] uris,
            @RequestParam(defaultValue = "false") Boolean unique) {
        return endpointHitService.getStatistics(start, end, uris, unique);
    }

    @PostMapping("/hit")
    public void create(@RequestBody EndpointHitDto endpointHitDto) {
        endpointHitService.create(endpointHitDto);
    }
}
