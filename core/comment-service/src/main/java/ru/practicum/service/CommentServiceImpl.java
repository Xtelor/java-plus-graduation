package ru.practicum.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.admin.UserDto;
import ru.practicum.dto.admin.UserShortDto;
import ru.practicum.dto.comments.CommentDto;
import ru.practicum.dto.comments.NewCommentDto;
import ru.practicum.dto.events.EventFullDto;
import ru.practicum.dto.requests.ParticipationRequestDto;
import ru.practicum.entity.Comment;
import ru.practicum.enums.CommentStatus;
import ru.practicum.enums.RequestStatus;
import ru.practicum.enums.State;
import ru.practicum.exceptions.ConditionsNotMetException;
import ru.practicum.exceptions.NotFoundException;
import ru.practicum.feign.admin.UserClient;
import ru.practicum.feign.events.PublicEventClient;
import ru.practicum.feign.requests.PrivateRequestClient;
import ru.practicum.mapper.CommentMapper;
import ru.practicum.repository.CommentRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserClient userClient;
    private final PublicEventClient eventClient;
    private final PrivateRequestClient requestClient;

    @Override
    @Transactional
    public CommentDto createComment(Long userId, Long eventId, NewCommentDto newCommentDto) {

        log.info("Создание комментария пользователем {} к событию {}", userId, eventId);

        // Проверяем существование пользователя
        UserDto authorDto = getUserOrThrow(userId);

        // Проверяем существование события
        EventFullDto event = getEventOrThrow(eventId);

        // Проверяем, что событие опубликовано
        if (!Objects.equals(State.PUBLISHED.name(), event.getState())) {
            throw new ConditionsNotMetException("Комментировать можно только опубликованные события");
        }

        // Проверяем, что пользователь посетил событие (имел подтвержденную заявку)
        List<ParticipationRequestDto> userRequests = requestClient.getUserRequests(userId);
        boolean hasParticipated = userRequests.stream()
                .anyMatch(request -> Objects.equals(request.getEvent(), eventId)
                        && Objects.equals(RequestStatus.CONFIRMED.name(), String.valueOf(request.getStatus())));

        if (!hasParticipated) {
            throw new ConditionsNotMetException("Комментировать могут только участники события");
        }

        // Создаем комментарий - ВСЕ комментарии на модерацию
        Comment comment = CommentMapper.toEntity(newCommentDto);
        comment.setAuthorId(userId);
        comment.setEventId(eventId);
        comment.setStatus(CommentStatus.PENDING); // Всегда на модерации

        Comment savedComment = commentRepository.save(comment);
        log.info("Создан комментарий с ID {}", savedComment.getId());

        return CommentMapper.toDto(savedComment, toShortDto(authorDto));
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {

        log.info("Удаление комментария {} пользователем {}", commentId, userId);

        Comment comment = commentRepository.findByIdAndAuthorId(commentId, userId)
                .orElseThrow(() -> new NotFoundException("Комментарий не найден"));

        commentRepository.delete(comment);
    }

    @Override
    public List<CommentDto> getUserComments(Long userId, int from, int size) {

        log.info("Получение комментариев пользователя {}", userId);

        UserDto authorDto = getUserOrThrow(userId);
        UserShortDto shortAuthor = toShortDto(authorDto);

        Pageable pageable = PageRequest.of(from / size, size);

        return commentRepository.findByAuthorId(userId, pageable).stream()
                .map(comment -> CommentMapper.toDto(comment, shortAuthor))
                .collect(Collectors.toList());
    }

    @Override
    public List<CommentDto> getEventComments(Long eventId, int from, int size) {

        log.info("Получение комментариев события {}", eventId);

        Pageable pageable = PageRequest.of(from / size, size);
        List<Comment> comments = commentRepository.findByEventIdAndStatus(eventId, CommentStatus.PUBLISHED, pageable);

        Map<Long, UserShortDto> authorsMap = loadAuthors(comments);

        return comments.stream()
                .map(comment -> CommentMapper.toDto(comment,
                        authorsMap.get(comment.getAuthorId())))
                .collect(Collectors.toList());
    }

    @Override
    public List<CommentDto> getCommentsForModeration(int from, int size) {

        log.info("Получение комментариев на модерацию");

        Pageable pageable = PageRequest.of(from / size, size);
        List<Comment> comments = commentRepository.findByStatus(CommentStatus.PENDING, pageable);

        Map<Long, UserShortDto> authorsMap = loadAuthors(comments);

        return comments.stream()
                .map(comment -> CommentMapper.toDto(comment,
                        authorsMap.get(comment.getAuthorId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CommentDto moderateComment(Long commentId, Boolean approve) {

        log.info("Модерация комментария {}", commentId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий не найден"));

        if (comment.getStatus() != CommentStatus.PENDING) {
            throw new ConditionsNotMetException("Можно модерировать только комментарии на рассмотрении");
        }

        comment.setStatus(Objects.equals(approve, Boolean.TRUE) ? CommentStatus.PUBLISHED : CommentStatus.REJECTED);
        Comment moderatedComment = commentRepository.save(comment);

        UserDto authorDto = getUserOrThrow(comment.getAuthorId());
        return CommentMapper.toDto(moderatedComment, toShortDto(authorDto));
    }

    // Получение DTO пользователя
    private UserDto getUserOrThrow(Long userId) {

        List<UserDto> users = userClient.getUsers(List.of(userId), 0, 1);

        if (users == null || users.isEmpty()) {
            throw new NotFoundException("Пользователь не найден");
        }

        return users.getFirst();
    }

    // Получение DTO события
    private EventFullDto getEventOrThrow(Long eventId) {

        try {
            return eventClient.findById(eventId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("Событие не найдено");
        }
    }

    private UserShortDto toShortDto(UserDto userDto) {

        return UserShortDto.builder()
                .id(userDto.getId())
                .name(userDto.getName())
                .build();
    }

    private Map<Long, UserShortDto> loadAuthors(List<Comment> comments) {

        if (comments == null || comments.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> authorIds = comments.stream()
                .map(Comment::getAuthorId)
                .distinct()
                .collect(Collectors.toList());

        List<UserDto> users = userClient.getUsers(authorIds, 0, authorIds.size());

        Map<Long, UserShortDto> authorsMap = new HashMap<>();

        if (users != null) {
            for (UserDto user : users) {
                authorsMap.put(user.getId(), toShortDto(user));
            }
        }

        return authorsMap;
    }
}