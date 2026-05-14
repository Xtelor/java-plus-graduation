package ru.practicum.feign.compilations;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.compilations.CompilationDto;
import ru.practicum.dto.compilations.NewCompilationDto;
import ru.practicum.dto.compilations.UpdateCompilationRequest;

@FeignClient(name = "compilation-service", contextId = "AdminCompilationClient", path = "/admin/compilations")
public interface AdminCompilationClient {

    // Создание подборки
    @PostMapping
    CompilationDto createCompilation(@Valid @RequestBody NewCompilationDto newCompilationDto);

    // Обновление подборки
    @PatchMapping("/{compId}")
    CompilationDto updateCompilation(@PathVariable("compId") Long compId,
                                     @Valid @RequestBody UpdateCompilationRequest request);

    // Удаление подборки
    @DeleteMapping("/{compId}")
    void deleteCompilation(@PathVariable("compId") @Positive Long compId);
}
