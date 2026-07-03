package com.goldrental.domain.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Stores a purchase bill / invoice URL for a jewellery listing.
 * Bills serve as proof of ownership for supplier verification.
 */
@Entity
@Table(name = "jewellery_bills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JewelleryBill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jewellery_id", nullable = false)
    private Jewellery jewellery;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String billUrl;
}
