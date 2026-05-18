package ru.practicum.mapper;

import ru.practicum.dto.categories.CategoryDto;
import ru.practicum.dto.categories.NewCategoryDto;
import ru.practicum.entity.Category;

public class CategoryMapper {

    public static CategoryDto toDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }

    public static Category toEntity(NewCategoryDto newCategoryDto) {
        return Category.builder()
                .name(newCategoryDto.getName())
                .build();
    }
}