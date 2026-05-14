package ru.practicum.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.entity.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name);

    List<Category> findAllBy(Pageable pageable);

    @Query(value = "SELECT COUNT(*) FROM events e WHERE e.category_id = :categoryId", nativeQuery = true)
    Long countEventsByCategoryId(@Param("categoryId") Long categoryId);

    Boolean existsByName(String name);
}