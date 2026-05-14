package ru.practicum.controller;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.compilations.CompilationDto;
import ru.practicum.feign.compilations.PublicCompilationClient;
import ru.practicum.service.CompilationService;

import java.util.List;

@RestController
@RequestMapping(path = "/compilations")
@RequiredArgsConstructor
@Slf4j
@Validated
public class PublicCompilationController implements PublicCompilationClient {

    private final CompilationService compilationService;

    @Override
    @GetMapping
    public List<CompilationDto> getAllCompilations(
            @RequestParam(required = false) Boolean pinned,
            @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size) {
        log.info("GET /compilations - получение подборок, pinned: {}, from: {}, size: {}",
                pinned, from, size);

        PageRequest pageRequest = PageRequest.of(from / size, size);
        return compilationService.getAllCompilations(pinned, pageRequest);
    }

    @Override
    @GetMapping("/{compId}")
    public CompilationDto getCompilationById(@PathVariable @Positive Long compId) {
        log.info("GET /compilations/{} - получение подборки", compId);
        return compilationService.getCompilationById(compId);
    }
}