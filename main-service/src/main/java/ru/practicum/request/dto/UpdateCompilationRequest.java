package ru.practicum.request.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdateCompilationRequest {
    // Список ID событий
    private List<Long> events;

    // Закрепление подборки на главной странице: true - если, подборка закреплена
    private Boolean pinned;

    // Заголовок подборки
    @Size(min = 1, max = 50)
    private String title;
}
