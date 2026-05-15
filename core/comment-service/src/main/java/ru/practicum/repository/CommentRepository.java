package ru.practicum.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.entity.Comment;
import ru.practicum.enums.CommentStatus;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // Получить опубликованные комментарии события
    List<Comment> findByEventIdAndStatus(Long eventId, CommentStatus status, Pageable pageable);

    // Получить комментарии пользователя
    List<Comment> findByAuthorId(Long authorId, Pageable pageable);

    // Получить комментарии на модерации
    List<Comment> findByStatus(CommentStatus status, Pageable pageable);

    // Получить комментарий по ID и автору
    Optional<Comment> findByIdAndAuthorId(Long id, Long authorId);
}