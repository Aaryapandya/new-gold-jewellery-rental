package com.goldrental.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Payload for creating a new jewellery listing.
 */
@Data
public class CreateJewelleryRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 255, message = "Title must be between 3 and 255 characters")
    private String title;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    @NotNull(message = "Weight is required")
    @DecimalMin(value = "0.001", message = "Weight must be greater than 0")
    @DecimalMax(value = "10000.0", message = "Weight cannot exceed 10 kg")
    private BigDecimal weightInGrams;

    @NotNull(message = "Rent per day is required")
    @DecimalMin(value = "1.0", message = "Rent per day must be at least ₹1")
    @DecimalMax(value = "999999.99", message = "Rent per day is too high")
    private BigDecimal rentPerDay;

    @NotBlank(message = "Category is required")
    @Size(max = 100)
    private String category;

    private String addressLine;

    @Size(max = 100)
    private String city;

    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Must be a valid 6-digit Indian pincode")
    private String pincode;

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private Double longitude;
}
