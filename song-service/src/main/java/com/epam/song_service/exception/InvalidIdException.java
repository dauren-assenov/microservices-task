package com.epam.song_service.exception;

public class InvalidIdException extends RuntimeException {
    public InvalidIdException(String message) { super(message); }
}
