package ru.practicum.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.category.CategoryMapper;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.NewEventDto;
import ru.practicum.user.UserMapper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class EventMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneOffset.UTC);

    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;


    public Event toEntity(NewEventDto newEventDto) {
        return Event.builder()
                .annotation(newEventDto.getAnnotation())
                .description(newEventDto.getDescription())
                .eventDate(LocalDateTime.from(FORMATTER.parse(newEventDto.getEventDate())))
                .location(newEventDto.getLocation())
                .paid(newEventDto.getPaid())
                .participationLimit(newEventDto.getParticipationLimit())
                .requestModeration(newEventDto.getRequestModeration())
                .title(newEventDto.getTitle())
                .build();
    }

    public EventShortDto toShortDto(Event event) {
        if (event == null) {
            return null;
        }

        return EventShortDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .annotation(event.getAnnotation())
                .category(categoryMapper.toDto(event.getCategory()))
                .paid(event.getPaid())
                .initiator(userMapper.toShortDto(event.getInitiator()))
                .eventDate(event.getEventDate().format(FORMATTER))
                .confirmedRequests(0L)
                .views(event.getViews())
                .build();
    }

    public EventFullDto toFullDto(Event event) {
        if (event == null) return null;

        return EventFullDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .annotation(event.getAnnotation())
                .description(event.getDescription())
                .paid(event.getPaid())
                .requestModeration(event.getRequestModeration())
                .participationLimit(event.getParticipationLimit())
                .eventDate(event.getEventDate().format(FORMATTER))
                .createdOn(event.getCreatedOn().format(FORMATTER))
                .publishedOn(event.getPublishedOn() != null ? event.getPublishedOn().format(FORMATTER) : null)
                .state(event.getState().toString())
                .views(event.getViews())
                .confirmedRequests(0L)
                .category(categoryMapper.toDto(event.getCategory()))
                .initiator(userMapper.toShortDto(event.getInitiator()))
                .location(event.getLocation())
                .build();
    }
}
