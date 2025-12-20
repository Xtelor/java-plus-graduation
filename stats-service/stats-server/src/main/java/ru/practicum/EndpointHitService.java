package ru.practicum;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EndpointHitService {
    private final EndpointHitRepository endpointHitRepository;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneOffset.UTC);

    public List<ViewStatsDto> getStatistics(String start, String end, String[] uris, Boolean unique) {
        return endpointHitRepository.findStatistics(Instant.from(FORMATTER.parse(start)), Instant.from(FORMATTER.parse(end)), uris, unique)
                .stream()
                .map(endpointHit -> EndpointHitMapper.toViewStatsDto(endpointHit, 10))
                .collect(Collectors.toList());
    }

    public void create(EndpointHitDto endpointHitDto) {
        EndpointHit endpointHit = EndpointHitMapper.toEndpointHit(endpointHitDto);
        endpointHitRepository.save(endpointHit);
    }
}
