package com.goldrental.dto.response;

import com.goldrental.domain.enums.UserRole;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * Public-safe user profile response.
 * Password is never included (guideline: Jaas::SecurePasswordStorage).
 */
@Data
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private String name;
    private String mobileNumber;
    private UserRole role;
    private boolean emailVerified;
    private boolean mobileVerified;
    private boolean isActive;
    private String address;
    private String city;
    private String pincode;
    private Double latitude;
    private Double longitude;
    private String profilePhotoUrl;
    private String aadhaarFrontUrl;
    private String aadhaarBackUrl;
    private String panFrontUrl;
    private String panBackUrl;
    private List<FamilyMemberResponse> familyMembers;
    private Instant createdAt;
}
