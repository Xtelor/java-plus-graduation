package ru.practicum.event;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import ru.practicum.user.User;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "stats", schema = "public")
@Data
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @NotBlank(message = "Аннотация события не может быть пустой")
    @Column(nullable = false)
    private String annotation;

    @NotBlank(message = "Описание события не может быть пустым")
    @Column(nullable = false)
    private String description;

    @Column(name = "event_date")
    private LocalDateTime eventDate;

    @Column(name = "is_paid")
    private boolean paid;

    @Column(name = "request_moderation")
    private boolean requestModeration;

    @Column(name = "participation_limit")
    private Integer participationLimit;

    @NotBlank(message = "Заголовок события не может быть пустым")
    @Column(nullable = false)
    private String title;
}
