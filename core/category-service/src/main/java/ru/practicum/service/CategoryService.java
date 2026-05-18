package ru.practicum.service;

import org.springframework.data.domain.Pageable;
import ru.practicum.dto.categories.CategoryDto;
import ru.practicum.dto.categories.NewCategoryDto;

import java.util.List;

public interface CategoryService {

    CategoryDto createCategory(NewCategoryDto newCategoryDto);

    CategoryDto updateCategory(Long categoryId, CategoryDto categoryDto);

    void deleteCategory(Long categoryId);

    List<CategoryDto> getAllCategories(Pageable pageable);

    CategoryDto getCategoryById(Long categoryId);
}