package ru.practicum.controller;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.comments.CommentDto;
import ru.practicum.feign.comments.PublicCommentClient;
import ru.practicum.service.CommentService;

import java.util.List;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
@Slf4j
@Validated
public class PublicCommentController implements PublicCommentClient {

    private final CommentService commentService;

    @Override
    @GetMapping("/events/{eventId}")
    public List<CommentDto> getEventComments(@PathVariable @Positive Long eventId,
                                             @RequestParam(defaultValue = "0") @PositiveOrZero int from,
                                             @RequestParam(defaultValue = "10") @Positive int size) {
        log.info("GET /comments/events/{}?from={}&size={}", eventId, from, size);
        return commentService.getEventComments(eventId, from, size);
    }
}