package com.goldrental.domain.entity;

import com.goldrental.domain.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a platform user who can be a BUYER, SUPPLIER, or ADMIN.
 *
 * <p>Security note: the {@code password} field stores only the BCrypt hash.
 * Plain-text passwords are never persisted (guideline: Jaas::SecurePasswordStorage).
 */
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /** BCrypt-hashed password – never expose this in DTOs. */
    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 20)
    private String mobileNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    // ─── Verification flags ────────────────────────────────────
    @Column(nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean mobileVerified = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean isActive = true;

    // ─── Address ───────────────────────────────────────────────
    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 10)
    private String pincode;

    private Double latitude;
    private Double longitude;

    // ─── Document URLs ─────────────────────────────────────────
    @Column(columnDefinition = "TEXT")
    private String profilePhotoUrl;

    @Column(columnDefinition = "TEXT")
    private String aadhaarFrontUrl;

    @Column(columnDefinition = "TEXT")
    private String aadhaarBackUrl;

    @Column(columnDefinition = "TEXT")
    private String panFrontUrl;

    @Column(columnDefinition = "TEXT")
    private String panBackUrl;

    // ─── Relationships ─────────────────────────────────────────
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<FamilyMember> familyMembers = new ArrayList<>();

    // ─── Audit ─────────────────────────────────────────────────
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;
}
