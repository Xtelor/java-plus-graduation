package ru.practicum.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.model.Interaction;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InteractionRepository extends JpaRepository<Interaction, Long> {

    Optional<Interaction> findByUserIdAndEventId(Long userId, Long eventId);

    List<Interaction> findByUserId(Long userId);

    List<Interaction> findByUserIdOrderByTsDesc(Long userId, Pageable pageable);

    List<Interaction> findByEventIdIn(Collection<Long> eventIds);
}
