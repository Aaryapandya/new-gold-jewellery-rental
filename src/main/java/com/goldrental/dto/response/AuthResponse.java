package com.goldrental.dto.response;

import com.goldrental.domain.enums.UserRole;
import lombok.Builder;
import lombok.Data;

/**
 * Returned after a successful login or registration.
 */
@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private Long userId;
    private String email;
    private String name;
    private UserRole role;
}
