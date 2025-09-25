package com.example.exception;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JAX-RS Exception Mapper that converts custom exceptions to proper JSON error responses.
 * This ensures consistent error response formats across all endpoints using DTOs.
 */
@Provider
public class UrlExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(UrlExceptionMapper.class);

    @Override
    public Response toResponse(Exception exception) {
        LOG.debugf("Mapping exception: %s", exception.getClass().getSimpleName());

        if (exception instanceof UrlNotFoundException) {
            return handleUrlNotFoundException((UrlNotFoundException) exception);
        } else if (exception instanceof InvalidUrlException) {
            return handleInvalidUrlException((InvalidUrlException) exception);
        } else if (exception instanceof ConstraintViolationException) {
            return handleConstraintViolationException((ConstraintViolationException) exception);
        } else if (exception instanceof NotAllowedException) {
            return handleNotAllowedException((NotAllowedException) exception);
        } else if (exception instanceof NotSupportedException) {
            return handleNotSupportedException((NotSupportedException) exception);
        } else {
            return handleGenericException(exception);
        }
    }

    private Response handleNotSupportedException(NotSupportedException exception) {
        LOG.debugf("UNSUPPORTED_MEDIA_TYPE: %s", exception.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                "UNSUPPORTED_MEDIA_TYPE",
                exception.getMessage(),
                LocalDateTime.now()
        );

        return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                .entity(errorResponse)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private Response handleNotAllowedException(NotAllowedException exception) {
        LOG.debugf("Method not allowed: %s", exception.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                "METHOD_NOT_ALLOWED",
                exception.getMessage(),
                LocalDateTime.now()
        );

        return Response.status(Response.Status.METHOD_NOT_ALLOWED)
                .entity(errorResponse)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    /**
     * Handles UrlNotFoundException and maps it to HTTP 404.
     */
    private Response handleUrlNotFoundException(UrlNotFoundException exception) {
        LOG.debugf("URL not found: %s", exception.getShortCode());

        ErrorResponse errorResponse = new ErrorResponse(
                "URL_NOT_FOUND",
                exception.getMessage(),
                LocalDateTime.now()
        );

        return Response.status(Response.Status.NOT_FOUND)
                .entity(errorResponse)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    /**
     * Handles InvalidUrlException and maps it to HTTP 400.
     */
    private Response handleInvalidUrlException(InvalidUrlException exception) {
        LOG.debugf("Invalid URL provided: %s", exception.getInvalidUrl());

        ErrorResponse errorResponse = new ErrorResponse(
                "INVALID_URL",
                exception.getMessage(),
                LocalDateTime.now()
        );

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorResponse)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    /**
     * Handles Bean Validation errors and maps them to HTTP 422.
     */
    private Response handleConstraintViolationException(ConstraintViolationException exception) {
        Set<ConstraintViolation<?>> violations = exception.getConstraintViolations();

        String message = violations.stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining(", "));

        LOG.debugf("Validation error: %s", message);

        ValidationErrorResponse errorResponse = new ValidationErrorResponse(
                "VALIDATION_ERROR",
                message,
                LocalDateTime.now(),
                violations.stream()
                        .map(violation -> new ValidationError(
                                violation.getPropertyPath().toString(),
                                violation.getMessage(),
                                violation.getInvalidValue() != null ? violation.getInvalidValue().toString() : null
                        ))
                        .collect(Collectors.toList())
        );

        return Response.status(422) // HTTP 422 Unprocessable Entity
                .entity(errorResponse)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    /**
     * Handles all other exceptions and maps them to HTTP 500.
     */
    private Response handleGenericException(Exception exception) {
        LOG.errorf(exception, "Unexpected error occurred: %s", exception.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred. Please try again later.",
                LocalDateTime.now()
        );

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorResponse)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    // =============================================================================
    // Error Response DTOs
    // =============================================================================

    /**
     * Standard error response DTO for consistent error formatting.
     */
    public static class ErrorResponse {
        @JsonbProperty("error")
        private String error;

        @JsonbProperty("message")
        private String message;

        @JsonbProperty("timestamp")
        private String timestamp;

        public ErrorResponse() {
        }

        public ErrorResponse(String error, String message, LocalDateTime timestamp) {
            this.error = error;
            this.message = message;
            this.timestamp = timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }
    }

    /**
     * Enhanced error response DTO for validation errors with detailed field information.
     */
    public static class ValidationErrorResponse extends ErrorResponse {
        @JsonbProperty("violations")
        private java.util.List<ValidationError> violations;

        public ValidationErrorResponse() {
        }

        public ValidationErrorResponse(String error, String message, LocalDateTime timestamp,
                                       java.util.List<ValidationError> violations) {
            super(error, message, timestamp);
            this.violations = violations;
        }

        public java.util.List<ValidationError> getViolations() {
            return violations;
        }

        public void setViolations(java.util.List<ValidationError> violations) {
            this.violations = violations;
        }
    }

    /**
     * Individual validation error details.
     */
    public static class ValidationError {
        @JsonbProperty("field")
        private String field;

        @JsonbProperty("message")
        private String message;

        @JsonbProperty("rejectedValue")
        private String rejectedValue;

        public ValidationError() {
        }

        public ValidationError(String field, String message, String rejectedValue) {
            this.field = field;
            this.message = message;
            this.rejectedValue = rejectedValue;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getRejectedValue() {
            return rejectedValue;
        }

        public void setRejectedValue(String rejectedValue) {
            this.rejectedValue = rejectedValue;
        }
    }
}