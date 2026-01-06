package ru.practicum.request.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ParticipationRequestDto {
    // ID запроса
    private Long id;

    // ID события
    private Long event;

    // ID пользователя
    private Long requester;

    // дата и время создания запроса
    private String created;

    // статус запроса
    private String status;
}
