package ru.practicum.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.AnalyzerClient;
import ru.practicum.dto.compilations.CompilationDto;
import ru.practicum.dto.compilations.NewCompilationDto;
import ru.practicum.dto.compilations.UpdateCompilationRequest;
import ru.practicum.dto.events.EventFullDto;
import ru.practicum.dto.events.EventShortDto;
import ru.practicum.entity.Compilation;
import ru.practicum.exceptions.NotFoundException;
import ru.practicum.feign.events.InternalEventClient;
import ru.practicum.mapper.CompilationMapper;
import ru.practicum.repository.CompilationRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final InternalEventClient internalEventClient;
    private final AnalyzerClient analyzerClient;

    @Override
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto newCompilationDto) {

        log.info("Создание подборки: {}", newCompilationDto.getTitle());

        Compilation compilation = CompilationMapper.toEntity(newCompilationDto);

        if (newCompilationDto.getEvents() != null && !newCompilationDto.getEvents().isEmpty()) {
            compilation.setEvents(new HashSet<>(newCompilationDto.getEvents()));
        }

        Compilation savedCompilation = compilationRepository.save(compilation);
        log.info("Подборка создана с ID: {}", savedCompilation.getId());

        Compilation compilationWithEvents = getCompilationWithEvents(savedCompilation.getId());

        return convertCompilationToDto(compilationWithEvents);
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compilationId, UpdateCompilationRequest request) {

        log.info("Обновление подборки ID: {}", compilationId);

        Compilation compilation = getCompilationWithEvents(compilationId);

        if (request.getTitle() != null) {
            compilation.setTitle(request.getTitle());
        }

        if (request.getPinned() != null) {
            compilation.setPinned(request.getPinned());
        }

        if (request.getEvents() != null) {
            Set<Long> events = findEventsByIds(request.getEvents());
            compilation.setEvents(events);
        }

        Compilation updatedCompilation = compilationRepository.save(compilation);
        log.info("Подборка ID {} обновлена", compilationId);

        return convertCompilationToDto(updatedCompilation);
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compilationId) {

        log.info("Удаление подборки ID: {}", compilationId);

        checkCompilationExists(compilationId);
        compilationRepository.deleteById(compilationId);

        log.info("Подборка ID {} удалена", compilationId);
    }

    @Override
    public List<CompilationDto> getAllCompilations(Boolean pinned, Pageable pageable) {

        log.info("Получение подборок, pinned: {}", pinned);

        List<Compilation> compilations = compilationRepository
                .findAllCompilationsWithEvents(pinned, pageable);

        log.info("Найдено {} подборок", compilations.size());

        if (compilations.isEmpty()) {
            return Collections.emptyList();
        }

        return convertCompilationsToDtoList(compilations);
    }

    @Override
    public CompilationDto getCompilationById(Long compilationId) {

        log.info("Получение подборки ID: {}", compilationId);
        Compilation compilation = getCompilationWithEvents(compilationId);

        return convertCompilationToDto(compilation);
    }

    private CompilationDto convertCompilationToDto(Compilation compilation) {

        Set<EventShortDto> events = new HashSet<>();

        if (compilation.getEvents() != null && !compilation.getEvents().isEmpty()) {
            events = convertEventsToShortDtos(new ArrayList<>(compilation.getEvents()));
        }

        return CompilationMapper.toDto(compilation, events);
    }

    private List<CompilationDto> convertCompilationsToDtoList(List<Compilation> compilations) {

        List<Long> allEvents = compilations.stream()
                .filter(c -> c.getEvents() != null && !c.getEvents().isEmpty())
                .flatMap(c -> c.getEvents().stream())
                .distinct()
                .toList();

        List<EventFullDto> allEventDtos = allEvents.stream()
                .map(this::getEventById)
                .filter(Objects::nonNull)
                .toList();

        Map<Long, Double> ratingsMap = getRatingsForEvents(
                allEventDtos.stream().map(EventFullDto::getId).collect(Collectors.toList())
        );

        Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsForEvents(allEventDtos);

        Map<Long, EventShortDto> eventDtoMap = allEventDtos.stream()
                .collect(Collectors.toMap(
                        EventFullDto::getId,
                        event -> createEventShortDtoWithStats(event, ratingsMap, confirmedRequestsMap)
                ));

        return compilations.stream()
                .map(compilation -> {
                    Set<EventShortDto> events = new HashSet<>();

                    if (compilation.getEvents() != null && !compilation.getEvents().isEmpty()) {
                        events = compilation.getEvents().stream()
                                .map(eventDtoMap::get)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet());
                    }

                    // Вызываем маппер
                    return CompilationMapper.toDto(compilation, events);
                })
                .collect(Collectors.toList());
    }

    private Set<EventShortDto> convertEventsToShortDtos(List<Long> events) {

        if (events.isEmpty()) {
            return Collections.emptySet();
        }

        List<EventFullDto> eventDtos = events.stream()
                .map(this::getEventById)
                .filter(Objects::nonNull)
                .toList();

        List<Long> eventIds = eventDtos.stream()
                .map(EventFullDto::getId)
                .collect(Collectors.toList());

        Map<Long, Double> ratingsMap = getRatingsForEvents(eventIds);
        Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsForEvents(eventDtos);

        return eventDtos.stream()
                .map(event -> createEventShortDtoWithStats(event, ratingsMap, confirmedRequestsMap))
                .collect(Collectors.toSet());
    }

    private EventShortDto createEventShortDtoWithStats(EventFullDto event,
                                                       Map<Long, Double> ratingsMap,
                                                       Map<Long, Long> confirmedRequestsMap) {
        EventShortDto dto = EventShortDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .annotation(event.getAnnotation())
                .category(event.getCategory())
                .eventDate(event.getEventDate())
                .initiator(event.getInitiator())
                .paid(event.getPaid())
                .build();

        dto.setRating(ratingsMap.getOrDefault(event.getId(), 0.0));
        dto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(event.getId(), 0L));

        return dto;
    }

    private Map<Long, Double> getRatingsForEvents(List<Long> eventIds) {

        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            return analyzerClient.getRatings(eventIds);
        } catch (Exception e) {
            log.warn("Не удалось получить рейтинги: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private Map<Long, Long> getConfirmedRequestsForEvents(List<EventFullDto> events) {

        if (events.isEmpty()) {
            return Collections.emptyMap();
        }

        return events.stream()
                .collect(Collectors.toMap(
                        EventFullDto::getId,
                        event -> event.getConfirmedRequests() == null ? 0L : event.getConfirmedRequests()
                ));
    }

    private Compilation getCompilationWithEvents(Long compilationId) {

        return compilationRepository.findByIdWithEvents(compilationId)
                .orElseThrow(() -> {
                    log.error("Подборка с ID {} не найдена", compilationId);
                    return new NotFoundException(
                            String.format("Подборка с id = %d не найдена", compilationId)
                    );
                });
    }

    private void checkCompilationExists(Long compilationId) {

        if (!compilationRepository.existsById(compilationId)) {
            log.error("Подборка с ID {} не найдена", compilationId);
            throw new NotFoundException(
                    String.format("Подборка с id = %d не найдена", compilationId)
            );
        }
    }

    private EventFullDto getEventById(Long eventId) {
        try {
            return internalEventClient.getById(eventId);
        } catch (FeignException e) {
            log.warn("Событие с ID {} недоступно: {}", eventId, e.getMessage());
            return null;
        }
    }

    private Set<Long> findEventsByIds(Set<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(eventIds);
    }
}