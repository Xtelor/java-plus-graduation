package ru.practicum.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.StatsClient;
import ru.practicum.ViewStatsDto;
import ru.practicum.category.Category;
import ru.practicum.category.CategoryRepository;
import ru.practicum.event.dto.*;
import ru.practicum.event.params.AdminEventsParam;
import ru.practicum.event.params.PublicEventsParam;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.ConditionsNotMetException;
import ru.practicum.event.dto.*;
import ru.practicum.exception.NotFoundException;
import ru.practicum.user.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final StatsClient statsClient;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneOffset.UTC);

    @Override
    public List<EventShortDto> findByInitiatorId(Long initiatorId, int from, int size) {
        log.info("Получение событий, добавленных пользователем с id: {} пользователем", initiatorId);
        if (initiatorId == null || !userRepository.existsById(initiatorId)) {
            throw new NotFoundException("Инициатор события не найден");
        }
        Pageable pageable = PageRequest.of(from, size, Sort.by("eventDate"));
        List<Event> events = eventRepository.findByInitiatorId(initiatorId, pageable).getContent();
        Map<Long, Long> views = getViews(events);
        List<EventShortDto> eventsShortDto = events
                .stream()
                .map(EventMapper::toShortDto)
                .collect(Collectors.toList());
        for (EventShortDto event : eventsShortDto) {
            if (views.get(event.getId()) != null) {
                event.setViews(views.get(event.getId()));
            } else {
                event.setViews(0L);
            }
        }
        return eventsShortDto;
    }

    @Override
    @Transactional
    public EventFullDto createEvent(NewEventDto newEventDto, Long initiatorId) {
        log.info("Создание нового события");

        User initiator = userRepository.findById(initiatorId)
                .orElseThrow(() -> new NotFoundException("Инициатор события не найден"));
        Category category = categoryRepository.findById(newEventDto.getCategory())
                .orElseThrow(() -> new NotFoundException("Категория события не найдена"));

        LocalDateTime eventDate = newEventDto.getEventDate() != null ? LocalDateTime.from(FORMATTER.parse(newEventDto.getEventDate())) : null;
        if (eventDate != null && Duration.between(LocalDateTime.now(), eventDate).toHours() < 2) {
            throw new ConditionsNotMetException("Дата и время на которые намечено событие не может быть раньше, чем через два часа от текущего момента");
        }

        Event event = EventMapper.toEntity(newEventDto);
        event.setInitiator(initiator);
        event.setCategory(category);
        event.setCreatedOn(LocalDateTime.now());
        event.setState(State.PENDING);

        Event savedEvent = eventRepository.save(event);
        EventFullDto eventFullDto = EventMapper.toFullDto(savedEvent);
        eventFullDto.setViews(0L);
        log.info("Создано событие с ID: {}", savedEvent.getId());

        return eventFullDto;
    }

    @Override
    public EventFullDto findByIdAndInitiatorId(Long initiatorId, Long eventId) {
        log.info("Получение полной информации о событии с id: {}, добавленного пользователем с id {}", eventId, initiatorId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));
        if (initiatorId == null || !userRepository.existsById(initiatorId)) {
            throw new NotFoundException("Инициатор события не найден");
        }
        if (Objects.equals(event.getInitiator().getId(), initiatorId)) {
            throw new NotFoundException("Текущий пользователь не является инициатором события");
        }
        EventFullDto eventFullDto = EventMapper.toFullDto(event);
        List<String> uris = new ArrayList<>();
        uris.add("/events/" + eventId);
        Long views = statsClient.getStats(null, null, uris, false)
                .stream()
                .map(ViewStatsDto::getHits)
                .findFirst()
                .orElse(0L);
        eventFullDto.setViews(views);
        return eventFullDto;
    }

    @Override
    @Transactional
    public EventFullDto updateEventUser(UpdateEventUserRequest updateEventUserRequest, Long initiatorId, Long eventId) {
        log.info("Обновление события администратором");

        Event oldEvent = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));
        if (initiatorId == null || !userRepository.existsById(initiatorId)) {
            throw new NotFoundException("Инициатор события не найден");
        }
        if (updateEventUserRequest.getCategory() != null && !categoryRepository.existsById(updateEventUserRequest.getCategory())) {
            throw new NotFoundException("Категория события не найдена");
        }
        LocalDateTime eventDate = updateEventUserRequest.getEventDate() != null ? LocalDateTime.from(FORMATTER.parse(updateEventUserRequest.getEventDate())) : null;
        if (eventDate != null && Duration.between(LocalDateTime.now(), eventDate).toHours() < 2) {
            throw new ConditionsNotMetException("Дата и время на которые намечено событие не может быть раньше, чем через два часа от текущего момента");
        }
        if (oldEvent.getState() != State.CANCELED && oldEvent.getState() != State.PENDING) {
            throw new ConditionsNotMetException("Изменять можно только отмененные события или события в состоянии ожидания модерации");
        }
        if (Objects.equals(oldEvent.getInitiator().getId(), initiatorId)) {
            throw new NotFoundException("Изменить событие может только его инициатор");
        }
        if (updateEventUserRequest.getStateAction() == UserStateAction.CANCEL_REVIEW) {
            oldEvent.setState(State.CANCELED);
        }
        if (updateEventUserRequest.getAnnotation() != null) {
            oldEvent.setAnnotation(updateEventUserRequest.getAnnotation());
        }
        if (updateEventUserRequest.getCategory() != null) {
            oldEvent.setCategory(categoryRepository.getById(updateEventUserRequest.getCategory()));
        }
        if (updateEventUserRequest.getDescription() != null) {
            oldEvent.setDescription(updateEventUserRequest.getDescription());
        }
        if (updateEventUserRequest.getEventDate() != null) {
            oldEvent.setEventDate(LocalDateTime.from(FORMATTER.parse(updateEventUserRequest.getEventDate())));
        }
        if (updateEventUserRequest.getLocation() != null) {
            oldEvent.setLocation(updateEventUserRequest.getLocation());
        }
        if (updateEventUserRequest.getPaid() != null) {
            oldEvent.setPaid(updateEventUserRequest.getPaid());
        }
        if (updateEventUserRequest.getParticipationLimit() != null) {
            oldEvent.setParticipationLimit(updateEventUserRequest.getParticipationLimit());
        }
        if (updateEventUserRequest.getRequestModeration() != null) {
            oldEvent.setRequestModeration(updateEventUserRequest.getRequestModeration());
        }
        if (updateEventUserRequest.getTitle() != null) {
            oldEvent.setTitle(updateEventUserRequest.getTitle());
        }

        Event updatedEvent = eventRepository.save(oldEvent);
        EventFullDto eventFullDto = EventMapper.toFullDto(updatedEvent);
        List<String> uris = new ArrayList<>();
        uris.add("/events/" + eventId);
        Long views = statsClient.getStats(null, null, uris, false)
                .stream()
                .map(ViewStatsDto::getHits)
                .findFirst()
                .orElse(0L);
        eventFullDto.setViews(views);
        log.info("Обновление событие с ID: {} пользователем", eventId);

        return eventFullDto;
    }

    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(UpdateEventAdminRequest updateEventAdminRequest, Long eventId) {
        log.info("Обновление события администратором");

        Event oldEvent = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));

        if (updateEventAdminRequest.getCategory() != null && !categoryRepository.existsById(updateEventAdminRequest.getCategory())) {
            throw new NotFoundException("Категория события не найдена");
        }
        LocalDateTime eventDate = updateEventAdminRequest.getEventDate() != null ? LocalDateTime.from(FORMATTER.parse(updateEventAdminRequest.getEventDate())) : null;
        if (eventDate != null && Duration.between(LocalDateTime.now(), eventDate).toHours() < 2) {
            throw new ConditionsNotMetException("Дата и время на которые намечено событие не может быть раньше, чем через два часа от текущего момента");
        }
        if (oldEvent.getState() != State.CANCELED && oldEvent.getState() != State.PENDING) {
            throw new ConditionsNotMetException("Изменять можно только отмененные события или события в состоянии ожидания модерации");
        }
        if (updateEventAdminRequest.getStateAction() == AdminStateAction.PUBLISH_EVENT) {
            oldEvent.setState(State.PUBLISHED);
            oldEvent.setPublishedOn(LocalDateTime.now());
        } else if (updateEventAdminRequest.getStateAction() == AdminStateAction.REJECT_EVENT) {
            oldEvent.setState(State.CANCELED);
        } else {
            throw new IllegalArgumentException("Некорректное изменение состояния события");
        }

        if (updateEventAdminRequest.getAnnotation() != null) {
            oldEvent.setAnnotation(updateEventAdminRequest.getAnnotation());
        }
        if (updateEventAdminRequest.getCategory() != null) {
            oldEvent.setCategory(categoryRepository.getById(updateEventAdminRequest.getCategory()));
        }
        if (updateEventAdminRequest.getDescription() != null) {
            oldEvent.setDescription(updateEventAdminRequest.getDescription());
        }
        if (updateEventAdminRequest.getEventDate() != null) {
            oldEvent.setEventDate(LocalDateTime.from(FORMATTER.parse(updateEventAdminRequest.getEventDate())));
        }
        if (updateEventAdminRequest.getLocation() != null) {
            oldEvent.setLocation(updateEventAdminRequest.getLocation());
        }
        if (updateEventAdminRequest.getPaid() != null) {
            oldEvent.setPaid(updateEventAdminRequest.getPaid());
        }
        if (updateEventAdminRequest.getParticipationLimit() != null) {
            oldEvent.setParticipationLimit(updateEventAdminRequest.getParticipationLimit());
        }
        if (updateEventAdminRequest.getRequestModeration() != null) {
            oldEvent.setRequestModeration(updateEventAdminRequest.getRequestModeration());
        }
        if (updateEventAdminRequest.getTitle() != null) {
            oldEvent.setTitle(updateEventAdminRequest.getTitle());
        }

        Event updatedEvent = eventRepository.save(oldEvent);
        EventFullDto eventFullDto = EventMapper.toFullDto(updatedEvent);
        List<String> uris = new ArrayList<>();
        uris.add("/events/" + eventId);
        Long views = statsClient.getStats(null, null, uris, false)
                .stream()
                .map(ViewStatsDto::getHits)
                .findFirst()
                .orElse(0L);
        eventFullDto.setViews(views);
        log.info("Обновление событие с ID: {}  администратором", eventId);

        return eventFullDto;
    }

    @Override
    public List<EventShortDto> getEventsPublic(PublicEventsParam publicEventsParam, String ip, String uri) {
        log.info("Получение опубликованных событий");
        Pageable pageable = PageRequest.of(publicEventsParam.getFrom(), publicEventsParam.getSize(), Sort.by("eventDate"));
        List<Event> events =  eventRepository.getEventsPublic(publicEventsParam, pageable).getContent();
        Map<Long, Long> views = getViews(events);
        List<EventShortDto> eventsShortDto = events
                .stream()
                .map(EventMapper::toShortDto)
                .collect(Collectors.toList());
        for (EventShortDto event : eventsShortDto) {
            if (views.get(event.getId()) != null) {
                event.setViews(views.get(event.getId()));
            } else {
                event.setViews(0L);
            }
        }
        statsClient.hit("ewm-main-service", uri, ip, LocalDateTime.now());
        if (publicEventsParam.getSort() == SortEvents.VIEWS) {
            return eventsShortDto.stream()
                    .sorted(Comparator.comparing(EventShortDto::getViews).reversed())
                    .collect(Collectors.toList());
        }
        return eventsShortDto;
    }

    @Override
    public List<EventFullDto> searchEventsByAdmin(AdminEventsParam adminEventsParam) {
        log.info("Получение событий администратором");
        Pageable pageable = PageRequest.of(adminEventsParam.getFrom(), adminEventsParam.getSize(), Sort.by("eventDate"));
        List<Event> events =  eventRepository.searchEventsByAdmin(adminEventsParam, pageable).getContent();
        Map<Long, Long> views = getViews(events);
        List<EventFullDto> eventsFullDto = events
                .stream()
                .map(EventMapper::toFullDto)
                .collect(Collectors.toList());
        for (EventFullDto event : eventsFullDto) {
            if (views.get(event.getId()) != null) {
                event.setViews(views.get(event.getId()));
            } else {
                event.setViews(0L);
            }
        }
        return eventsFullDto;
    }

    @Override
    public EventFullDto findById(Long eventId, String ip, String uri) {
        Event event = eventRepository.findByIdPublished(eventId);
        if (event == null) {
            throw new NotFoundException("Событие не найдено или недоступно");
        }
        EventFullDto eventFullDto = EventMapper.toFullDto(event);
        List<String> uris = new ArrayList<>();
        uris.add("/events/" + eventId);
        Long views = statsClient.getStats(null, null, uris, false)
                .stream()
                .map(ViewStatsDto::getHits)
                .findFirst()
                .orElse(0L);
        eventFullDto.setViews(views);
        statsClient.hit("ewm-main-service", uri, ip, LocalDateTime.now());
        return eventFullDto;
    }

    Map<Long, Long> getViews(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return new HashMap<>();
        }
        Map<String, Long> uris = events
                .stream()
                .collect(Collectors.toMap(
                        currentEvent -> "/events/" + currentEvent.getId(),
                        Event::getId)
                );
        return statsClient.getStats(null, LocalDateTime.now(), uris.keySet().stream().toList(), false)
                .stream()
                .collect(Collectors.toMap(
                        currentViewStatDto -> uris.get(currentViewStatDto.getUri()),
                        ViewStatsDto::getHits)
                );
    }
}
