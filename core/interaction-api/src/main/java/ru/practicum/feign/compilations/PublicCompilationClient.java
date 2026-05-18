package ru.practicum.feign.compilations;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.compilations.CompilationDto;

import java.util.List;

@FeignClient(name = "compilation-service", contextId = "PublicCompilationClient", path = "/compilations")
public interface PublicCompilationClient {

    // Получение подборок
    @GetMapping
    List<CompilationDto> getAllCompilations(
            @RequestParam(value = "pinned", required = false) Boolean pinned,
            @RequestParam(value = "from", defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(value = "size", defaultValue = "10") @Positive Integer size);

    // Получение подборки по ID
    @GetMapping("/{compId}")
    CompilationDto getCompilationById(@PathVariable("compId") @Positive Long compId);
}
