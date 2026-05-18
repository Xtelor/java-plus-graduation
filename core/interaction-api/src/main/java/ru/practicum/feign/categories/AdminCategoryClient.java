package ru.practicum.feign.categories;

import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.categories.CategoryDto;
import ru.practicum.dto.categories.NewCategoryDto;

@FeignClient(name = "category-service", contextId = "AdminCategoryClient", path = "/admin/categories")
public interface AdminCategoryClient {

    // Создание категории
    @PostMapping
    CategoryDto createCategory(@Valid @RequestBody NewCategoryDto newCategoryDto);

    // Обновление категории
    @PatchMapping("/{catId}")
    CategoryDto updateCategory(@PathVariable("catId") Long catId,
                               @Valid @RequestBody CategoryDto categoryDto);

    // Удаление категории
    @DeleteMapping("/{catId}")
    void deleteCategory(@PathVariable("catId") Long catId);
}
