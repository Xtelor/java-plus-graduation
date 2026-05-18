package ru.practicum.dto.compilations;

import lombok.*;
import ru.practicum.dto.events.EventShortDto;


import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompilationDto {

    private Long id;
    private Set<EventShortDto> events;
    private Boolean pinned;
    private String title;
}