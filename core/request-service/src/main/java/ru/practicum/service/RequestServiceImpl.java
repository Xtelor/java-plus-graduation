package ru.practicum.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.admin.UserDto;
import ru.practicum.dto.events.EventFullDto;
import ru.practicum.dto.requests.ParticipationRequestDto;
import ru.practicum.entity.Request;
import ru.practicum.enums.RequestStatus;
import ru.practicum.enums.State;
import ru.practicum.exceptions.ConflictException;
import ru.practicum.exceptions.NotFoundException;
import ru.practicum.feign.admin.UserClient;
import ru.practicum.feign.events.InternalEventClient;
import ru.practicum.mapper.RequestMapper;
import ru.practicum.repository.RequestRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final UserClient userClient;
    private final InternalEventClient internalEventClient;


    // Добавление запроса на участие в событии
    @Override
    @Transactional
    public ParticipationRequestDto addRequest(Long userId, Long eventId) {

        log.info("Пользователь userId = {} создает запрос на участие в событии eventId = {}", userId, eventId);

        // Проверка на добавление повторного запроса
        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            log.warn("Попытка повторного запроса: userId = {}, eventId = {}", userId, eventId);
            throw new ConflictException("Нельзя добавить повторный запрос.");
        }

        // Проверка существования пользователя и события, валидация события
        findUserById(userId);
        EventFullDto event = findEventById(eventId);
        validateEventForRequest(event, userId);

        Request request = Request.builder()
                .created(LocalDateTime.now())
                .eventId(eventId)
                .requesterId(userId)
                .status(RequestStatus.PENDING)
                .build();

        if (!event.getRequestModeration() || Objects.equals(event.getParticipantLimit(), 0)) {
            log.info("Заявка подтверждена автоматически (лимит = 0 или модерация отключена): eventId = {}", eventId);
            request.setStatus(RequestStatus.CONFIRMED);
        }

        Request savedRequest = requestRepository.save(request);
        log.info("Запрос успешно создан: requestId = {}, status = {}", savedRequest.getId(), savedRequest.getStatus());

        return RequestMapper.toDto(savedRequest);
    }

    // Отмена запроса на участие в событии
    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {

        log.info("Пользователь userId = {} отменяет запрос requestId = {}", userId, requestId);

        // Проверка существования пользователя
        findUserById(userId);

        // Проверка и получение запроса
        Request request = findRequestAndCheckOwner(requestId, userId);

        if (Objects.equals(request.getStatus(), RequestStatus.CONFIRMED)) {
            throw new ConflictException("Нельзя отменить уже подтвержденную заявку.");
        }

        request.setStatus(RequestStatus.CANCELED);

        Request savedRequest = requestRepository.save(request);
        log.info("Запрос requestId = {} переведен в статус CANCELED", requestId);

        return RequestMapper.toDto(savedRequest);
    }

    // Получение списка запросов текущего пользователя на участие в чужих событиях
    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {

        log.info("Получение всех запросов пользователя userId = {}", userId);

        // Проверка существования пользователя
        findUserById(userId);

        List<ParticipationRequestDto> requests = requestRepository.findAllByRequesterId(userId).stream()
                .map(RequestMapper::toDto)
                .collect(Collectors.toList());

        log.info("Найдено {} запросов для userId = {}", requests.size(), userId);

        return requests;
    }

    // Проверка существования пользователя
    private void findUserById(Long userId) {

        List<UserDto> users = userClient.getUsers(List.of(userId), 0 , 1);

        if (users == null || users.isEmpty()) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден.");
        }
    }

    // Проверка существования события
    private EventFullDto findEventById(Long eventId) {

        try {
            return internalEventClient.getById(eventId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("Событие с id = " + eventId + " не найдено.");
        }
    }

    // Валидация события для запроса
    private void validateEventForRequest(EventFullDto event, Long userId) {

        if (Objects.equals(event.getInitiator().getId(), userId)) {
            throw new ConflictException("Инициатор события не может добавить запрос на участие в своём событии.");
        }

        if (!Objects.equals(event.getState(), State.PUBLISHED.name())) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии.");
        }

        if (event.getParticipantLimit() > 0) {
            long confirmedRequests = requestRepository.countByEventIdAndStatus(
                    event.getId(),
                    RequestStatus.CONFIRMED
            );

            if (confirmedRequests >= event.getParticipantLimit()) {
                throw new ConflictException("У события достигнут лимит запросов на участие.");
            }
        }
    }

    // Проверка существования запроса и проверка запроса пользователя
    private Request findRequestAndCheckOwner(Long requestId, Long userId) {

        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Запрос с id = " + requestId + " не найден."));

        if (!Objects.equals(request.getRequesterId(), userId)) {
            throw new NotFoundException("Запрос с id = " + requestId + " не найден у текущего пользователя.");
        }
        return request;
    }
}
