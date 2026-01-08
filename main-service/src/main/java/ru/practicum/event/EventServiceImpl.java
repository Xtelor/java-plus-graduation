package ru.practicum.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.StatsClient;
import ru.practicum.category.Category;
import ru.practicum.category.CategoryRepository;
import ru.practicum.event.dto.*;
import ru.practicum.event.params.PublicEventsParam;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.ConditionsNotMetException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.user.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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
    @Transactional
    public EventFullDto createEvent(NewEventDto newEventDto, Long initiatorId) {
        log.info("Создание нового события");

        if (initiatorId == null || !userRepository.existsById(initiatorId)) {
            throw new NotFoundException("Инициатор события не найден");
        }

        if (newEventDto.getCategory() == null || !categoryRepository.existsById(newEventDto.getCategory())) {
            throw new NotFoundException("Категория события не найдена");
        }
        LocalDateTime eventDate = newEventDto.getEventDate() != null ? LocalDateTime.from(FORMATTER.parse(newEventDto.getEventDate())) : null;
        if (eventDate != null && Duration.between(LocalDateTime.now(), eventDate).toHours() < 2) {
            throw new ConditionsNotMetException("Дата и время на которые намечено событие не может быть раньше, чем через два часа от текущего момента");
        }

        User initiator = userRepository.getById(initiatorId);
        Category category = categoryRepository.getById(newEventDto.getCategory());

        Event event = EventMapper.toEntity(newEventDto);
        event.setInitiator(initiator);
        event.setCategory(category);
        event.setCreatedOn(LocalDateTime.now());
        event.setState(State.PENDING);

        Event savedEvent = eventRepository.save(event);
        log.info("Создано событие с ID: {}", savedEvent.getId());

        return EventMapper.toFullDto(savedEvent);
    }

    @Override
    @Transactional
    public EventFullDto updateEventUser(UpdateEventUserRequest updateEventUserRequest, Long initiatorId, Long eventId) {
        log.info("Обновление события администратором");

        if (eventId == null || !eventRepository.existsById(eventId)) {
            throw new NotFoundException("Событие не найдено");
        }

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

        Event oldEvent = eventRepository.getById(eventId);
        if (oldEvent.getState() != State.CANCELED && oldEvent.getState() != State.PENDING) {
            throw new ConditionsNotMetException("Изменять можно только отмененные события или события в состоянии ожидания модерации");
        }
        if (oldEvent.getInitiator().getId() != initiatorId) {
            throw new ValidationException("Изменить событие может только его инициатор");
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
        log.info("Обновление событие с ID: {} пользователем", eventId);

        return EventMapper.toFullDto(updatedEvent);
    }

    @Override
    @Transactional
    public EventFullDto updateEventAdmin(UpdateEventAdminRequest updateEventAdminRequest, Long eventId) {
        log.info("Обновление события администратором");

        if (eventId == null || !eventRepository.existsById(eventId)) {
            throw new NotFoundException("Событие не найдено");
        }

        if (updateEventAdminRequest.getCategory() != null && !categoryRepository.existsById(updateEventAdminRequest.getCategory())) {
            throw new NotFoundException("Категория события не найдена");
        }
        LocalDateTime eventDate = updateEventAdminRequest.getEventDate() != null ? LocalDateTime.from(FORMATTER.parse(updateEventAdminRequest.getEventDate())) : null;
        if (eventDate != null && Duration.between(LocalDateTime.now(), eventDate).toHours() < 2) {
            throw new ConditionsNotMetException("Дата и время на которые намечено событие не может быть раньше, чем через два часа от текущего момента");
        }

        Event oldEvent = eventRepository.getById(eventId);
        if (oldEvent.getState() != State.CANCELED && oldEvent.getState() != State.PENDING) {
            throw new ConditionsNotMetException("Изменять можно только отмененные события или события в состоянии ожидания модерации");
        }
        if (updateEventAdminRequest.getStateAction() == AdminStateAction.PUBLISH_EVENT) {
            oldEvent.setState(State.PUBLISHED);
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
        log.info("Обновление событие с ID: {}  администратором", eventId);

        return EventMapper.toFullDto(updatedEvent);
    }

    @Override
    public List<EventShortDto> findByInitiatorId(Long initiatorId, int from, int size) {
        Pageable pageable = PageRequest.of(from, size, Sort.by("eventDate"));
        return eventRepository.findByInitiatorId(initiatorId, pageable)
                .map(EventMapper::toShortDto)
                .getContent();
    }

    @Override
    public List<EventShortDto> getEventsPublic(PublicEventsParam publicEventsParam) {
        Pageable pageable = PageRequest.of(publicEventsParam.getFrom(), publicEventsParam.getSize(), Sort.by("eventDate"));
        return eventRepository.getEventsPublic(publicEventsParam, pageable)
                .map(EventMapper::toShortDto)
                .getContent();
    }
}
