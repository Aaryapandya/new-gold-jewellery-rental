package com.goldrental.dto.response;

import com.goldrental.domain.enums.BookingStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Lightweight booking entry used in the jewellery calendar view.
 * Allows buyers to see blocked dates without exposing sensitive booking details.
 */
@Data
@Builder
public class BookingCalendarEntry {
    private Long bookingId;
    private Instant startDateTime;
    private Instant endDateTime;
    private BookingStatus status;
}
