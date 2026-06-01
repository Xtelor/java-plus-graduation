package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.model.Similarity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SimilarityRepository extends JpaRepository<Similarity, Long> {

    Optional<Similarity> findByEvent1AndEvent2(Long event1, Long event2);

    @Query("""
        select s from Similarity s
        where s.event1 = :eventId or s.event2 = :eventId
        order by s.similarity desc
    """)
    List<Similarity> findAllForEvent(Long eventId);

    @Query("""
        select s from Similarity s
        where s.event1 in :eventIds or s.event2 in :eventIds
        order by s.similarity desc
    """)
    List<Similarity> findAllForEvents(Collection<Long> eventIds);
}
