package ru.practicum.event.dto;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import ru.practicum.user.User;

import java.time.LocalDateTime;

public class NewEventRequest {

    @NotBlank(message = "Аннотация события не может быть пустой")
    private String annotation;

    @NotBlank(message = "Описание события не может быть пустым")
    private String description;

    private LocalDateTime eventDate;

    private boolean paid;

    private boolean requestModeration;

    private Integer participationLimit;

    @NotBlank(message = "Заголовок события не может быть пустым")
    private String title;
}
