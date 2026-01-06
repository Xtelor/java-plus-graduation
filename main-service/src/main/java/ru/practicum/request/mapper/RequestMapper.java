package ru.practicum.request.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.model.Request;

@Component
public class RequestMapper {
    public ParticipationRequestDto toDto(Request request) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .event(request.getEvent().getId())
                .requester(request.getRequester().getId())
                .created(request.getCreated().toString())
                .status(request.getStatus().toString())
                .build();
    }
}
