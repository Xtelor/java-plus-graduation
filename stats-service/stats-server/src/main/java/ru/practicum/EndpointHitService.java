package ru.practicum;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EndpointHitService {
    private final EndpointHitRepository endpointHitRepository;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneOffset.UTC);

    public List<ViewStatsDto> getStatistics(String start, String end, String[] uris, Boolean unique) {
        log.info("Получение статистика по посещениям с параметрами start={}, end={}, uris={}, unique={}", start, end, uris, unique);
        if (unique == true) {
            return endpointHitRepository.findStatisticsUniqueIp(Instant.from(FORMATTER.parse(start)), Instant.from(FORMATTER.parse(end)), uris);
        } else {
            return endpointHitRepository.findStatistics(Instant.from(FORMATTER.parse(start)), Instant.from(FORMATTER.parse(end)), uris);
        }
    }

    public void create(EndpointHitDto endpointHitDto) {
        log.info("Сохранение информации о запросе к эндпоинту endpointHitDto={}", endpointHitDto);
        EndpointHit endpointHit = EndpointHitMapper.toEndpointHit(endpointHitDto);
        endpointHitRepository.save(endpointHit);
    }
}
