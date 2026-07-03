package com.goldrental.dto.response;

import com.goldrental.domain.enums.JewelleryStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Jewellery listing response DTO.
 * Includes computed distance when returned from a radius search.
 */
@Data
@Builder
public class JewelleryResponse {
    private Long id;
    private Long supplierId;
    private String supplierName;
    private String title;
    private String description;
    private BigDecimal weightInGrams;
    private BigDecimal rentPerDay;
    private String category;
    private String addressLine;
    private String city;
    private String pincode;
    private Double latitude;
    private Double longitude;
    private boolean isAvailable;
    private JewelleryStatus status;
    private List<String> imageUrls;
    private List<String> billUrls;
    private Instant createdAt;

    /** Populated only in nearby-search results (km). */
    private Double distanceKm;
}
