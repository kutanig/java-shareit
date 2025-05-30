package ru.practicum.shareit.exception;

public class SelfBookingException extends RuntimeException {
    public SelfBookingException(String message) {
        super(message);
    }
}
