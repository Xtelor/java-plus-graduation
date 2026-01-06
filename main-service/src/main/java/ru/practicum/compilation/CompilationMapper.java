package ru.practicum.compilation;

import org.springframework.stereotype.Component;

@Component
public class CompilationMapper {

    public CompilationDto toDto(Compilation compilation) {
        return CompilationDto.builder()
                .id(compilation.getId())
                .title(compilation.getTitle())
                .pinned(compilation.getPinned())
                .events(null)
                .build();
    }

    public Compilation toEntity(NewCompilationDto newCompilationDto) {
        return Compilation.builder()
                .title(newCompilationDto.getTitle())
                .pinned(newCompilationDto.getPinned() != null ?
                        newCompilationDto.getPinned() : false)
                .build();
    }
}