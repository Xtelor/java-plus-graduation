package ru.practicum.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.StatsClient;
import ru.practicum.ViewStatsDto;
import ru.practicum.dto.admin.UserDto;
import ru.practicum.dto.admin.UserShortDto;
import ru.practicum.dto.categories.CategoryDto;
import ru.practicum.dto.comments.CommentDto;
import ru.practicum.dto.events.*;
import ru.practicum.dto.requests.ParticipationRequestDto;
import ru.practicum.entity.Event;
import ru.practicum.enums.*;
import ru.practicum.exceptions.ConditionsNotMetException;
import ru.practicum.exceptions.NotFoundException;
import ru.practicum.exceptions.ValidationException;
import ru.practicum.feign.admin.UserClient;
import ru.practicum.feign.categories.PublicCategoryClient;
import ru.practicum.feign.comments.PublicCommentClient;
import ru.practicum.feign.requests.PrivateEventRequestClient;
import ru.practicum.mapper.EventMapper;
import ru.practicum.mappers.LocationMapper;
import ru.practicum.params.AdminEventsParam;
import ru.practicum.params.PublicEventsParam;
import ru.practicum.repository.EventRepository;

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
public class EventServiceImpl implements EventService {

    private final StatsClient statsClient;
    private final EventRepository eventRepository;
    private final UserClient userClient;
    private final PublicCategoryClient publicCategoryClient;
    private final PrivateEventRequestClient privateEventRequestClient;
    private final PublicCommentClient publicCommentClient;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneOffset.UTC);

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> findByInitiatorId(Long initiatorId, int from, int size) {

        log.info("Получение событий, добавленных пользователем с id: {} пользователем", initiatorId);

        if (initiatorId == null) {
            throw new NotFoundException("Инициатор события не найден");
        }

        getUserOrThrow(initiatorId);

        int pageFrom = (from > 0 && size > 0) ? from / size : 0;
        int pageSize = size > 0 ? size : 10;

        Pageable pageable = PageRequest.of(pageFrom, pageSize, Sort.by("eventDate"));

        List<Event> events = eventRepository.findByInitiatorId(initiatorId, pageable).getContent();
        Map<Long, Long> views = getViews(events);
        Map<Long, Long> confirmedRequests = getConfirmedRequestsForEvents(events);

        List<EventShortDto> eventsShortDto = events
                .stream()
                .map(event -> EventMapper.toShortDto(
                        event,
                        getCategoryOrNull(event.getCategoryId()),
                        toUserShortDto(getUserOrThrow(event.getInitiatorId()))
                ))
                .collect(Collectors.toList());

        for (EventShortDto event : eventsShortDto) {
            event.setViews(views.getOrDefault(event.getId(), 0L));
            event.setConfirmedRequests(confirmedRequests.getOrDefault(event.getId(), 0L));
        }

        return eventsShortDto;
    }

    @Override
    @Transactional
    public EventFullDto createEvent(NewEventDto newEventDto, Long initiatorId) {

        log.info("Создание нового события");

        UserDto initiator = getUserOrThrow(initiatorId);

        CategoryDto category = getCategoryOrThrow(newEventDto.getCategory());

        LocalDateTime eventDate = newEventDto.getEventDate() != null ?
                LocalDateTime.from(FORMATTER.parse(newEventDto.getEventDate())) : null;

        if (eventDate != null && Duration.between(LocalDateTime.now(), eventDate).toHours() < 2) {
            throw new ValidationException("Дата и время на которые намечено событие не может быть раньше, " +
                    "чем через два часа от текущего момента");
        }

        if (newEventDto.getPaid() == null) {
            newEventDto.setPaid(false);
        }
        if (newEventDto.getParticipantLimit() == null) {
            newEventDto.setParticipantLimit(0);
        }
        if (newEventDto.getRequestModeration() == null) {
            newEventDto.setRequestModeration(true);
        }
        if (newEventDto.getParticipantLimit() < 0) {
            throw new ValidationException("Лимит участников не может быть отрицательным");
        }

        Event event = EventMapper.toEntity(newEventDto);
        event.setInitiatorId(initiatorId);
        event.setCategoryId(newEventDto.getCategory());
        event.setCreatedOn(LocalDateTime.now());
        event.setState(State.PENDING);

        Event savedEvent = eventRepository.save(event);
        EventFullDto eventFullDto = EventMapper.toFullDto(savedEvent, category, toUserShortDto(initiator));
        eventFullDto.setViews(0L);
        eventFullDto.setConfirmedRequests(0L);
        log.info("Создано событие с ID: {}", savedEvent.getId());

        return eventFullDto;
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto findByIdAndInitiatorId(Long initiatorId, Long eventId) {

        log.info("Получение полной информации о событии с id: {}, добавленного пользователем с id {}",
                eventId, initiatorId);

        if (initiatorId == null) {
            throw new NotFoundException("Инициатор события не найден");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));

        if (!Objects.equals(event.getInitiatorId(), initiatorId)) {
            throw new NotFoundException("Событие не найдено у текущего пользователя");
        }

        UserDto initiator = getUserOrThrow(initiatorId);

        EventFullDto eventFullDto = EventMapper.toFullDto(
                event,
                getCategoryOrNull(event.getCategoryId()),
                toUserShortDto(initiator)
        );

        List<String> uris = new ArrayList<>();
        uris.add("/events/" + eventId);

        Long views = statsClient.getStats(
                        LocalDateTime.now().minusYears(1),
                        LocalDateTime.now().plusDays(1),
                        uris,
                        true
                )
                .stream()
                .map(ViewStatsDto::getHits)
                .findFirst()
                .orElse(0L);

        eventFullDto.setViews(views);


        eventFullDto.setConfirmedRequests(0L);
        eventFullDto.setCommentCount(0L);

        return eventFullDto;
    }

    @Override
    @Transactional
    public EventFullDto updateEventUser(UpdateEventUserRequest updateEventUserRequest,
                                        Long initiatorId,
                                        Long eventId) {

        log.info("Обновление события пользователем");

        Event oldEvent = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));

        if (initiatorId == null) {
            throw new NotFoundException("Инициатор события не найден");
        }

        if (updateEventUserRequest.getCategory() != null
                && !Objects.equals(updateEventUserRequest.getCategory(), oldEvent.getCategoryId())) {
            getCategoryOrThrow(updateEventUserRequest.getCategory());
        }

        LocalDateTime eventDate = updateEventUserRequest.getEventDate() != null ?
                LocalDateTime.from(FORMATTER.parse(updateEventUserRequest.getEventDate())) : null;

        if (eventDate != null && Duration.between(LocalDateTime.now(), eventDate).toHours() < 2) {
            throw new ValidationException("Дата и время на которые намечено событие не может быть раньше, " +
                    "чем через два часа от текущего момента");
        }

        if (oldEvent.getState() != State.CANCELED && oldEvent.getState() != State.PENDING) {
            throw new ConditionsNotMetException("Изменять можно только отмененные события или " +
                    "события в состоянии ожидания модерации");
        }

        if (updateEventUserRequest.getStateAction() == UserStateAction.SEND_TO_REVIEW) {
            oldEvent.setState(State.PENDING);
        } else if (updateEventUserRequest.getStateAction() == UserStateAction.CANCEL_REVIEW) {
            oldEvent.setState(State.CANCELED);
        }

        if (updateEventUserRequest.getAnnotation() != null) {
            oldEvent.setAnnotation(updateEventUserRequest.getAnnotation());
        }

        if (updateEventUserRequest.getCategory() != null) {
            oldEvent.setCategoryId(updateEventUserRequest.getCategory());
        }

        if (updateEventUserRequest.getDescription() != null) {
            oldEvent.setDescription(updateEventUserRequest.getDescription());
        }

        if (updateEventUserRequest.getEventDate() != null) {
            oldEvent.setEventDate(LocalDateTime.from(FORMATTER.parse(updateEventUserRequest.getEventDate())));
        }

        if (updateEventUserRequest.getLocation() != null) {
            oldEvent.setLocation(LocationMapper.toEntity(updateEventUserRequest.getLocation()));
        }

        if (updateEventUserRequest.getPaid() != null) {
            oldEvent.setPaid(updateEventUserRequest.getPaid());
        }

        if (updateEventUserRequest.getParticipantLimit() != null) {
            oldEvent.setParticipantLimit(updateEventUserRequest.getParticipantLimit());
        }

        if (updateEventUserRequest.getRequestModeration() != null) {
            oldEvent.setRequestModeration(updateEventUserRequest.getRequestModeration());
        }

        if (updateEventUserRequest.getTitle() != null) {
            oldEvent.setTitle(updateEventUserRequest.getTitle());
        }

        Event updatedEvent = eventRepository.save(oldEvent);

        EventFullDto eventFullDto = EventMapper.toFullDto(
                updatedEvent,
                getCategoryForResponse(updatedEvent.getCategoryId()),
                getUserShortForResponse(updatedEvent.getInitiatorId())
        );

        eventFullDto.setViews(getEventViewsSafely(eventId));
        eventFullDto.setConfirmedRequests(getConfirmedRequestsCount(updatedEvent));

        log.info("Обновление событие с ID: {} пользователем", eventId);

        return eventFullDto;
    }

    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(UpdateEventAdminRequest updateEventAdminRequest, Long eventId) {
        log.info("Обновление события администратором");

        Event oldEvent = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));

        if (updateEventAdminRequest.getCategory() != null) {
            getCategoryOrThrow(updateEventAdminRequest.getCategory());
        }

        LocalDateTime eventDate = updateEventAdminRequest.getEventDate() != null
                ? LocalDateTime.from(FORMATTER.parse(updateEventAdminRequest.getEventDate()))
                : null;

        if (eventDate != null && Duration.between(LocalDateTime.now(), eventDate).toHours() < 2) {
            throw new ValidationException("Дата и время на которые намечено событие не может быть раньше, " +
                    "чем через два часа от текущего момента");
        }

        if (updateEventAdminRequest.getStateAction() == AdminStateAction.PUBLISH_EVENT) {
            if (oldEvent.getState() != State.PENDING) {
                throw new ConditionsNotMetException("Событие можно публиковать только из состояния ожидания модерации");
            }

            oldEvent.setState(State.PUBLISHED);
            oldEvent.setPublishedOn(LocalDateTime.now());

        } else if (updateEventAdminRequest.getStateAction() == AdminStateAction.REJECT_EVENT) {
            if (oldEvent.getState() == State.PUBLISHED) {
                throw new ConditionsNotMetException("Нельзя отклонить опубликованное событие");
            }

            oldEvent.setState(State.CANCELED);
        }

        if (updateEventAdminRequest.getAnnotation() != null) {
            oldEvent.setAnnotation(updateEventAdminRequest.getAnnotation());
        }

        if (updateEventAdminRequest.getCategory() != null) {
            oldEvent.setCategoryId(updateEventAdminRequest.getCategory());
        }

        if (updateEventAdminRequest.getDescription() != null) {
            oldEvent.setDescription(updateEventAdminRequest.getDescription());
        }

        if (updateEventAdminRequest.getEventDate() != null) {
            oldEvent.setEventDate(LocalDateTime.from(FORMATTER.parse(updateEventAdminRequest.getEventDate())));
        }

        if (updateEventAdminRequest.getLocation() != null) {
            oldEvent.setLocation(LocationMapper.toEntity(updateEventAdminRequest.getLocation()));
        }

        if (updateEventAdminRequest.getPaid() != null) {
            oldEvent.setPaid(updateEventAdminRequest.getPaid());
        }

        if (updateEventAdminRequest.getParticipantLimit() != null) {
            oldEvent.setParticipantLimit(updateEventAdminRequest.getParticipantLimit());
        }

        if (updateEventAdminRequest.getRequestModeration() != null) {
            oldEvent.setRequestModeration(updateEventAdminRequest.getRequestModeration());
        }

        if (updateEventAdminRequest.getTitle() != null) {
            oldEvent.setTitle(updateEventAdminRequest.getTitle());
        }

        Event updatedEvent = eventRepository.save(oldEvent);
        EventFullDto eventFullDto = EventMapper.toFullDto(
                updatedEvent,
                getCategoryOrNull(updatedEvent.getCategoryId()),
                toUserShortDto(getUserOrThrow(updatedEvent.getInitiatorId()))
        );

        List<String> uris = new ArrayList<>();
        uris.add("/events/" + eventId);

        Long views = statsClient.getStats(LocalDateTime.now().minusYears(1),
                        LocalDateTime.now().plusDays(1), uris, true)
                .stream()
                .map(ViewStatsDto::getHits)
                .findFirst()
                .orElse(0L);

        eventFullDto.setViews(views);
        eventFullDto.setConfirmedRequests(getConfirmedRequestsCount(updatedEvent));

        log.info("Обновление событие с ID: {} администратором", eventId);

        return eventFullDto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getEventsPublic(PublicEventsParam publicEventsParam, String ip, String uri) {

        log.info("Получение опубликованных событий");

        LocalDateTime rangeStart = publicEventsParam.getRangeStart() != null
                ? LocalDateTime.from(FORMATTER.parse(publicEventsParam.getRangeStart()))
                : null;

        LocalDateTime rangeEnd = publicEventsParam.getRangeEnd() != null
                ? LocalDateTime.from(FORMATTER.parse(publicEventsParam.getRangeEnd()))
                : null;

        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new ValidationException("Дата начала поиска не может быть позже даты окончания");
        }

        int from = (publicEventsParam.getFrom() != null) ? publicEventsParam.getFrom() : 0;
        int size = (publicEventsParam.getSize() != null && publicEventsParam.getSize() > 0)
                ? publicEventsParam.getSize() : 10;

        Pageable pageable = PageRequest.of(from / size, size, Sort.by("eventDate"));

        List<Event> events = eventRepository.getEventsPublic(publicEventsParam, pageable).getContent();

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Long> views = getViews(events);
        Map<Long, Long> confirmedRequests = getConfirmedRequestsForEvents(events);

        List<EventShortDto> eventsShortDto = events
                .stream()
                .map(event -> EventMapper.toShortDto(
                        event,
                        getCategoryOrNull(event.getCategoryId()),
                        getUserShortOrUnknown(event.getInitiatorId())
                ))
                .collect(Collectors.toList());

        for (EventShortDto event : eventsShortDto) {
            event.setViews(views.getOrDefault(event.getId(), 0L));
            event.setConfirmedRequests(confirmedRequests.getOrDefault(event.getId(), 0L));
        }

        if (publicEventsParam.getSort() == SortEvents.VIEWS) {
            eventsShortDto = eventsShortDto.stream()
                    .sorted(Comparator.comparing(EventShortDto::getViews).reversed())
                    .collect(Collectors.toList());
        }

        statsClient.hit("events", uri, ip, LocalDateTime.now());
        return eventsShortDto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventFullDto> searchEventsByAdmin(AdminEventsParam adminEventsParam) {

        log.info("Получение событий администратором");

        Pageable pageable = PageRequest.of(
                adminEventsParam.getFrom() / adminEventsParam.getSize(),
                adminEventsParam.getSize(),
                Sort.by("eventDate")
        );

        List<Event> events = eventRepository.searchEventsByAdmin(adminEventsParam, pageable).getContent();

        Map<Long, Long> views = getViews(events);
        Map<Long, Long> confirmedRequests = getConfirmedRequestsForEvents(events);

        return events.stream()
                .map(event -> {
                    EventFullDto dto = EventMapper.toFullDto(
                            event,
                            getCategoryOrNull(event.getCategoryId()),
                            toUserShortDto(getUserOrThrow(event.getInitiatorId()))
                    );
                    dto.setViews(views.getOrDefault(event.getId(), 0L));
                    dto.setConfirmedRequests(confirmedRequests.getOrDefault(event.getId(), 0L));
                    dto.setCommentCount(getCommentCount(event.getId()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto findById(Long eventId, String ip, String uri) {

        Event event = eventRepository.findByIdPublished(eventId);

        if (event == null) {
            throw new NotFoundException("Событие не найдено или недоступно");
        }

        EventFullDto eventFullDto = EventMapper.toFullDto(
                event,
                getCategoryOrNull(event.getCategoryId()),
                getUserShortOrUnknown(event.getInitiatorId())
        );

        List<String> uris = new ArrayList<>();
        uris.add("/events/" + eventId);

        //statsClient.hit("ewm-event-service", uri, ip, LocalDateTime.now());
        statsClient.hit("events", uri, ip, LocalDateTime.now());

        Long views = statsClient.getStats(LocalDateTime.now().minusYears(1),
                        LocalDateTime.now().plusDays(1), uris, true)
                .stream()
                .map(ViewStatsDto::getHits)
                .findFirst()
                .orElse(0L);

        eventFullDto.setViews(views);
        eventFullDto.setConfirmedRequests(getConfirmedRequestsCount(event));

        // Добавляем количество комментариев
        eventFullDto.setCommentCount(getCommentCount(eventId));


        return eventFullDto;
    }

    @Override
    public EventFullDto getInternalEventById(Long eventId) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));

        EventFullDto dto = EventMapper.toFullDto(
                event,
                getCategoryOrNull(event.getCategoryId()),
                toUserShortDto(getUserOrThrow(event.getInitiatorId()))
        );

        dto.setViews(0L);
        dto.setConfirmedRequests(0L);
        dto.setCommentCount(0L);

        return dto;
    }

    @Override
    public boolean existsByCategoryId(Long categoryId) {
        return eventRepository.existsByCategoryId(categoryId);
    }

    // Получения количества комментариев
    private Long getCommentCount(Long eventId) {

        List<CommentDto> comments = publicCommentClient.getEventComments(eventId, 0, Integer.MAX_VALUE);
        return (long) comments.size();
    }

    private Map<Long, Long> getViews(List<Event> events) {

        if (events == null || events.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, Long> uris = events
                .stream()
                .collect(Collectors.toMap(
                        currentEvent -> "/events/" + currentEvent.getId(),
                        Event::getId)
                );

        return statsClient.getStats(LocalDateTime.now().minusYears(1), LocalDateTime.now().plusDays(1),
                        uris.keySet().stream().toList(), true)
                .stream()
                .collect(Collectors.toMap(
                        currentViewStatDto -> uris.get(currentViewStatDto.getUri()),
                        ViewStatsDto::getHits)
                );
    }

    private Map<Long, Long> getConfirmedRequestsForEvents(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Long> confirmedRequests = new HashMap<>();

        for (Event event : events) {
            try {
                List<ParticipationRequestDto> results = privateEventRequestClient
                        .getEventParticipants(event.getInitiatorId(), event.getId());

                long confirmedCount = results == null ? 0L : results.stream()
                        .filter(result ->
                                Objects.equals(String.valueOf(result.getStatus()), RequestStatus.CONFIRMED.name()))
                        .count();

                confirmedRequests.put(event.getId(), confirmedCount);

            } catch (Exception e) {
                log.warn("Не удалось получить заявки для события {}: {}", event.getId(), e.getMessage());
                confirmedRequests.put(event.getId(), 0L);
            }
        }

        return confirmedRequests;
    }

    private Long getConfirmedRequestsCount(Event event) {

        try {
            List<ParticipationRequestDto> results = privateEventRequestClient
                    .getEventParticipants(event.getInitiatorId(), event.getId());

            return results == null ? 0L : results.stream()
                    .filter(result ->
                            Objects.equals(String.valueOf(result.getStatus()), RequestStatus.CONFIRMED.name()))
                    .count();
        } catch (Exception e) {
            return 0L;
        }
    }

    // Получение DTO пользователя
    private UserDto getUserOrThrow(Long userId) {

        List<UserDto> users = userClient.getUsers(List.of(userId), 0, 1);

        if (users == null || users.isEmpty()) {
            throw new NotFoundException("Пользователь не найден");
        }

        return users.getFirst();
    }

    private UserShortDto toUserShortDto(UserDto userDto) {

        return UserShortDto.builder()
                .id(userDto.getId())
                .name(userDto.getName())
                .build();
    }

    private CategoryDto getCategoryOrThrow(Long categoryId) {

        try {
            return publicCategoryClient.getCategoryById(categoryId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("Категория события не найдена");
        }
    }

    private CategoryDto getCategoryOrNull(Long categoryId) {

        try {
            return publicCategoryClient.getCategoryById(categoryId);
        } catch (FeignException.NotFound e) {
            return null;
        } catch (Exception e) {
            log.warn("Не удалось получить категорию {}: {}", categoryId, e.getMessage());
            return CategoryDto.builder()
                    .id(categoryId)
                    .name("unknown")
                    .build();
        }
    }

    private Long getEventViewsSafely(Long eventId) {
        try {
            return statsClient.getStats(
                            LocalDateTime.now().minusYears(1),
                            LocalDateTime.now().plusDays(1),
                            List.of("/events/" + eventId),
                            true
                    )
                    .stream()
                    .map(ViewStatsDto::getHits)
                    .findFirst()
                    .orElse(0L);
        } catch (Exception e) {
            log.warn("Не удалось получить просмотры для события {}: {}", eventId, e.getMessage());
            return 0L;
        }
    }

    private UserShortDto getUserShortForResponse(Long userId) {

        try {
            return toUserShortDto(getUserOrThrow(userId));
        } catch (Exception e) {
            log.warn("Не удалось получить пользователя {} для ответа: {}", userId, e.getMessage());
            return UserShortDto.builder()
                    .id(userId)
                    .name("unknown")
                    .build();
        }
    }

    private CategoryDto getCategoryForResponse(Long categoryId) {

        try {
            return publicCategoryClient.getCategoryById(categoryId);
        } catch (Exception e) {
            log.warn("Не удалось получить категорию {} для ответа: {}", categoryId, e.getMessage());
            return CategoryDto.builder()
                    .id(categoryId)
                    .name("unknown")
                    .build();
        }
    }

    private UserShortDto getUserShortOrUnknown(Long userId) {
        try {
            return toUserShortDto(getUserOrThrow(userId));
        } catch (Exception e) {
            log.warn("Не удалось получить пользователя {}: {}", userId, e.getMessage());
            return UserShortDto.builder()
                    .id(userId)
                    .name("unknown")
                    .build();
        }
    }
}
