package ru.practicum.feign.categories;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.categories.CategoryDto;

import java.util.List;

@FeignClient(name = "category-service", contextId = "PublicCategoryClient", path = "/categories")
public interface PublicCategoryClient {

    // Получение категорий
    @GetMapping
    List<CategoryDto> getAllCategories(@RequestParam(value = "from", defaultValue = "0") @PositiveOrZero Integer from,
                                       @RequestParam(value = "size", defaultValue = "10") @Positive Integer size);

    // Получение категории по ID
    @GetMapping("/{catId}")
    CategoryDto getCategoryById(@PathVariable("catId") Long catId);
}
