package com.goldrental.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Payload for updating a user's profile details.
 * All fields are optional — only non-null values are applied.
 */
@Data
public class UpdateProfileRequest {

    @Size(min = 2, max = 255)
    private String name;

    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Must be a valid 10-digit Indian mobile number")
    private String mobileNumber;

    private String address;

    @Size(max = 100)
    private String city;

    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Must be a valid 6-digit Indian pincode")
    private String pincode;

    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0",  message = "Latitude must be between -90 and 90")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0",  message = "Longitude must be between -180 and 180")
    private Double longitude;
}
