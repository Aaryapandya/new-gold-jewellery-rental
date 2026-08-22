package com.goldrental.service;

import com.goldrental.domain.entity.User;
import com.goldrental.domain.enums.BookingStatus;
import com.goldrental.domain.enums.JewelleryStatus;
import com.goldrental.domain.enums.UserRole;
import com.goldrental.dto.request.CreateAdminRequest;
import com.goldrental.dto.response.BookingResponse;
import com.goldrental.dto.response.JewelleryResponse;
import com.goldrental.dto.response.PagedResponse;
import com.goldrental.dto.response.UserResponse;
import com.goldrental.exception.BusinessException;
import com.goldrental.exception.DuplicateResourceException;
import com.goldrental.exception.ResourceNotFoundException;
import com.goldrental.repository.BookingRepository;
import com.goldrental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Admin-only business logic.
 *
 * <p>All methods in this service are gated at the controller layer by
 * {@code @PreAuthorize("hasRole('ADMIN')")}. This service itself does not
 * re-check the role — the security layer is the single source of truth.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>User management: listing, verification toggling, activation/deactivation.</li>
 *   <li>Jewellery moderation: status changes (approve / deactivate listings).</li>
 *   <li>Booking oversight: list all bookings, force-cancel.</li>
 *   <li>Dashboard stats.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository    userRepository;
    private final BookingRepository bookingRepository;
    private final UserService       userService;
    private final JewelleryService  jewelleryService;
    private final BookingService    bookingService;
    private final PasswordEncoder   passwordEncoder;

    // ─── User management ───────────────────────────────────────

    /**
     * Lists all users with optional role and city filters, paginated.
     */
    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> getAllUsers(
            final UserRole role, final String city, final Pageable pageable) {

        final Page<User> page = userRepository.findAllWithFilters(role, city, pageable);
        return PagedResponse.of(page, page.getContent().stream()
                .map(userService::toUserResponse)
                .toList());
    }

    /**
     * Creates a new ADMIN account. Only callable by an existing ADMIN.
     * Admin accounts are created pre-verified and active.
     */
    @Transactional
    public UserResponse createAdmin(final CreateAdminRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }
        if (request.getMobileNumber() != null
                && userRepository.existsByMobileNumber(request.getMobileNumber())) {
            throw new DuplicateResourceException(
                    "Mobile number already registered: " + request.getMobileNumber());
        }

        final User admin = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .mobileNumber(request.getMobileNumber())
                .role(UserRole.ADMIN)
                .emailVerified(true)
                .mobileVerified(true)
                .isActive(true)
                .build();

        final User saved = userRepository.save(admin);
        log.info("Admin created: id={}, email={}", saved.getId(), saved.getEmail());
        return userService.toUserResponse(saved);
    }

    /**
     * Returns a specific user by ID.
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(final Long userId) {
        return userService.getUserById(userId);
    }

    /**
     * Lists users whose email or mobile verification is still pending.
     */
    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> getPendingVerificationUsers(final Pageable pageable) {
        final Page<User> page =
                userRepository.findAllByEmailVerifiedFalseOrMobileVerifiedFalse(pageable);
        return PagedResponse.of(page, page.getContent().stream()
                .map(userService::toUserResponse)
                .toList());
    }

    /**
     * Manually toggles the {@code emailVerified} flag for a user.
     */
    @Transactional
    public UserResponse setEmailVerified(final Long userId, final boolean verified) {
        final User user = findUserOrThrow(userId);
        user.setEmailVerified(verified);
        userRepository.save(user);
        log.info("Admin set emailVerified={} for userId={}", verified, userId);
        return userService.toUserResponse(user);
    }

    /**
     * Manually toggles the {@code mobileVerified} flag for a user.
     */
    @Transactional
    public UserResponse setMobileVerified(final Long userId, final boolean verified) {
        final User user = findUserOrThrow(userId);
        user.setMobileVerified(verified);
        userRepository.save(user);
        log.info("Admin set mobileVerified={} for userId={}", verified, userId);
        return userService.toUserResponse(user);
    }

    /**
     * Activates or deactivates a user account.
     * Deactivated accounts cannot log in (Spring Security's {@code isEnabled()} returns false).
     *
     * <p>Edge case: ADMIN accounts cannot be deactivated to prevent lockout.
     */
    @Transactional
    public UserResponse setUserActive(final Long userId, final boolean active) {
        final User user = findUserOrThrow(userId);

        if (!active && user.getRole() == UserRole.ADMIN) {
            throw new BusinessException("ADMIN accounts cannot be deactivated");
        }

        user.setActive(active);
        userRepository.save(user);
        log.info("Admin set isActive={} for userId={}", active, userId);
        return userService.toUserResponse(user);
    }

    // ─── Jewellery moderation ───────────────────────────────────

    /**
     * Lists all jewellery with an optional status filter for admin review.
     */
    @Transactional(readOnly = true)
    public PagedResponse<JewelleryResponse> getAllJewellery(
            final JewelleryStatus status, final Pageable pageable) {
        return jewelleryService.getAllJewellery(status, pageable);
    }

    /**
     * Admin approves / rejects / deactivates a jewellery listing by changing its status.
     *
     * <p>Typically used to:
     * <ul>
     *   <li>Move UNDER_VERIFICATION → ACTIVE (approve).</li>
     *   <li>Move ACTIVE → INACTIVE (moderate / deactivate).</li>
     *   <li>Move INACTIVE → ACTIVE (re-activate).</li>
     * </ul>
     */
    @Transactional
    public JewelleryResponse changeJewelleryStatus(
            final Long jewelleryId, final JewelleryStatus newStatus) {
        return jewelleryService.changeJewelleryStatus(jewelleryId, newStatus);
    }

    // ─── Booking oversight ──────────────────────────────────────

    /**
     * Lists all bookings on the platform, most recent first, paginated.
     */
    @Transactional(readOnly = true)
    public PagedResponse<BookingResponse> getAllBookings(final Pageable pageable) {
        return bookingService.getAllBookings(pageable);
    }

    // ─── Dashboard statistics ───────────────────────────────────

    /**
     * Returns high-level platform statistics for the admin dashboard.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStats() {
        final long totalUsers     = userRepository.count();
        final long totalBuyers    = userRepository.findAllWithFilters(UserRole.BUYER,    null, Pageable.unpaged()).getTotalElements();
        final long totalSuppliers = userRepository.findAllWithFilters(UserRole.SUPPLIER, null, Pageable.unpaged()).getTotalElements();

        final long totalBookings      = bookingRepository.count();
        final long activeBookings     = bookingRepository.countByStatus(BookingStatus.ACTIVE);
        final long requestedBookings  = bookingRepository.countByStatus(BookingStatus.REQUESTED);
        final long completedBookings  = bookingRepository.countByStatus(BookingStatus.COMPLETED);

        return Map.of(
                "totalUsers",         totalUsers,
                "totalBuyers",        totalBuyers,
                "totalSuppliers",     totalSuppliers,
                "totalBookings",      totalBookings,
                "activeBookings",     activeBookings,
                "requestedBookings",  requestedBookings,
                "completedBookings",  completedBookings
        );
    }

    // ─── Private helpers ───────────────────────────────────────

    private User findUserOrThrow(final Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}
