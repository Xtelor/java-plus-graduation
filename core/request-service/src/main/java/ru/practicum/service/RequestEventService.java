package ru.practicum.service;

import ru.practicum.dto.requests.EventRequestStatusUpdateRequest;
import ru.practicum.dto.requests.EventRequestStatusUpdateResult;
import ru.practicum.dto.requests.ParticipationRequestDto;

import java.util.List;
import java.util.Map;

public interface RequestEventService {

    // Получение информации о запросах на участие в событии текущего пользователя
    List<ParticipationRequestDto> getEventParticipants(Long userId, Long eventId);

    // Изменение статуса заявок на участие в событии текущего пользователя
    EventRequestStatusUpdateResult changeRequestStatus(Long userId, Long eventId,
                                                       EventRequestStatusUpdateRequest updateRequest);

    Map<Long, Long> getConfirmedRequestsCountByEvents(List<Long> eventIds);
}