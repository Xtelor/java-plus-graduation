package ru.practicum.feign.comments;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.comments.CommentDto;

import java.util.List;

@FeignClient(
        name = "comment-service",
        contextId = "PublicCommentClient",
        path = "/comments",
        fallback = PublicCommentClientFallback.class
)
public interface PublicCommentClient {

    // Получение комментариев
    @GetMapping("/events/{eventId}")
    List<CommentDto> getEventComments(@PathVariable("eventId") @Positive Long eventId,
                                      @RequestParam(value = "from", defaultValue = "0") @PositiveOrZero int from,
                                      @RequestParam(value = "size", defaultValue = "10") @Positive int size);
}
