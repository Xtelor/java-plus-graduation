package ru.practicum.mapper;

import ru.practicum.dto.admin.UserShortDto;
import ru.practicum.dto.categories.CategoryDto;
import ru.practicum.dto.events.EventFullDto;
import ru.practicum.dto.events.EventShortDto;
import ru.practicum.dto.events.NewEventDto;
import ru.practicum.entity.Event;
import ru.practicum.mappers.LocationMapper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class EventMapper {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneOffset.UTC);

    public static Event toEntity(NewEventDto newEventDto) {
        return Event.builder()
                .annotation(newEventDto.getAnnotation())
                .description(newEventDto.getDescription())
                .eventDate(LocalDateTime.from(FORMATTER.parse(newEventDto.getEventDate())))
                .location(LocationMapper.toEntity(newEventDto.getLocation()))
                .paid(newEventDto.getPaid())
                .participantLimit(newEventDto.getParticipantLimit())
                .requestModeration(newEventDto.getRequestModeration())
                .title(newEventDto.getTitle())
                .build();
    }

    public static EventShortDto toShortDto(Event event, CategoryDto categoryDto, UserShortDto initiatorDto) {
        return EventShortDto.builder()
                .annotation(event.getAnnotation())
                .category(categoryDto)
                .eventDate(event.getEventDate() != null ? FORMATTER.format(event.getEventDate()) : "")
                .id(event.getId())
                .initiator(initiatorDto)
                .paid(event.getPaid())
                .title(event.getTitle())
                .build();
    }

    public static EventFullDto toFullDto(Event event, CategoryDto categoryDto, UserShortDto initiatorDto) {
        return EventFullDto.builder()
                .annotation(event.getAnnotation())
                .category(categoryDto)
                .createdOn(event.getCreatedOn() != null ? FORMATTER.format(event.getCreatedOn()) : null)
                .description(event.getDescription())
                .eventDate(event.getEventDate() != null ? FORMATTER.format(event.getEventDate()) : "")
                .id(event.getId())
                .initiator(initiatorDto)
                .location(LocationMapper.toDto(event.getLocation()))
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .publishedOn(event.getPublishedOn() != null ? FORMATTER.format(event.getPublishedOn()) : null)
                .requestModeration(event.getRequestModeration())
                .state(event.getState() != null ? event.getState().toString() : null)
                .title(event.getTitle())
                .commentCount(0L)
                .build();
    }
}
