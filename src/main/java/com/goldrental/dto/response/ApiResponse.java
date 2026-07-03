package com.goldrental.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Uniform API envelope used for all non-paginated responses.
 *
 * <pre>
 * {
 *   "success": true,
 *   "message": "User registered successfully",
 *   "data": { ... },
 *   "timestamp": "2024-01-01T00:00:00Z"
 * }
 * </pre>
 *
 * @param <T> type of the payload data
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    @Builder.Default
    private Instant timestamp = Instant.now();

    // ─── Factory methods ───────────────────────────────────────

    public static <T> ApiResponse<T> success(final String message, final T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(final T data) {
        return success("Success", data);
    }

    public static <T> ApiResponse<T> error(final String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
}
