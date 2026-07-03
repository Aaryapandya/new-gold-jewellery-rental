package com.goldrental.controller;

import com.goldrental.domain.enums.JewelleryStatus;
import com.goldrental.domain.enums.UserRole;
import com.goldrental.dto.request.BookingActionRequest;
import com.goldrental.dto.response.*;
import com.goldrental.service.AdminService;
import com.goldrental.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin-only management endpoints.
 *
 * <p>All routes under {@code /admin/**} are restricted to users with the ADMIN role
 * via the global security rule in {@link com.goldrental.security.SecurityConfig}
 * and individual {@code @PreAuthorize} annotations for defence in depth.
 *
 * <pre>
 * ── Users ──────────────────────────────────────────────────────────────
 * GET    /api/v1/admin/users                      – list all users
 * GET    /api/v1/admin/users/{id}                 – get user by id
 * GET    /api/v1/admin/users/pending-verification – unverified users
 * PATCH  /api/v1/admin/users/{id}/verify-email    – toggle email verified
 * PATCH  /api/v1/admin/users/{id}/verify-mobile   – toggle mobile verified
 * PATCH  /api/v1/admin/users/{id}/activate        – activate/deactivate account
 *
 * ── Jewellery ───────────────────────────────────────────────────────────
 * GET    /api/v1/admin/jewellery                  – list all jewellery
 * PATCH  /api/v1/admin/jewellery/{id}/status      – change listing status
 *
 * ── Bookings ────────────────────────────────────────────────────────────
 * GET    /api/v1/admin/bookings                   – list all bookings
 * POST   /api/v1/admin/bookings/{id}/cancel       – force cancel a booking
 *
 * ── Dashboard ───────────────────────────────────────────────────────────
 * GET    /api/v1/admin/dashboard/stats            – platform statistics
 * </pre>
 */
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AdminService   adminService;
    private final BookingService bookingService;

    // ─── Users ─────────────────────────────────────────────────

    /**
     * Lists all users with optional role and city filters, paginated.
     *
     * <p>Sample request:
     * <pre>
     * GET /api/v1/admin/users?role=SUPPLIER&amp;city=Mumbai&amp;page=0&amp;size=20
     * Authorization: Bearer &lt;admin-token&gt;
     * </pre>
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> getAllUsers(
            @RequestParam(required = false) final UserRole role,
            @RequestParam(required = false) final String city,
            @RequestParam(defaultValue = "0")  final int page,
            @RequestParam(defaultValue = "20") final int size) {

        final Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(adminService.getAllUsers(role, city, pageable)));
    }

    /**
     * Returns a specific user's full profile.
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @PathVariable final Long id) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getUserById(id)));
    }

    /**
     * Returns users whose email or mobile verification is still pending.
     */
    @GetMapping("/users/pending-verification")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> getPendingVerificationUsers(
            @RequestParam(defaultValue = "0")  final int page,
            @RequestParam(defaultValue = "20") final int size) {

        final Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        return ResponseEntity.ok(
                ApiResponse.success(adminService.getPendingVerificationUsers(pageable)));
    }

    /**
     * Toggles the {@code emailVerified} flag for a user.
     *
     * <p>Sample request:
     * <pre>
     * PATCH /api/v1/admin/users/5/verify-email?verified=true
     * Authorization: Bearer &lt;admin-token&gt;
     * </pre>
     */
    @PatchMapping("/users/{id}/verify-email")
    public ResponseEntity<ApiResponse<UserResponse>> verifyEmail(
            @PathVariable final Long id,
            @RequestParam final boolean verified) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Email verification status updated",
                        adminService.setEmailVerified(id, verified)));
    }

    /**
     * Toggles the {@code mobileVerified} flag for a user.
     */
    @PatchMapping("/users/{id}/verify-mobile")
    public ResponseEntity<ApiResponse<UserResponse>> verifyMobile(
            @PathVariable final Long id,
            @RequestParam final boolean verified) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Mobile verification status updated",
                        adminService.setMobileVerified(id, verified)));
    }

    /**
     * Activates or deactivates a user account.
     * ADMIN accounts cannot be deactivated.
     *
     * <p>Sample request:
     * <pre>
     * PATCH /api/v1/admin/users/5/activate?active=false
     * Authorization: Bearer &lt;admin-token&gt;
     * </pre>
     */
    @PatchMapping("/users/{id}/activate")
    public ResponseEntity<ApiResponse<UserResponse>> setUserActive(
            @PathVariable final Long id,
            @RequestParam final boolean active) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        active ? "User account activated" : "User account deactivated",
                        adminService.setUserActive(id, active)));
    }

    // ─── Jewellery ─────────────────────────────────────────────

    /**
     * Lists all jewellery listings with an optional status filter.
     *
     * <p>Sample request:
     * <pre>
     * GET /api/v1/admin/jewellery?status=UNDER_VERIFICATION&amp;page=0&amp;size=20
     * Authorization: Bearer &lt;admin-token&gt;
     * </pre>
     */
    @GetMapping("/jewellery")
    public ResponseEntity<ApiResponse<PagedResponse<JewelleryResponse>>> getAllJewellery(
            @RequestParam(required = false) final JewelleryStatus status,
            @RequestParam(defaultValue = "0")  final int page,
            @RequestParam(defaultValue = "20") final int size) {

        final Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(adminService.getAllJewellery(status, pageable)));
    }

    /**
     * Changes the lifecycle status of a jewellery listing (approve, deactivate, re-activate).
     *
     * <p>Sample request:
     * <pre>
     * PATCH /api/v1/admin/jewellery/1/status?status=ACTIVE
     * Authorization: Bearer &lt;admin-token&gt;
     * </pre>
     */
    @PatchMapping("/jewellery/{id}/status")
    public ResponseEntity<ApiResponse<JewelleryResponse>> changeJewelleryStatus(
            @PathVariable final Long id,
            @RequestParam final JewelleryStatus status) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Jewellery status updated to " + status,
                        adminService.changeJewelleryStatus(id, status)));
    }

    // ─── Bookings ──────────────────────────────────────────────

    /**
     * Lists all platform bookings, most recent first.
     */
    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<PagedResponse<BookingResponse>>> getAllBookings(
            @RequestParam(defaultValue = "0")  final int page,
            @RequestParam(defaultValue = "20") final int size) {

        final Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(adminService.getAllBookings(pageable)));
    }

    /**
     * Force-cancels any non-terminal booking.
     * Releases jewellery availability if it was previously reserved.
     *
     * <p>Sample request:
     * <pre>{@code
     * POST /api/v1/admin/bookings/42/cancel
     * Authorization: Bearer <admin-token>
     * { "reason": "Policy violation" }
     * }</pre>
     */
    @PostMapping("/bookings/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> adminCancelBooking(
            @PathVariable final Long id,
            @Valid @RequestBody final BookingActionRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Booking cancelled by admin",
                        bookingService.adminCancelBooking(id, request)));
    }

    // ─── Dashboard ─────────────────────────────────────────────

    /**
     * Returns high-level platform statistics for the admin dashboard.
     *
     * <p>Sample response:
     * <pre>{@code
     * {
     *   "success": true,
     *   "data": {
     *     "totalUsers": 120,
     *     "totalBuyers": 95,
     *     "totalSuppliers": 25,
     *     "totalBookings": 340,
     *     "activeBookings": 12,
     *     "requestedBookings": 8,
     *     "completedBookings": 290
     *   }
     * }
     * }</pre>
     */
    @GetMapping("/dashboard/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getDashboardStats()));
    }
}
