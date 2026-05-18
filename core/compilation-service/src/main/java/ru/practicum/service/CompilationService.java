package ru.practicum.service;

import org.springframework.data.domain.Pageable;
import ru.practicum.dto.compilations.CompilationDto;
import ru.practicum.dto.compilations.NewCompilationDto;
import ru.practicum.dto.compilations.UpdateCompilationRequest;

import java.util.List;

public interface CompilationService {

    CompilationDto createCompilation(NewCompilationDto newCompilationDto);

    CompilationDto updateCompilation(Long compilationId, UpdateCompilationRequest request);

    void deleteCompilation(Long compilationId);

    List<CompilationDto> getAllCompilations(Boolean pinned, Pageable pageable);

    CompilationDto getCompilationById(Long compilationId);
}