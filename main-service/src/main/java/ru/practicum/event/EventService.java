package ru.practicum.event;

import ru.practicum.event.dto.*;
import ru.practicum.event.params.PublicEventsParam;

import java.util.List;

public interface EventService {

    EventFullDto createEvent(NewEventDto NewEventDto, Long initiatorId);

    EventFullDto updateEventUser(UpdateEventUserRequest updateEventUserRequest, Long initiatorId, Long eventId);

    EventFullDto updateEventAdmin(UpdateEventAdminRequest updateEventAdminRequest, Long eventId);

    List<EventShortDto> findByInitiatorId(Long initiatorId, int from, int size);

    List<EventShortDto> getEventsPublic(PublicEventsParam publicEventsParam);
}
