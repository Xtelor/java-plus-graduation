package ru.practicum.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import ru.practicum.event.Location;

@Getter
@Setter
public class NewEventDto {

    @NotBlank(message = "Аннотация события не может быть пустой")
    @Size(min = 2, max = 2000, message = "Аннотация должна содержать от 2 до 2000 символов")
    private String annotation;

    private Long category;

    @NotBlank(message = "Описание события не может быть пустым")
    @Size(min = 20, max = 7000, message = "Описание события должно содержать от 20 до 7000 символов")
    private String description;

    private String eventDate;

    private Location location;

    private boolean paid = false;

    @PositiveOrZero(message = "Число участников должно быть неотрицательным")
    private Integer participationLimit = 0;

    private boolean requestModeration = true;

    @NotBlank(message = "Заголовок события не может быть пустым")
    @Size(min = 3, max = 120, message = "Заголовок события должен содержать от 3 до 120 символов")
    private String title;
}
