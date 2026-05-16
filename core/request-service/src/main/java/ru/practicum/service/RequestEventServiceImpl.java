package ru.practicum.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.admin.UserDto;
import ru.practicum.dto.events.EventFullDto;
import ru.practicum.dto.requests.EventRequestStatusUpdateRequest;
import ru.practicum.dto.requests.EventRequestStatusUpdateResult;
import ru.practicum.dto.requests.ParticipationRequestDto;
import ru.practicum.entity.Request;
import ru.practicum.enums.RequestStatus;
import ru.practicum.exceptions.ConflictException;
import ru.practicum.exceptions.NotFoundException;
import ru.practicum.feign.admin.UserClient;
import ru.practicum.feign.events.PrivateEventClient;
import ru.practicum.mapper.RequestMapper;
import ru.practicum.repository.RequestRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RequestEventServiceImpl implements RequestEventService {

    private final RequestRepository requestRepository;
    private final UserClient userClient;
    private final PrivateEventClient privateEventClient;


    @Override
    public List<ParticipationRequestDto> getEventParticipants(Long userId, Long eventId) {

        log.info("Получение запросов на участие в событии eventId = {} пользователем userId = {}", eventId, userId);

        // Проверка существования пользователя
        findUserById(userId);

        List<ParticipationRequestDto> requests = requestRepository.findAllByEventId(eventId).stream()
                .map(RequestMapper::toDto)
                .collect(Collectors.toList());

        log.info("Найдено {} запросов для события eventId = {}", requests.size(), eventId);
        return requests;
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult changeRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest updateRequest) {

        log.info("Изменение статуса запросов для события eventId = {} пользователем userId = {}", eventId, userId);

        // Проверка существования пользователя
        findUserById(userId);

        // Проверка существования события и что пользователь является инициатором
        EventFullDto event = findEventById(userId, eventId);

        // Проверка лимита участников
        if (event.getParticipantLimit() > 0) {

            Long confirmedRequests = requestRepository
                    .countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);

            if (confirmedRequests >= event.getParticipantLimit()) {
                throw new ConflictException("Достигнут лимит одобренных заявок");
            }
        }

        List<Request> requestsToUpdate = requestRepository.findAllByIdIn(updateRequest.getRequestIds());

        // Проверка, что все запросы относятся к данному событию
        for (Request request : requestsToUpdate) {

            if (!Objects.equals(request.getEventId(), eventId)) {
                throw new NotFoundException("Запрос с id = " + request.getId() + " не относится к событию " + eventId);
            }
        }

        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        String status = updateRequest.getStatus();
        Long confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);

        for (Request request : requestsToUpdate) {

            if (!Objects.equals(request.getStatus(), RequestStatus.PENDING)) {
                throw new ConflictException("Статус можно изменить только у заявок, находящихся в состоянии ожидания");
            }

            if (Objects.equals(status, "CONFIRMED")) {

                if (Objects.equals(event.getParticipantLimit(), 0) || !event.getRequestModeration()) {
                    request.setStatus(RequestStatus.CONFIRMED);
                    confirmed.add(RequestMapper.toDto(request));
                } else if (event.getParticipantLimit() > 0 && confirmedRequests < event.getParticipantLimit()) {
                    request.setStatus(RequestStatus.CONFIRMED);
                    confirmed.add(RequestMapper.toDto(request));
                    confirmedRequests++;
                } else {
                    request.setStatus(RequestStatus.REJECTED);
                    rejected.add(RequestMapper.toDto(request));
                }
            } else if (Objects.equals(status, "REJECTED")) {
                request.setStatus(RequestStatus.REJECTED);
                rejected.add(RequestMapper.toDto(request));
            }

            requestRepository.save(request);
        }

        // Если при подтверждении лимит исчерпан, отклоняем все оставшиеся
        if (Objects.equals(status, "CONFIRMED") && event.getParticipantLimit() > 0 &&
                confirmedRequests >= event.getParticipantLimit()) {
            List<Request> pendingRequests = requestRepository.findAllByEventId(eventId).stream()
                    .filter(r -> Objects.equals(r.getStatus(), RequestStatus.PENDING))
                    .toList();

            for (Request request : pendingRequests) {
                request.setStatus(RequestStatus.REJECTED);
                requestRepository.save(request);
                rejected.add(RequestMapper.toDto(request));
            }
        }

        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();
        result.setConfirmedRequests(confirmed);
        result.setRejectedRequests(rejected);

        return result;
    }

    @Override
    public Map<Long, Long> getConfirmedRequestsCountByEvents(List<Long> eventIds) {

        if (eventIds == null || eventIds.isEmpty()) {
            return new HashMap<>();
        }

        List<Object[]> results = requestRepository.countByEventIdsAndStatus(
                eventIds, RequestStatus.CONFIRMED);

        Map<Long, Long> confirmedCounts = new HashMap<>();

        for (Object[] row : results) {
            Long eventId = (Long) row[0];
            Long count = (Long) row[1];
            confirmedCounts.put(eventId, count);
        }

        for (Long eventId : eventIds) {
            confirmedCounts.putIfAbsent(eventId, 0L);
        }

        return confirmedCounts;
    }

    private void findUserById(Long userId) {

        List<UserDto> users = userClient.getUsers(List.of(userId), 0, 1);

        if (users == null || users.isEmpty()) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден.");
        }
    }

    private EventFullDto findEventById(Long userId, Long eventId) {

        try {
            return privateEventClient.findByIdAndInitiatorId(userId, eventId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("Событие с id = " + eventId + " не найдено у текущего пользователя.");
        }
    }
}