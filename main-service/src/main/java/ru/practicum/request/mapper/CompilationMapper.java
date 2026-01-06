package ru.practicum.request.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.event.EventMapper;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.request.dto.CompilationDto;
import ru.practicum.request.model.Compilation;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CompilationMapper {

    private final EventMapper eventMapper;

    public CompilationDto toDto(Compilation compilation) {
        List<EventShortDto> eventShortDtos = compilation.getEvents().stream()
                .map(eventMapper::toShortDto)
                .collect(Collectors.toList());

        return CompilationDto.builder()
                .id(compilation.getId())
                .pinned(compilation.getPinned())
                .title(compilation.getTitle())
                .events(eventShortDtos)
                .build();
    }
}
