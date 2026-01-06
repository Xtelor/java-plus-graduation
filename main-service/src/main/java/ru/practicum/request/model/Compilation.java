package ru.practicum.request.model;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.event.Event;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "compilations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Compilation {
    // ID подборки событий
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Закрепление подборки на главной странице: true - если, подборка закреплена
    @Column(nullable = false)
    private Boolean pinned;

    // Заголовок подборки
    @Column(nullable = false, length = 50, unique = true)
    private String title;

    // События
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "compilation_events",
            joinColumns = @JoinColumn(name = "compilation_id"),
            inverseJoinColumns = @JoinColumn(name = "event_id")
    )
    @ToString.Exclude
    private Set<Event> events = new HashSet<>();
}
