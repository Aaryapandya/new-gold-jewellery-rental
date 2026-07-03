package com.goldrental.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Payload for updating a jewellery listing.
 * All fields are optional — only non-null values are applied.
 */
@Data
public class UpdateJewelleryRequest {

    @Size(min = 3, max = 255)
    private String title;

    @Size(max = 2000)
    private String description;

    @DecimalMin(value = "0.001")
    @DecimalMax(value = "10000.0")
    private BigDecimal weightInGrams;

    @DecimalMin(value = "1.0")
    @DecimalMax(value = "999999.99")
    private BigDecimal rentPerDay;

    @Size(max = 100)
    private String category;

    private String addressLine;

    @Size(max = 100)
    private String city;

    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Must be a valid 6-digit Indian pincode")
    private String pincode;

    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private Double latitude;

    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private Double longitude;
}
