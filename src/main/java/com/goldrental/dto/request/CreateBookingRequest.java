package com.goldrental.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;

/**
 * Payload for creating a booking request (sent by BUYER).
 *
 * <p>Both dates must be in the future. Business-level validation
 * (end > start, no overlap, max advance days) is enforced in the service layer.
 */
@Data
public class CreateBookingRequest {

    @NotNull(message = "Jewellery ID is required")
    private Long jewelleryId;

    @NotNull(message = "Start date/time is required")
    @Future(message = "Start date must be in the future")
    private Instant startDateTime;

    @NotNull(message = "End date/time is required")
    @Future(message = "End date must be in the future")
    private Instant endDateTime;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String buyerNotes;
}
