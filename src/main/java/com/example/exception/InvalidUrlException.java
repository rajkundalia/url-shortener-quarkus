package com.example.exception;

/**
 * Exception thrown when an invalid URL is provided for shortening.
 * This exception is mapped to HTTP 400 Bad Request responses.
 */
public class InvalidUrlException extends RuntimeException {

    private final String invalidUrl;

    public InvalidUrlException(String invalidUrl) {
        super("Invalid URL format provided: " + invalidUrl);
        this.invalidUrl = invalidUrl;
    }

    public InvalidUrlException(String invalidUrl, String message) {
        super(message);
        this.invalidUrl = invalidUrl;
    }

    public InvalidUrlException(String invalidUrl, String message, Throwable cause) {
        super(message, cause);
        this.invalidUrl = invalidUrl;
    }

    public String getInvalidUrl() {
        return invalidUrl;
    }
}