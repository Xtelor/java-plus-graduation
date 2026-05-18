package ru.practicum.exceptions;

public record ErrorResponse(
        String status,
        String reason,
        String message,
        String timestamp
) {}
