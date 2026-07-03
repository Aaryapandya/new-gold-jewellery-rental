package com.goldrental.domain.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Stores a single image URL for a jewellery listing.
 * Display order allows clients to show images in a defined sequence.
 */
@Entity
@Table(name = "jewellery_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JewelleryImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jewellery_id", nullable = false)
    private Jewellery jewellery;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String imageUrl;

    @Column(nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;
}
