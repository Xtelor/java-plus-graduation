package ru.practicum;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.List;

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