package ru.practicum.event;

import org.springframework.stereotype.Component;
import ru.practicum.category.CategoryMapper;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.NewEventDto;
import ru.practicum.user.User;
import ru.practicum.user.UserDto;
import ru.practicum.user.UserMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class EventMapper {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneOffset.UTC);

    public static Event toEntity(NewEventDto newEventDto) {
        return Event.builder()
                .annotation(newEventDto.getAnnotation())
                .description(newEventDto.getDescription())
                .eventDate(LocalDateTime.from(FORMATTER.parse(newEventDto.getEventDate())))
                .location(newEventDto.getLocation())
                .paid(newEventDto.isPaid())
                .participationLimit(newEventDto.getParticipationLimit())
                .requestModeration(newEventDto.isRequestModeration())
                .title(newEventDto.getTitle())
                .build();
    }

    public static EventShortDto toShortDto(Event event) {
        return EventShortDto.builder()
                .annotation(event.getAnnotation())
                .category(event.getCategory() != null ? CategoryMapper.toDto(event.getCategory()) : null)
                .eventDate(event.getEventDate() != null ? FORMATTER.format(event.getEventDate()) : "")
                .id(event.getId())
                .initiator(event.getInitiator() != null ? UserMapper.toShortDto(event.getInitiator()) : null)
                .paid(event.isPaid())
                .title(event.getTitle())
                .build();
    }
}
