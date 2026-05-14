package ru.practicum.mapper;

import ru.practicum.dto.admin.UserShortDto;
import ru.practicum.dto.comments.CommentDto;
import ru.practicum.dto.comments.NewCommentDto;
import ru.practicum.entity.Comment;
import ru.practicum.enums.CommentStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CommentMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static CommentDto toDto(Comment comment, UserShortDto dto) {
        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .author(dto)
                .eventId(comment.getEventId())
                .created(comment.getCreated().format(FORMATTER))
                .status(comment.getStatus().toString())
                .build();
    }

    public static Comment toEntity(NewCommentDto newCommentDto) {
        return Comment.builder()
                .text(newCommentDto.getText())
                .created(LocalDateTime.now())
                .status(CommentStatus.PENDING)
                .build();
    }
}