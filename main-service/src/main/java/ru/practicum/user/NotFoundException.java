package ru.practicum.user;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}