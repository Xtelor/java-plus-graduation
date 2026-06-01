package ru.practicum.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "similarities")
@Getter
@Setter
public class Similarity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event1", nullable = false)
    private Long event1;

    @Column(name = "event2", nullable = false)
    private Long event2;

    @Column(name = "similarity", nullable = false)
    private Double similarity;

    @Column(name = "ts", nullable = false)
    private Instant ts;
}
