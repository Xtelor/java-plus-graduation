package ru.practicum.request.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;
import ru.practicum.event.Location;

@Data
public class UpdateEventUserRequest {
    // Новая аннотация
    @Size(min = 20, max = 2000)
    private String annotation;

    // Новая категория
    private Long category;

    // Новое описание
    @Size(min = 20, max = 7000)
    private String description;

    // Новые дата и время
    private String eventDate;

    // Локация проведения события
    private Location location;

    // Новое значение флага о платности мероприятия
    private Boolean paid;

    // Новый лимит пользователей
    private Integer participantLimit;

    // Пре-модерация заявок на участие: true - требуется модерация
    private Boolean requestModeration;

    // Изменение состояния события
    private String stateAction;

    // Новый заголовок
    @Size(min = 3, max = 120)
    private String title;
}
