package ru.practicum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EndpointHitDto {

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
    private String timestamp;
}