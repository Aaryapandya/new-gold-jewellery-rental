package com.goldrental.dto.response;

import com.goldrental.domain.enums.BookingStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Booking detail response DTO.
 */
@Data
@Builder
public class BookingResponse {
    private Long id;
    private Long jewelleryId;
    private String jewelleryTitle;
    private Long buyerId;
    private String buyerName;
    private Long supplierId;
    private String supplierName;
    private Instant startDateTime;
    private Instant endDateTime;
    private Instant actualReturnDateTime;
    private BigDecimal totalAmount;
    private BookingStatus status;
    private String buyerNotes;
    private String rejectionReason;
    private String cancellationReason;
    private Instant createdAt;
    private Instant updatedAt;
}
