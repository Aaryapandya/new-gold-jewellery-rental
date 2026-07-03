package com.goldrental.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Query parameters for location-based jewellery discovery.
 */
@Data
public class NearbySearchRequest {

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0",  message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0",   message = "Latitude must be between -90 and 90")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0",  message = "Longitude must be between -180 and 180")
    private Double longitude;

    @DecimalMin(value = "0.1", message = "Radius must be at least 0.1 km")
    @DecimalMax(value = "500.0", message = "Radius cannot exceed 500 km")
    private Double radiusKm;

    private String category;
}
