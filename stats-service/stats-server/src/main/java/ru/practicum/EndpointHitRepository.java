package ru.practicum;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.EndpointHit;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EndpointHitRepository extends JpaRepository<EndpointHit, Long> {

    @Query("select eh " +
            "from EndpointHit eh " +
            "where eh.timestamp >= ?1 " +
            "and eh.timestamp <= ?2 " +
            "and eh.uri in ?3 " +
            "order by eh.timestamp desc")
    List<EndpointHit> findStatistics(Instant start, Instant end, String[] uris, boolean unique);

    EndpointHit save(EndpointHit endpointHit);

    boolean existsById(Long id);
}