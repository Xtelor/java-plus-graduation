package ru.practicum.exceptions;

public class ConditionsNotMetException extends RuntimeException {

    public ConditionsNotMetException(String message) {
        super(message);
    }
}
