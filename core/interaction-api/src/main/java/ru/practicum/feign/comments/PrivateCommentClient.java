package ru.practicum.feign.comments;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.comments.CommentDto;
import ru.practicum.dto.comments.NewCommentDto;

import java.util.List;

@FeignClient(name = "comment-service", contextId = "PrivateCommentClient")
public interface PrivateCommentClient {

    // Создание комментария
    @PostMapping("/users/{userId}/comments")
    CommentDto createComment(@PathVariable("userId") @Positive Long userId,
                             @RequestParam(value = "eventId") @Positive Long eventId,
                             @Valid @RequestBody NewCommentDto newCommentDto);

    // Удаление комментария
    @DeleteMapping("/users/{userId}/comments/{commentId}")
    void deleteComment(@PathVariable("userId") @Positive Long userId,
                       @PathVariable("commentId") @Positive Long commentId);

    // Получение списка комментариев
    @GetMapping("/users/{userId}/comments")
    List<CommentDto> getUserComments(@PathVariable("userId") @Positive Long userId,
                                     @RequestParam(value = "from", defaultValue = "0") @PositiveOrZero int from,
                                     @RequestParam(value = "size", defaultValue = "10") @Positive int size);

}
