package com.goldrental.controller;

import com.goldrental.dto.request.BookingActionRequest;
import com.goldrental.dto.request.CreateBookingRequest;
import com.goldrental.dto.response.ApiResponse;
import com.goldrental.dto.response.BookingCalendarEntry;
import com.goldrental.dto.response.BookingResponse;
import com.goldrental.dto.response.PagedResponse;
import com.goldrental.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * REST endpoints for the booking / rental lifecycle.
 *
 * <pre>
 * POST   /api/v1/bookings                         – create booking request   (BUYER)
 * GET    /api/v1/bookings/{id}                    – get booking detail       (buyer/supplier/admin)
 * GET    /api/v1/bookings/my/buyer                – buyer's booking history  (BUYER)
 * GET    /api/v1/bookings/my/supplier             – supplier booking list    (SUPPLIER)
 * POST   /api/v1/bookings/{id}/approve            – approve request          (SUPPLIER)
 * POST   /api/v1/bookings/{id}/reject             – reject request           (SUPPLIER)
 * POST   /api/v1/bookings/{id}/active             – mark active (handover)   (SUPPLIER)
 * POST   /api/v1/bookings/{id}/return             – record return            (SUPPLIER)
 * POST   /api/v1/bookings/{id}/cancel             – cancel booking           (BUYER)
 * GET    /api/v1/bookings/calendar/{jewelleryId}  – jewellery calendar       (authenticated)
 * </pre>
 */
@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@Slf4j
public class BookingController {

    private final BookingService bookingService;

    // ─── BUYER: Create booking request ─────────────────────────

