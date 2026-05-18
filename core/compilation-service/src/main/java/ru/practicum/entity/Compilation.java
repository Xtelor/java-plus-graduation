package ru.practicum.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "compilations", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Compilation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 50, unique = true)
    private String title;

    @Column(name = "pinned", nullable = false)
    private Boolean pinned;

    @ElementCollection
    @CollectionTable(
            name = "compilations_events",
            joinColumns = @JoinColumn(name = "compilation_id")
    )
    @Column(name = "event_id")
    private Set<Long> events;
}