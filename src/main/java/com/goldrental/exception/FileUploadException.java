package com.goldrental.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a file upload operation fails (e.g., Cloudinary I/O error).
 * Maps to HTTP 500 Internal Server Error.
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class FileUploadException extends RuntimeException {

    public FileUploadException(final String message) {
        super(message);
    }

    public FileUploadException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
