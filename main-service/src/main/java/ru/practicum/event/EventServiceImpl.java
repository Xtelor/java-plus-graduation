package ru.practicum.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.MethodArgumentNotValidException;
import ru.practicum.category.Category;
import ru.practicum.category.CategoryMapper;
import ru.practicum.category.CategoryRepository;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.NewEventDto;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.user.*;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneOffset.UTC);

    @Override
    @Transactional
    public EventFullDto createEvent(NewEventDto newEventDto, Long initiatorId) {
        log.info("Создание нового события");

        if (initiatorId == null || !userRepository.existsById(initiatorId)) {
            throw new NotFoundException("Инициатор события не найден");
        }

        if (newEventDto.getCategory() == null || !categoryRepository.existsById(newEventDto.getCategory())) {
            throw new NotFoundException("Категория события не найдена");
        }
        LocalDateTime eventDate = newEventDto.getEventDate() != null ? LocalDateTime.from(FORMATTER.parse(newEventDto.getEventDate())) : null;
        if (Duration.between(LocalDateTime.now(), eventDate).toHours() < 2) {
            throw new ValidationException("Дата и время на которые намечено событие не может быть раньше, чем через два часа от текущего момента");
        }

        User initiator = userRepository.getById(initiatorId);
        Category category = categoryRepository.getById(newEventDto.getCategory());

        Event event = eventMapper.toEntity(newEventDto);
        event.setInitiator(initiator);
        event.setCategory(category);
        event.setCreatedOn(LocalDateTime.now());
        event.setState(State.PENDING);

        Event savedEvent = eventRepository.save(event);
        log.info("Создано событие с ID: {}", savedEvent.getId());

        EventFullDto eventFullDto = EventFullDto.builder()
                .annotation(savedEvent.getAnnotation())
                .category(category != null ? categoryMapper.toDto(category) : null)
                .createdOn(FORMATTER.format(savedEvent.getCreatedOn()))
                .description(savedEvent.getDescription())
                .eventDate(savedEvent.getEventDate() != null ? FORMATTER.format(savedEvent.getEventDate()) : "")
                .id(savedEvent.getId())
                .initiator(initiator != null ? userMapper.toShortDto(initiator) : null)
                .location(savedEvent.getLocation())
                .paid(newEventDto.isPaid())
                .participationLimit(savedEvent.getParticipationLimit())
                .publishedOn(savedEvent.getPublishedOn() != null ? FORMATTER.format(savedEvent.getPublishedOn()) : "")
                .requestModeration(savedEvent.isRequestModeration())
                .state(savedEvent.getState().toString())
                .title(newEventDto.getTitle())
                .build();

        return eventFullDto;
    }
//
//    @Override
//    public List<EventShortDto> getEventsByInitiatorId(Long initiatorId){
//
//    }
}