    /**
     * Submits a rental booking request for a jewellery item.
     *
     * <p>Sample request:
     * <pre>{@code
     * POST /api/v1/bookings
     * Authorization: Bearer <buyer-token>
     * {
     *   "jewelleryId": 1,
     *   "startDateTime": "2025-02-01T10:00:00Z",
     *   "endDateTime": "2025-02-05T10:00:00Z",
     *   "buyerNotes": "Need for wedding ceremony"
     * }
     * }</pre>
     *
     * <p>Sample response (201 Created):
     * <pre>{@code
     * {
     *   "success": true,
     *   "message": "Booking request submitted",
     *   "data": {
     *     "id": 42,
     *     "jewelleryTitle": "22K Gold Necklace Set",
     *     "status": "REQUESTED",
     *     "totalAmount": 2000.00,
     *     ...
     *   }
     * }
     * }</pre>
     */
    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody final CreateBookingRequest request) {

        final BookingResponse response = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking request submitted", response));
    }

    // ─── Shared: Get booking by ID ─────────────────────────────

    /**
     * Returns the full details of a single booking.
     * Accessible by the buyer, the supplier involved, or an admin.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(
            @PathVariable final Long id) {

        return ResponseEntity.ok(ApiResponse.success(bookingService.getBookingById(id)));
    }

    // ─── BUYER: My booking history ─────────────────────────────

    /**
     * Returns all bookings created by the currently authenticated buyer.
     *
     * <p>Query params: {@code page} (default 0), {@code size} (default 20).
     */
    @GetMapping("/my/buyer")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<ApiResponse<PagedResponse<BookingResponse>>> getMyBookingsAsBuyer(
            @RequestParam(defaultValue = "0")  final int page,
            @RequestParam(defaultValue = "20") final int size) {

        final Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(
                ApiResponse.success(bookingService.getMyBookingsAsBuyer(pageable)));
    }

    // ─── SUPPLIER: Incoming bookings ────────────────────────────

    /**
     * Returns all bookings on the authenticated supplier's jewellery items.
     */
    @GetMapping("/my/supplier")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<ApiResponse<PagedResponse<BookingResponse>>> getMyBookingsAsSupplier(
            @RequestParam(defaultValue = "0")  final int page,
            @RequestParam(defaultValue = "20") final int size) {

        final Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(
                ApiResponse.success(bookingService.getMyBookingsAsSupplier(pageable)));
    }

    // ─── SUPPLIER: Approve ──────────────────────────────────────

    /**
     * Supplier approves a REQUESTED booking.
     * The jewellery is marked unavailable and reserved for the buyer.
     *
     * <p>Sample request:
     * <pre>
     * POST /api/v1/bookings/42/approve
     * Authorization: Bearer &lt;supplier-token&gt;
     * {} (empty body or no body)
     * </pre>
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<ApiResponse<BookingResponse>> approveBooking(
            @PathVariable final Long id,
            @RequestBody(required = false) final BookingActionRequest request) {

        final BookingActionRequest req = request != null ? request : new BookingActionRequest();
        return ResponseEntity.ok(
                ApiResponse.success("Booking approved", bookingService.approveBooking(id, req)));
    }

    // ─── SUPPLIER: Reject ───────────────────────────────────────

    /**
     * Supplier rejects a REQUESTED booking.
     *
     * <p>Sample request:
     * <pre>{@code
     * POST /api/v1/bookings/42/reject
     * Authorization: Bearer <supplier-token>
     * { "reason": "Item not available for that period" }
     * }</pre>
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<ApiResponse<BookingResponse>> rejectBooking(
            @PathVariable final Long id,
            @Valid @RequestBody final BookingActionRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("Booking rejected", bookingService.rejectBooking(id, request)));
    }

    // ─── SUPPLIER: Mark active (handover) ──────────────────────

    /**
     * Supplier confirms the jewellery has been physically handed over to the buyer.
     * Transitions booking APPROVED → ACTIVE.
     */
    @PostMapping("/{id}/active")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<ApiResponse<BookingResponse>> markActive(
            @PathVariable final Long id) {

        return ResponseEntity.ok(
                ApiResponse.success("Booking marked as active", bookingService.markActive(id)));
    }

    // ─── SUPPLIER: Return jewellery ────────────────────────────

    /**
     * Records the physical return of the jewellery, completing the rental.
     * Transitions booking ACTIVE → COMPLETED and restores item availability.
     */
    @PostMapping("/{id}/return")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<ApiResponse<BookingResponse>> returnJewellery(
            @PathVariable final Long id) {

        return ResponseEntity.ok(
                ApiResponse.success("Jewellery returned, booking completed",
                        bookingService.returnJewellery(id)));
    }

    // ─── BUYER: Cancel ──────────────────────────────────────────

    /**
     * Buyer cancels a REQUESTED or APPROVED booking.
     *
     * <p>Sample request:
     * <pre>{@code
     * POST /api/v1/bookings/42/cancel
     * Authorization: Bearer <buyer-token>
     * { "reason": "Change of plans" }
     * }</pre>
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable final Long id,
            @Valid @RequestBody final BookingActionRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("Booking cancelled", bookingService.cancelBooking(id, request)));
    }

    // ─── Public (authenticated): Calendar ─────────────────────

    /**
     * Returns the booking calendar for a jewellery item within an optional date window.
     * Useful for buyers to check which dates are already taken before booking.
     *
     * <p>Sample request:
     * <pre>
     * GET /api/v1/bookings/calendar/1?windowStart=2025-02-01T00:00:00Z&amp;windowEnd=2025-03-01T00:00:00Z
     * Authorization: Bearer &lt;any-token&gt;
     * </pre>
     *
     * <p>Sample response item:
     * <pre>{@code
     * { "bookingId": 42, "startDateTime": "...", "endDateTime": "...", "status": "APPROVED" }
     * }</pre>
     */
    @GetMapping("/calendar/{jewelleryId}")
    public ResponseEntity<ApiResponse<List<BookingCalendarEntry>>> getBookingCalendar(
            @PathVariable final Long jewelleryId,
            @RequestParam(required = false) final Instant windowStart,
            @RequestParam(required = false) final Instant windowEnd) {

        return ResponseEntity.ok(ApiResponse.success(
                bookingService.getBookingCalendar(jewelleryId, windowStart, windowEnd)));
    }
}
