package com.goldrental.domain.entity;

import com.goldrental.domain.enums.JewelleryStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a piece of jewellery listed for rent by a supplier.
 *
 * <p>Availability is tracked via the {@code isAvailable} flag, which is
 * automatically updated by the booking service when an ACTIVE booking exists.
 * The {@code status} field governs admin-controlled visibility.
 */
@Entity
@Table(name = "jewellery")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Jewellery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The supplier who listed this item. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private User supplier;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal weightInGrams;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal rentPerDay;

    @Column(nullable = false, length = 100)
    private String category;

    // ─── Location ──────────────────────────────────────────────
    @Column(columnDefinition = "TEXT")
    private String addressLine;

    @Column(length = 100)
    private String city;

    @Column(length = 10)
    private String pincode;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    // ─── Availability ──────────────────────────────────────────
    /**
     * Reflects whether the item is currently free to be booked.
     * Updated atomically by the BookingService on status transitions.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean isAvailable = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private JewelleryStatus status = JewelleryStatus.UNDER_VERIFICATION;

    // ─── Media ─────────────────────────────────────────────────
    @OneToMany(mappedBy = "jewellery", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<JewelleryImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "jewellery", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<JewelleryBill> bills = new ArrayList<>();

    // ─── Audit ─────────────────────────────────────────────────
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;
}
