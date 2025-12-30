package ru.practicum.event;

import org.springframework.stereotype.Component;
import ru.practicum.event.dto.NewEventDto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class EventMapper {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneOffset.UTC);

    public Event toEntity(NewEventDto newEventDto) {
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
}
