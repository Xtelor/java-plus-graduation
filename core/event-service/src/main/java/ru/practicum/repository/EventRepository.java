package ru.practicum.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import ru.practicum.entity.Event;

public interface EventRepository extends
        JpaRepository<Event, Long>,
        QuerydslPredicateExecutor<Event>,
        EventRepositoryCustom {

    Page<Event> findByInitiatorId(Long initiatorId, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.id = ?1 AND e.state = 'PUBLISHED'")
    Event findByIdPublished(Long eventId);

    boolean existsByCategoryId(Long categoryId);
}
