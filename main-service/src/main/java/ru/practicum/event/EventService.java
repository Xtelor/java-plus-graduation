package ru.practicum.event;

import ru.practicum.event.dto.*;
import ru.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request.dto.ParticipationRequestDto;

import java.util.List;

public interface EventService {

    // Создание события
    EventFullDto createEvent(NewEventDto NewEventDto, Long initiatorId);

    // Получение полной информации о событии
    EventFullDto getEventByInitiator(Long initiatorId, Long eventId);

    // Получение заявок на участие в событии текущего пользователя
    List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId);

    // Изменение статуса заявок на участие в событии текущего пользователя
    EventRequestStatusUpdateResult changeRequestStatus(Long userId,
                                                       Long eventId,
                                                       EventRequestStatusUpdateRequest dto);

    // Редактирование события
    EventFullDto updateEventByInitiator(Long initiatorId,
                                        Long eventId,
                                        UpdateEventUserRequest request);

    // Редактирование события администратором
    EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request);
}
