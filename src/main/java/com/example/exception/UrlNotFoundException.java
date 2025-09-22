package com.example.exception;

/**
 * Exception thrown when a requested short code is not found in the system.
 * This exception is mapped to HTTP 404 Not Found responses.
 */
public class UrlNotFoundException extends RuntimeException {

    private final String shortCode;

    public UrlNotFoundException(String shortCode) {
        super("Short code '" + shortCode + "' not found");
        this.shortCode = shortCode;
    }

    public UrlNotFoundException(String shortCode, String message) {
        super(message);
        this.shortCode = shortCode;
    }

    public UrlNotFoundException(String shortCode, String message, Throwable cause) {
        super(message, cause);
        this.shortCode = shortCode;
    }
    
    public String getShortCode() {
        return shortCode;
    }
}