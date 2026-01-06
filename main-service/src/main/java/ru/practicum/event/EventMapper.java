package ru.practicum.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.category.CategoryMapper;
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
                .paid(newEventDto.isPaid())
                .participationLimit(newEventDto.getParticipationLimit())
                .requestModeration(newEventDto.isRequestModeration())
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
                .paid(event.isPaid())
                .initiator(userMapper.toShortDto(event.getInitiator()))
                .eventDate(event.getEventDate().format(FORMATTER))
                .confirmedRequests(0L)
                .views(event.getViews())
                .build();
    }
}
