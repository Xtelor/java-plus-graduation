package ru.practicum.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.Category;
import ru.practicum.category.CategoryRepository;
import ru.practicum.event.dto.*;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.mapper.RequestMapper;
import ru.practicum.request.model.Request;
import ru.practicum.request.model.RequestStatus;
import ru.practicum.request.repository.RequestRepository;
import ru.practicum.user.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final RequestRepository requestRepository;
    private final RequestMapper requestMapper;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneOffset.UTC);

    // Создание события
    @Override
    @Transactional
    public EventFullDto createEvent(NewEventDto newEventDto, Long initiatorId) {
        log.info("Создание нового события");

        // Валидация даты и времени, валидация и получение инициатора, категории
        checkEventDate(newEventDto.getEventDate());
        User initiator = getUserOrThrow(initiatorId);
        Category category = getCategoryOrThrow(newEventDto.getCategory());

        Event event = eventMapper.toEntity(newEventDto);
        event.setInitiator(initiator);
        event.setCategory(category);
        event.setCreatedOn(LocalDateTime.now());
        event.setState(State.PENDING);

        Event savedEvent = eventRepository.save(event);
        log.info("Создано событие с ID: {}", savedEvent.getId());

        return eventMapper.toFullDto(savedEvent);
    }

    // Получение полной информации о событии
    @Override
    public EventFullDto getEventByInitiator(Long initiatorId, Long eventId) {
        log.info("Получение события с id = {} для пользователя с id = {}", eventId, initiatorId);

        // Валидация инициатора и события, получение события
        getUserOrThrow(initiatorId);
        Event event = getEventOrThrow(eventId, initiatorId);

        return eventMapper.toFullDto(event);
    }

    // Редактирование события
    @Override
    @Transactional
    public EventFullDto updateEventByInitiator(Long initiatorId, Long eventId, UpdateEventUserRequest request) {
        log.info("Обновление события с id = {} пользователем с id = {}", eventId, initiatorId);

        getUserOrThrow(initiatorId);
        Event event = getEventOrThrow(eventId, initiatorId);

        if (event.getState() == State.PUBLISHED) {
            throw new ConflictException("Нельзя редактировать опубликованное событие.");
        }

        if (request.getEventDate() != null) {
            checkEventDate(request.getEventDate());
        }

        // Обновление полей
        updateEventCommonFields(event, request);
        updateEventState(event, request.getStateAction());

        return eventMapper.toFullDto(eventRepository.save(event));
    }

    // Получение заявок на участие
    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        log.info("Получение заявок на событие с id = {} пользователем с id = {}", eventId, userId);

        // Валидация
        getUserOrThrow(userId);
        getEventOrThrow(eventId, userId);

        return requestRepository.findAllByEventId(eventId).stream()
                .map(requestMapper::toDto)
                .collect(Collectors.toList());
    }

    // Изменение статуса заявок (Подтверждение/Отклонение)
    @Override
    @Transactional
    public EventRequestStatusUpdateResult changeRequestStatus(Long userId,
                                                              Long eventId,
                                                              EventRequestStatusUpdateRequest dto) {
        log.info("Изменение статуса заявок события с id = {} пользователем с id = {}", eventId, userId);

        // Валидация
        getUserOrThrow(userId);
        Event event = getEventOrThrow(eventId, userId);

        checkModerationRequired(event);

        long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        checkLimitNotReached(event, confirmedCount);

        List<Request> requests = requestRepository.findAllByIdIn(dto.getRequestIds());

        return processRequests(requests, event, dto.getStatus(), confirmedCount);
    }

    // Редактирование события администратором
    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request) {
        log.info("Администратор обновляет событие с id = {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено."));

        // Обновление полей
        updateAdminEventState(event, request.getStateAction());

        if (request.getEventDate() != null) {
            updateAdminEventDate(event, request.getEventDate());
        }
        updateEventCommonFields(event, request);

        return eventMapper.toFullDto(eventRepository.save(event));
    }

    // Валидация даты и времени
    private void checkEventDate(String eventDateStr) {
        LocalDateTime eventDate = LocalDateTime.from(FORMATTER.parse(eventDateStr));

        if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ConflictException("Дата и время на которые намечено событие не может быть раньше," +
                    " чем через два часа от текущего момента.");
        }
    }

    // Проверка существования инициатора события
    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Инициатор события не найден."));
    }

    // Проверка существования категории события
    private Category getCategoryOrThrow(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Категория события не найдена."));
    }

    // Проверка существования события
    private Event getEventOrThrow(Long eventId, Long initiatorId) {
        return eventRepository.findByIdAndInitiatorId(eventId, initiatorId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Событие с id = %d и user_id = %d не найдено.", eventId, initiatorId)
                ));
    }

    // Обновление состояния события
    private void updateEventState(Event event, String stateAction) {
        if (stateAction == null) return;

        switch (stateAction) {
            case "SEND_TO_REVIEW":
                event.setState(State.PENDING);
                break;
            case "CANCEL_REVIEW":
                event.setState(State.CANCELED);
                break;
        }
    }

    // Метод для обновления полей
    private void updateEventCommonFields(Event event, UpdateEventRequest request) {
        if (request.getAnnotation() != null) event.setAnnotation(request.getAnnotation());
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getTitle() != null) event.setTitle(request.getTitle());
        if (request.getPaid() != null) event.setPaid(request.getPaid());
        if (request.getParticipantLimit() != null) event.setParticipationLimit(request.getParticipantLimit());
        if (request.getRequestModeration() != null) event.setRequestModeration(request.getRequestModeration());
        if (request.getLocation() != null) event.setLocation(request.getLocation());

        if (request.getEventDate() != null) {
            event.setEventDate(LocalDateTime.from(FORMATTER.parse(request.getEventDate())));
        }

        if (request.getCategory() != null) {
            event.setCategory(getCategoryOrThrow(request.getCategory()));
        }
    }

    // Проверка необходимости модерации
    private void checkModerationRequired(Event event) {
        if (event.getParticipationLimit() == 0 || !event.getRequestModeration()) {
            throw new ConflictException("Модерация заявок не требуется (лимит = 0 или модерация отключена).");
        }
    }

    // Проверка лимита участников
    private void checkLimitNotReached(Event event, long confirmedCount) {
        if (event.getParticipationLimit() > 0 && confirmedCount >= event.getParticipationLimit()) {
            throw new ConflictException("Лимит участников исчерпан.");
        }
    }

    // Обработка запросов
    private EventRequestStatusUpdateResult processRequests(List<Request> requests,
                                                           Event event,
                                                           String status,
                                                           long confirmedCount) {
        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        for (Request request : requests) {
            checkRequestIsPending(request);

            if ("CONFIRMED".equals(status)) {
                if (event.getParticipationLimit() > 0 && confirmedCount >= event.getParticipationLimit()) {
                    rejectRequest(request, rejected);
                    throw new ConflictException("Лимит исчерпан в процессе подтверждения.");
                }
                confirmRequest(request, confirmed);
                confirmedCount++;
            } else if ("REJECTED".equals(status)) {
                rejectRequest(request, rejected);
            }
        }

        return new EventRequestStatusUpdateResult(confirmed, rejected);
    }

    // Валидация статуса заявки
    private void checkRequestIsPending(Request request) {
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new ConflictException("Статус можно менять только у заявок, находящихся в ожидании.");
        }
    }

    // Подтверждение запроса
    private void confirmRequest(Request request, List<ParticipationRequestDto> list) {
        request.setStatus(RequestStatus.CONFIRMED);
        requestRepository.save(request);
        list.add(requestMapper.toDto(request));
    }

    // Отклонение запроса
    private void rejectRequest(Request request, List<ParticipationRequestDto> list) {
        request.setStatus(RequestStatus.REJECTED);
        requestRepository.save(request);
        list.add(requestMapper.toDto(request));
    }

    // Смена статуса администратором
    private void updateAdminEventState(Event event, String stateAction) {
        if (stateAction == null) return;

        if (stateAction.equals("PUBLISH_EVENT")) {
            if (event.getState() != State.PENDING) {
                throw new ConflictException("Событие можно публиковать, только если оно в ожидании публикации.");
            }

            event.setState(State.PUBLISHED);
            event.setPublishedOn(LocalDateTime.now());
        } else if (stateAction.equals("REJECT_EVENT")) {
            if (event.getState() == State.PUBLISHED) {
                throw new ConflictException("Событие можно отклонить, только если оно еще не опубликовано.");
            }

            event.setState(State.CANCELED);
        }
    }

    // Обновление даты администратором
    private void updateAdminEventDate(Event event, String dateStr) {
        LocalDateTime newDate = LocalDateTime.from(FORMATTER.parse(dateStr));

        if (newDate.isBefore(LocalDateTime.now().plusHours(1))) {
            throw new ConflictException("Дата начала события должна быть не ранее" +
                    " чем за час от даты публикации.");
        }

        event.setEventDate(newDate);
    }
}
