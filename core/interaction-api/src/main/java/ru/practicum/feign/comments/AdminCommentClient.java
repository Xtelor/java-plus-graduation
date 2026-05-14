package ru.practicum.feign.comments;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.comments.CommentDto;

import java.util.List;

@FeignClient(name = "comment-service", contextId = "AdminCommentClient", path = "/admin/comments")
public interface AdminCommentClient {

    // Получение комментариев для модерации
    @GetMapping("/moderation")
    List<CommentDto> getCommentsForModeration(
            @RequestParam(value = "from", defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(value = "size", defaultValue = "10") @Positive int size);

    // Модерация комментария
    @PatchMapping("/{commentId}/moderate")
    CommentDto moderateComment(@PathVariable("commentId") @Positive Long commentId,
                               @RequestParam(value = "approve") Boolean approve);
}
