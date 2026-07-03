package com.goldrental.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Payload for supplier approve/reject action on a booking.
 */
@Data
public class BookingActionRequest {

    /** Optional reason populated when rejecting a booking. */
    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;
}
