package ru.practicum;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name = "stats", schema = "public")
@Data
public class EndpointHit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Название приложения не может быть пустым")
    private String app;

    @NotBlank(message = "URI не может быть пустым")
    private String uri;

    @NotBlank(message = "IP-адрес не может быть пустым")
    @Pattern(
            regexp = "^([0-9]{1,3}\\.){3}[0-9]{1,3}$",
            message = "Неверный формат IP-адреса"
    )
    private String ip;

    @NotNull(message = "Время запроса не может быть пустым")
    private Instant timestamp;
}
