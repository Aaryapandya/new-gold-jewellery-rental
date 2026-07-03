package com.goldrental.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a requested resource does not exist in the database.
 * Maps to HTTP 404 Not Found.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(final String message) {
        super(message);
    }

    public ResourceNotFoundException(final String resourceName, final Long id) {
        super(String.format("%s with id [%d] not found", resourceName, id));
    }

    public ResourceNotFoundException(final String resourceName, final String field, final Object value) {
        super(String.format("%s with %s [%s] not found", resourceName, field, value));
    }
}
