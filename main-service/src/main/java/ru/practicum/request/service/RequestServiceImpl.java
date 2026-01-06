package ru.practicum.request.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.event.Event;
import ru.practicum.event.EventRepository;
import ru.practicum.event.State;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.request.RequestRepository;
import ru.practicum.request.RequestService;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.mapper.RequestMapper;
import ru.practicum.request.model.Request;
import ru.practicum.request.model.RequestStatus;
import ru.practicum.user.User;
import ru.practicum.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RequestMapper requestMapper;

    // Добавление запроса на участие в событии
    @Override
    public ParticipationRequestDto addRequest(Long userId, Long eventId) {
        // Проверка на добавление повторного запроса
        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("Нельзя добавить повторный запрос.");
        }

        // Проверка существования пользователя и события, валидация события
        User user = findUserById(userId);
        Event event = findEventById(eventId);
        validateEventForRequest(event, userId);

        Request request = Request.builder()
                .created(LocalDateTime.now())
                .event(event)
                .requester(user)
                .status(RequestStatus.PENDING)
                .build();

        if (!event.getRequestModeration() || event.getParticipationLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
        }

        return requestMapper.toDto(requestRepository.save(request));
    }

    // Отмена запроса на участие в событии
    @Override
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        // Проверка существования пользователя
        findUserById(userId);

        // Проверка и получение запроса
        Request request = findRequestAndCheckOwner(requestId, userId);

        request.setStatus(RequestStatus.CANCELED);
        return requestMapper.toDto(requestRepository.save(request));
    }

    // Получение списка запросов текущего пользователя на участие в чужих событиях
    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        // Проверка существования пользователя
        findUserById(userId);

        return requestRepository.findAllByRequesterId(userId).stream()
                .map(requestMapper::toDto)
                .collect(Collectors.toList());
    }

    // Проверка существования пользователя
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id = " + userId + " не найден."));
    }

    // Проверка существования события
    private Event findEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id = " + eventId + " не найдено."));
    }

    // Валидация события для запроса
    private void validateEventForRequest(Event event, Long userId) {
        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Инициатор события не может добавить запрос на участие в своём событии.");
        }

        if (event.getState() != State.PUBLISHED) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии.");
        }

        if (event.getParticipationLimit() > 0) {
            Long confirmedRequests = requestRepository
                    .countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);

            if (confirmedRequests >= event.getParticipationLimit()) {
                throw new ConflictException("У события достигнут лимит запросов на участие.");
            }
        }
    }

    // Проверка существования запроса и проверка запроса пользователя
    private Request findRequestAndCheckOwner(Long requestId, Long userId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Запрос с id=" + requestId + " не найден"));

        if (!Objects.equals(request.getRequester().getId(), userId)) {
            throw new NotFoundException("Запрос с id=" + requestId + " не найден у текущего пользователя");
        }
        return request;
    }
}
