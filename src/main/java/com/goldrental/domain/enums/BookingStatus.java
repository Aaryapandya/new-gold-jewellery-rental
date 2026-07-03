package com.goldrental.domain.enums;

/**
 * State machine for the booking lifecycle.
 *
 * <pre>
 *   REQUESTED → APPROVED → ACTIVE → COMPLETED
 *   REQUESTED → REJECTED
 *   REQUESTED | APPROVED → CANCELLED
 * </pre>
 */
public enum BookingStatus {
    /** Buyer has submitted a booking request; awaiting supplier decision. */
    REQUESTED,

    /** Supplier has approved the booking; jewellery is reserved. */
    APPROVED,

    /** Supplier rejected the booking request. */
    REJECTED,

    /** Rental period has started; jewellery is with the buyer. */
    ACTIVE,

    /** Jewellery has been returned; rental is complete. */
    COMPLETED,

    /** Booking was cancelled (by buyer before approval, or by admin). */
    CANCELLED
}
