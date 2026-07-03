package com.goldrental.domain.entity;

import com.goldrental.domain.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Represents a rental booking for a jewellery item.
 *
 * <p>Strict invariants enforced at the service layer:
 * <ul>
 *   <li>Only one ACTIVE or APPROVED booking may exist per jewellery item at a time.</li>
 *   <li>New booking requests must not overlap with any existing APPROVED/ACTIVE booking.</li>
 *   <li>Total amount is calculated and locked at booking creation time.</li>
 * </ul>
 */
@Entity
@Table(name = "bookings")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jewellery_id", nullable = false)
    private Jewellery jewellery;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private User supplier;

    @Column(nullable = false)
    private Instant startDateTime;

    @Column(nullable = false)
    private Instant endDateTime;

    /** Populated when the buyer physically returns the item. */
    private Instant actualReturnDateTime;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private BookingStatus status = BookingStatus.REQUESTED;

    @Column(columnDefinition = "TEXT")
    private String buyerNotes;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(columnDefinition = "TEXT")
    private String cancellationReason;

    // ─── Audit ─────────────────────────────────────────────────
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;
}
