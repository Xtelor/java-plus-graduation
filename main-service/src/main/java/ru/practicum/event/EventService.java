package ru.practicum.event;

import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.NewEventDto;

import java.util.List;

public interface EventService {

    EventFullDto createEvent(NewEventDto NewEventDto, Long initiatorId);

//    List<EventShortDto> getEventsByInitiatorId(Long initiatorId);
}
