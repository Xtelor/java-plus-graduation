package ru.practicum;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class EndpointHitController {
    private final EndpointHitService endpointHitService;

    public EndpointHitController(EndpointHitService endpointHitService) {
        this.endpointHitService = endpointHitService;
    }

    @GetMapping("/stats")
    public List<ViewStatsDto> getAll(
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) String[] uris,
            @RequestParam(defaultValue = "false") Boolean unique) {
        return endpointHitService.getStatistics(start, end, uris, unique);
    }

    @PostMapping("/hit")
    public void create(@Valid @RequestBody EndpointHitDto endpointHitDto) {
        endpointHitService.create(endpointHitDto);
    }
}
