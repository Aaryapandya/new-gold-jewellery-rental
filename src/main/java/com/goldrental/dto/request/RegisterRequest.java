package com.goldrental.dto.request;

import com.goldrental.domain.enums.UserRole;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Payload for user registration.
 * Role is restricted to BUYER or SUPPLIER at self-registration time.
 * ADMIN accounts are created out-of-band.
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "Password must contain at least one uppercase, one lowercase, one digit, and one special character"
    )
    private String password;

    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Must be a valid 10-digit Indian mobile number")
    private String mobileNumber;

    @NotNull(message = "Role is required")
    private UserRole role;
}
