package ru.practicum.request.dto;

import lombok.Builder;
import lombok.Data;
import ru.practicum.event.dto.EventShortDto;

import java.util.List;

@Data
@Builder
public class CompilationDto {
    // ID подборки событий
    private Long id;

    // Подборка событий
    private List<EventShortDto> events;

    // Закрепление подборки на главной странице: true - если, подборка закреплена
    private Boolean pinned;

    // Заголовок подборки
    private String title;
}
