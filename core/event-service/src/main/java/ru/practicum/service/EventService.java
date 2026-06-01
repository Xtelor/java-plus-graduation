package ru.practicum.service;

import ru.practicum.dto.events.*;
import ru.practicum.params.AdminEventsParam;
import ru.practicum.params.PublicEventsParam;

import java.util.List;

public interface EventService {

    EventFullDto createEvent(NewEventDto newEventDto, Long initiatorId);

    EventFullDto updateEventUser(UpdateEventUserRequest updateEventUserRequest, Long initiatorId, Long eventId);

    EventFullDto updateEventByAdmin(UpdateEventAdminRequest updateEventAdminRequest, Long eventId);

    List<EventShortDto> findByInitiatorId(Long initiatorId, int from, int size);

    List<EventShortDto> getEventsPublic(PublicEventsParam publicEventsParam);

    List<EventFullDto> searchEventsByAdmin(AdminEventsParam adminEventsParam);

    EventFullDto findById(Long eventId, long userId);

    EventFullDto findById(Long eventId);

    EventFullDto findByIdAndInitiatorId(Long initiatorId, Long eventId);

    EventFullDto getInternalEventById(Long eventId);

    boolean existsByCategoryId(Long categoryId);

    List<EventShortDto> getByIds(List<Long> eventIds);

    List<EventShortDto> getRecommendations(long userId, int size);

    void likeEvent(long userId, long eventId);
}
