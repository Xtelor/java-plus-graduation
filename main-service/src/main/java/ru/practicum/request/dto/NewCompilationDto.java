package ru.practicum.request.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class NewCompilationDto {
    // Список id событий
    private List<Long> events;

    // Закрепление подборки на главной странице: true - если, подборка закреплена
    @Builder.Default
    private Boolean pinned = false;

    // Заголовок подборки
    @NotBlank(message = "Заголовок подборки не может быть пустым.")
    @Size(min = 1, max = 50)
    private String title;
}
