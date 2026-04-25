package ru.practicum;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.exception.StatsServerUnavailableException;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class StatsClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final DiscoveryClient discoveryClient;
    private final RetryTemplate retryTemplate;

    private static final String statsServiceId = "stats-server";

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneOffset.UTC);

    private static final ParameterizedTypeReference<List<ViewStatsDto>> LIST_TYPE =
            new ParameterizedTypeReference<>() {};

    public StatsClient(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;

        this.retryTemplate = new RetryTemplate();

        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(3000L);
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);

        MaxAttemptsRetryPolicy retryPolicy = new MaxAttemptsRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);

        log.info("StatsClient инициализирован с DiscoveryClient");
    }

    // Сохранение информации о том, что к эндпоинту был запрос
    public void hit(String app, String uri, String ip, LocalDateTime timestamp) {
        EndpointHitDto hitDto = EndpointHitDto.builder()
                .app(app)
                .uri(uri)
                .ip(ip)
                .timestamp(FORMATTER.format(timestamp))
                .build();
        try {
            restTemplate.postForEntity(makeUri("/hit"), hitDto, Void.class);
            log.debug("Информация сохранена: app={}, uri={}, ip={}", app, uri, ip);
        } catch (Exception e) {
            log.error("Ошибка сохранения информации: {}", e.getMessage());
        }
    }

    // Получение статистики по посещениям
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end,
                                       List<String> uris, Boolean unique) {
        try {
            String url = UriComponentsBuilder.fromUri(makeUri("/stats"))
                    .queryParam("start", start == null ? null : encode(start))
                    .queryParam("end", end == null ? null : encode(end))
                    .queryParamIfPresent("uris", Optional.ofNullable(uris)
                            .filter(list -> !list.isEmpty()))
                    .queryParamIfPresent("unique", Optional.ofNullable(unique))
                    .build(false)
                    .toUriString();

            log.debug("Получена статистика для {}", url);
            return Optional.ofNullable(
                    restTemplate.exchange(url, HttpMethod.GET, null, LIST_TYPE).getBody()
            ).orElse(List.of());
        } catch (Exception e) {
            log.warn("Не удалось собрать статистику: {}", e.getMessage());
            return List.of();
        }
    }

    // Получение экземпляра сервиса статистики из Eureka
    private ServiceInstance getInstance() {
        try {
            return discoveryClient.getInstances(statsServiceId).getFirst();
        } catch (Exception exception) {
            throw new StatsServerUnavailableException(
                    "Ошибка обнаружения адреса сервиса статистики с id: " + statsServiceId
            );
        }
    }

    // Получение URL сервера с учётом повторных попыток
    private URI makeUri(String path) {
        ServiceInstance instance = retryTemplate.execute(cxt -> getInstance());
        return URI.create("http://" + instance.getHost() + ":" + instance.getPort() + path);
    }

    // Кодировка даты и времени
    private String encode(LocalDateTime dateTime) {
        return URLEncoder.encode(FORMATTER.format(dateTime), StandardCharsets.UTF_8);
    }
}