package org.example.response;

// Custom exceptions for better error handling
public class RecraftApiException extends RuntimeException {
    public RecraftApiException(String message) {
        super(message);
    }
}
