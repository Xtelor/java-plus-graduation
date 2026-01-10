package ru.practicum.event.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import ru.practicum.event.Event;

public interface EventRepository extends
        JpaRepository<Event, Long>,
        QuerydslPredicateExecutor<Event>,
        EventRepositoryCustom {

    Page<Event> findByInitiatorId(Long initiatorId, Pageable pageable);

    @Query("select e " +
            "from Event e " +
            "where e.id =  ?1 " +
            "and e.state = 'PUBLISHED'")
    Event findByIdPublished(Long eventId);
}
