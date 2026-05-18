package ru.practicum.feign.comments;

import org.springframework.stereotype.Component;
import ru.practicum.dto.comments.CommentDto;

import java.util.Collections;
import java.util.List;

@Component
public class PublicCommentClientFallback implements PublicCommentClient {

    @Override
    public List<CommentDto> getEventComments(Long eventId, int from, int size) {
        return Collections.emptyList();
    }
}
