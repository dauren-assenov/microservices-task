package com.epam.song_service.exception;

public class InvalidCsvException extends RuntimeException {
    public InvalidCsvException(String message) { super(message); }
}
