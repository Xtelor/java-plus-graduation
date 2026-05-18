package ru.practicum.controller;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.categories.CategoryDto;
import ru.practicum.feign.categories.PublicCategoryClient;
import ru.practicum.service.CategoryService;

import java.util.List;

@RestController
@RequestMapping(path = "/categories")
@RequiredArgsConstructor
@Slf4j
@Validated
public class PublicCategoryController implements PublicCategoryClient {

    private final CategoryService categoryService;

    @Override
    @GetMapping
    public List<CategoryDto> getAllCategories(
            @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size) {
        log.info("GET /categories - получение категорий, from: {}, size: {}", from, size);

        PageRequest pageRequest = PageRequest.of(from / size, size);
        return categoryService.getAllCategories(pageRequest);
    }

    @Override
    @GetMapping("/{catId}")
    public CategoryDto getCategoryById(@PathVariable Long catId) {
        log.info("GET /categories/{} - получение категории по ID", catId);
        return categoryService.getCategoryById(catId);
    }
}