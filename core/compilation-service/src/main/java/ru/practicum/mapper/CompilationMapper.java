package ru.practicum.mapper;

import ru.practicum.dto.compilations.CompilationDto;
import ru.practicum.dto.compilations.NewCompilationDto;
import ru.practicum.dto.events.EventShortDto;
import ru.practicum.entity.Compilation;

import java.util.HashSet;
import java.util.Set;

public class CompilationMapper {

    public static CompilationDto toDto(Compilation compilation, Set<EventShortDto> events) {
        return CompilationDto.builder()
                .id(compilation.getId())
                .title(compilation.getTitle())
                .pinned(compilation.getPinned())
                .events(events)
                .build();
    }

    public static Compilation toEntity(NewCompilationDto newCompilationDto) {
        return Compilation.builder()
                .title(newCompilationDto.getTitle())
                .pinned(newCompilationDto.getPinned() != null ?
                        newCompilationDto.getPinned() : false)
                .events(new HashSet<>())
                .build();
    }
}