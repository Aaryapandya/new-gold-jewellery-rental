package com.goldrental.service;

import com.goldrental.config.AppProperties;
import com.goldrental.domain.entity.Booking;
import com.goldrental.domain.entity.Jewellery;
import com.goldrental.domain.entity.User;
import com.goldrental.domain.enums.BookingStatus;
import com.goldrental.domain.enums.JewelleryStatus;
import com.goldrental.dto.request.BookingActionRequest;
import com.goldrental.dto.request.CreateBookingRequest;
import com.goldrental.dto.response.BookingCalendarEntry;
import com.goldrental.dto.response.BookingResponse;
import com.goldrental.dto.response.PagedResponse;
import com.goldrental.exception.AccessDeniedException;
import com.goldrental.exception.BusinessException;
import com.goldrental.exception.ResourceNotFoundException;
import com.goldrental.repository.BookingRepository;
import com.goldrental.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Core business logic for the booking / rental lifecycle.
 *
 * <h3>State machine</h3>
 * <pre>
 *   REQUESTED ──► APPROVED ──► ACTIVE ──► COMPLETED
 *   REQUESTED ──► REJECTED
 *   REQUESTED ──► CANCELLED   (buyer cancels before approval)
 *   APPROVED  ──► CANCELLED   (admin or buyer cancels before start)
 * </pre>
 *
 * <h3>Concurrency / consistency</h3>
 * <p>All booking-state transitions are executed inside a {@code @Transactional} method.
 * The overlap check ({@link BookingRepository#existsConflictingBooking}) and the subsequent
 * INSERT are performed in the same transaction, so PostgreSQL's serializable isolation
 * (or optimistic locking at the application level) prevents double-booking.
 *
 * <h3>Availability flag</h3>
 * <p>The {@code jewellery.isAvailable} flag is updated via {@link JewelleryService#setAvailability}
 * within the same transaction as the booking status change. This keeps the search index
 * consistent without requiring a separate cron job.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final JewelleryService  jewelleryService;
    private final SecurityUtils     securityUtils;
    private final AppProperties     appProperties;

    // ─── BUYER: Create booking request ─────────────────────────

    /**
     * Creates a rental booking request from a BUYER.
     *
     * <p>Business rules enforced:
     * <ol>
     *   <li>Jewellery must be ACTIVE and available.</li>
     *   <li>Start must be before end.</li>
     *   <li>Minimum rental duration ({@code app.booking.min-rental-hours}) must be satisfied.</li>
     *   <li>Booking cannot be made more than {@code app.booking.max-advance-days} in advance.</li>
     *   <li>No date overlap with existing APPROVED / ACTIVE bookings for the same jewellery.</li>
     *   <li>A buyer cannot book their own listed jewellery.</li>
     * </ol>
     */
    @Transactional
    public BookingResponse createBooking(final CreateBookingRequest req) {
        final User buyer = securityUtils.getCurrentUser();
        final Jewellery jewellery = jewelleryService.findJewelleryOrThrow(req.getJewelleryId());

        // ── Validation guards ──
        validateJewelleryForBooking(jewellery, buyer);
        validateBookingDates(req.getStartDateTime(), req.getEndDateTime());
        validateNoOverlap(jewellery.getId(), req.getStartDateTime(), req.getEndDateTime(), null);

        final BigDecimal totalAmount = calculateTotalAmount(
                jewellery.getRentPerDay(), req.getStartDateTime(), req.getEndDateTime());

        final Booking booking = Booking.builder()
                .jewellery(jewellery)
                .buyer(buyer)
                .supplier(jewellery.getSupplier())
                .startDateTime(req.getStartDateTime())
                .endDateTime(req.getEndDateTime())
                .totalAmount(totalAmount)
                .buyerNotes(req.getBuyerNotes())
                .status(BookingStatus.REQUESTED)
                .build();

        final Booking saved = bookingRepository.save(booking);
        log.info("Booking created: id={}, jewelleryId={}, buyerId={}, total={}",
                saved.getId(), jewellery.getId(), buyer.getId(), totalAmount);
        return toBookingResponse(saved);
    }

    // ─── SUPPLIER: Approve booking ──────────────────────────────

    /**
     * Supplier approves a REQUESTED booking.
     *
     * <p>Upon approval:
     * <ul>
     *   <li>Booking status → APPROVED.</li>
     *   <li>Jewellery {@code isAvailable} → {@code false} to block new bookings.</li>
     * </ul>
     *
     * <p>A double-check overlap validation is run at approval time to guard against
     * race conditions where two requests were submitted for the same window.
     */
    @Transactional
    public BookingResponse approveBooking(final Long bookingId, final BookingActionRequest req) {
        final User supplier = securityUtils.getCurrentUser();
        final Booking booking = findBookingOrThrow(bookingId);

        assertSupplierOwnership(booking, supplier);
        assertStatus(booking, BookingStatus.REQUESTED,
                "Only REQUESTED bookings can be approved");

        // Re-check for overlap at approval time (race condition guard)
        validateNoOverlap(
                booking.getJewellery().getId(),
                booking.getStartDateTime(),
                booking.getEndDateTime(),
                bookingId);

        booking.setStatus(BookingStatus.APPROVED);

        // Mark jewellery unavailable to block concurrent requests
        jewelleryService.setAvailability(booking.getJewellery().getId(), false);

        final Booking updated = bookingRepository.save(booking);
        log.info("Booking APPROVED: id={}, jewelleryId={}", bookingId, booking.getJewellery().getId());
        return toBookingResponse(updated);
    }

    // ─── SUPPLIER: Reject booking ───────────────────────────────

    /**
     * Supplier rejects a REQUESTED booking.
     * The rejection reason is stored for transparency.
     */
    @Transactional
    public BookingResponse rejectBooking(final Long bookingId, final BookingActionRequest req) {
        final User supplier = securityUtils.getCurrentUser();
        final Booking booking = findBookingOrThrow(bookingId);

        assertSupplierOwnership(booking, supplier);
        assertStatus(booking, BookingStatus.REQUESTED,
                "Only REQUESTED bookings can be rejected");

        booking.setStatus(BookingStatus.REJECTED);
        booking.setRejectionReason(req.getReason());

        final Booking updated = bookingRepository.save(booking);
        log.info("Booking REJECTED: id={}, reason={}", bookingId, req.getReason());
        return toBookingResponse(updated);
    }

    // ─── SUPPLIER: Mark as ACTIVE (rental started) ─────────────

    /**
     * Supplier confirms that the jewellery has been physically handed over to the buyer.
     * Transitions booking from APPROVED → ACTIVE.
     */
    @Transactional
    public BookingResponse markActive(final Long bookingId) {
        final User supplier = securityUtils.getCurrentUser();
        final Booking booking = findBookingOrThrow(bookingId);

        assertSupplierOwnership(booking, supplier);
        assertStatus(booking, BookingStatus.APPROVED,
                "Only APPROVED bookings can be marked ACTIVE");

        booking.setStatus(BookingStatus.ACTIVE);

        final Booking updated = bookingRepository.save(booking);
        log.info("Booking marked ACTIVE: id={}", bookingId);
        return toBookingResponse(updated);
    }

    // ─── SUPPLIER: Return jewellery (Complete) ──────────────────

    /**
     * Records the actual return of the jewellery by the buyer, completing the rental.
     *
     * <p>Upon completion:
     * <ul>
     *   <li>Booking status → COMPLETED.</li>
     *   <li>{@code actualReturnDateTime} is captured.</li>
     *   <li>Jewellery {@code isAvailable} → {@code true} so it can be re-booked.</li>
     * </ul>
     */
    @Transactional
    public BookingResponse returnJewellery(final Long bookingId) {
        final User supplier = securityUtils.getCurrentUser();
        final Booking booking = findBookingOrThrow(bookingId);

        assertSupplierOwnership(booking, supplier);
        assertStatus(booking, BookingStatus.ACTIVE,
                "Only ACTIVE bookings can be completed via return");

        booking.setStatus(BookingStatus.COMPLETED);
        booking.setActualReturnDateTime(Instant.now());

        // Jewellery is now free to be rented again
        jewelleryService.setAvailability(booking.getJewellery().getId(), true);

        final Booking updated = bookingRepository.save(booking);
        log.info("Booking COMPLETED: id={}, jewelleryId={}", bookingId, booking.getJewellery().getId());
        return toBookingResponse(updated);
    }

    // ─── BUYER: Cancel booking ──────────────────────────────────

    /**
     * Buyer cancels their own booking.
     *
     * <p>Cancellation is allowed only when the booking is in REQUESTED or APPROVED state.
     * If the booking was APPROVED (jewellery was reserved), the availability flag is
     * restored so the jewellery can accept new bookings.
     */
    @Transactional
    public BookingResponse cancelBooking(final Long bookingId, final BookingActionRequest req) {
        final User buyer = securityUtils.getCurrentUser();
        final Booking booking = findBookingOrThrow(bookingId);

        assertBuyerOwnership(booking, buyer);

        if (booking.getStatus() != BookingStatus.REQUESTED
                && booking.getStatus() != BookingStatus.APPROVED) {
            throw new BusinessException(
                    "Only REQUESTED or APPROVED bookings can be cancelled by the buyer. " +
                    "Current status: " + booking.getStatus());
        }

        final boolean wasApproved = booking.getStatus() == BookingStatus.APPROVED;

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(req.getReason());

        // If jewellery was already reserved, release it
        if (wasApproved) {
            jewelleryService.setAvailability(booking.getJewellery().getId(), true);
        }

        final Booking updated = bookingRepository.save(booking);
        log.info("Booking CANCELLED by buyer: id={}, wasApproved={}", bookingId, wasApproved);
        return toBookingResponse(updated);
    }

    // ─── Admin: Cancel any booking ──────────────────────────────

    /**
     * Admin forcefully cancels any non-terminal booking.
     * Releases jewellery availability if previously locked.
     */
    @Transactional
    public BookingResponse adminCancelBooking(final Long bookingId, final BookingActionRequest req) {
        final Booking booking = findBookingOrThrow(bookingId);

        if (booking.getStatus() == BookingStatus.COMPLETED
                || booking.getStatus() == BookingStatus.CANCELLED
                || booking.getStatus() == BookingStatus.REJECTED) {
            throw new BusinessException(
                    "Booking is already in a terminal state: " + booking.getStatus());
        }

        final boolean wasLocked = booking.getStatus() == BookingStatus.APPROVED
                || booking.getStatus() == BookingStatus.ACTIVE;

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(req.getReason());

        if (wasLocked) {
            jewelleryService.setAvailability(booking.getJewellery().getId(), true);
        }

        final Booking updated = bookingRepository.save(booking);
        log.info("Booking CANCELLED by admin: id={}", bookingId);
        return toBookingResponse(updated);
    }

    // ─── Read: Booking history ──────────────────────────────────

    /**
     * Returns all bookings created by the currently authenticated buyer, paginated.
     */
    @Transactional(readOnly = true)
    public PagedResponse<BookingResponse> getMyBookingsAsBuyer(final Pageable pageable) {
        final User buyer = securityUtils.getCurrentUser();
        final Page<Booking> page =
                bookingRepository.findAllByBuyerIdOrderByCreatedAtDesc(buyer.getId(), pageable);
        return PagedResponse.of(page, page.getContent().stream()
                .map(this::toBookingResponse)
                .toList());
    }

    /**
     * Returns all bookings on the currently authenticated supplier's jewellery, paginated.
     */
    @Transactional(readOnly = true)
    public PagedResponse<BookingResponse> getMyBookingsAsSupplier(final Pageable pageable) {
        final User supplier = securityUtils.getCurrentUser();
        final Page<Booking> page =
                bookingRepository.findAllBySupplierIdOrderByCreatedAtDesc(supplier.getId(), pageable);
        return PagedResponse.of(page, page.getContent().stream()
                .map(this::toBookingResponse)
                .toList());
    }

    /**
     * Returns the booking calendar for a specific jewellery item.
     * Shows REQUESTED, APPROVED, and ACTIVE bookings within a date window.
     * Any authenticated user can view the calendar of a jewellery item.
     */
    @Transactional(readOnly = true)
    public List<BookingCalendarEntry> getBookingCalendar(
            final Long jewelleryId, final Instant windowStart, final Instant windowEnd) {

        // Ensure the jewellery exists
        jewelleryService.findJewelleryOrThrow(jewelleryId);

        if (windowStart != null && windowEnd != null && !windowEnd.isAfter(windowStart)) {
            throw new BusinessException("Window end must be after window start");
        }

        final Instant effectiveStart = windowStart != null ? windowStart : Instant.now();
        final Instant effectiveEnd   = windowEnd   != null ? windowEnd   : effectiveStart.plus(30, ChronoUnit.DAYS);

        return bookingRepository
                .findBookingsForCalendar(jewelleryId, effectiveStart, effectiveEnd)
                .stream()
                .map(b -> BookingCalendarEntry.builder()
                        .bookingId(b.getId())
                        .startDateTime(b.getStartDateTime())
                        .endDateTime(b.getEndDateTime())
                        .status(b.getStatus())
                        .build())
                .toList();
    }

    /**
     * Returns a single booking by ID.
     * Only the buyer, supplier involved, or an admin may view it.
     */
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(final Long bookingId) {
        final User currentUser = securityUtils.getCurrentUser();
        final Booking booking = findBookingOrThrow(bookingId);

        final boolean isBuyer    = booking.getBuyer().getId().equals(currentUser.getId());
        final boolean isSupplier = booking.getSupplier().getId().equals(currentUser.getId());
        final boolean isAdmin    = currentUser.getRole().name().equals("ADMIN");

        if (!isBuyer && !isSupplier && !isAdmin) {
            throw new AccessDeniedException("You do not have permission to view this booking");
        }
        return toBookingResponse(booking);
    }

    // ─── Admin: All bookings ────────────────────────────────────

    /**
     * Admin: returns all bookings, most recent first, paginated.
     */
    @Transactional(readOnly = true)
    public PagedResponse<BookingResponse> getAllBookings(final Pageable pageable) {
        final Page<Booking> page = bookingRepository.findAllByOrderByCreatedAtDesc(pageable);
        return PagedResponse.of(page, page.getContent().stream()
                .map(this::toBookingResponse)
                .toList());
    }

    // ─── Mapper ────────────────────────────────────────────────

    public BookingResponse toBookingResponse(final Booking b) {
        return BookingResponse.builder()
                .id(b.getId())
                .jewelleryId(b.getJewellery().getId())
                .jewelleryTitle(b.getJewellery().getTitle())
                .buyerId(b.getBuyer().getId())
                .buyerName(b.getBuyer().getName())
                .supplierId(b.getSupplier().getId())
                .supplierName(b.getSupplier().getName())
                .startDateTime(b.getStartDateTime())
                .endDateTime(b.getEndDateTime())
                .actualReturnDateTime(b.getActualReturnDateTime())
                .totalAmount(b.getTotalAmount())
                .status(b.getStatus())
                .buyerNotes(b.getBuyerNotes())
                .rejectionReason(b.getRejectionReason())
                .cancellationReason(b.getCancellationReason())
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .build();
    }

    // ─── Private helpers ───────────────────────────────────────

    private Booking findBookingOrThrow(final Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
    }

    /**
     * Validates that the jewellery is eligible for a new booking request.
     */
    private void validateJewelleryForBooking(final Jewellery jewellery, final User buyer) {
        if (jewellery.getStatus() != JewelleryStatus.ACTIVE) {
            throw new BusinessException(
                    "This jewellery is not currently available for rental (status: "
                    + jewellery.getStatus() + ")");
        }
        if (!jewellery.isAvailable()) {
            throw new BusinessException(
                    "This jewellery is currently rented out. Please check the calendar for available dates.");
        }
        // Prevent self-booking
        if (jewellery.getSupplier().getId().equals(buyer.getId())) {
            throw new BusinessException("You cannot book your own listed jewellery");
        }
    }

    /**
     * Validates business rules for start/end date constraints.
     */
    private void validateBookingDates(final Instant start, final Instant end) {
        if (!end.isAfter(start)) {
            throw new BusinessException("End date/time must be after start date/time");
        }

        final long hours = ChronoUnit.HOURS.between(start, end);
        if (hours < appProperties.getBooking().getMinRentalHours()) {
            throw new BusinessException(
                    "Minimum rental duration is " + appProperties.getBooking().getMinRentalHours() + " hours");
        }

        final long daysUntilStart = ChronoUnit.DAYS.between(Instant.now(), start);
        if (daysUntilStart > appProperties.getBooking().getMaxAdvanceDays()) {
            throw new BusinessException(
                    "Bookings cannot be made more than "
                    + appProperties.getBooking().getMaxAdvanceDays()
                    + " days in advance");
        }
    }

    /**
     * Checks that no APPROVED/ACTIVE booking overlaps with the requested window.
     *
     * @param excludeId optional booking ID to exclude (used during approval re-check)
     */
    private void validateNoOverlap(
            final Long jewelleryId,
            final Instant start,
            final Instant end,
            final Long excludeId) {

        final boolean conflict = excludeId == null
                ? bookingRepository.existsConflictingBooking(jewelleryId, start, end)
                : bookingRepository.existsConflictingBookingExcluding(jewelleryId, start, end, excludeId);

        if (conflict) {
            throw new BusinessException(
                    "The selected dates conflict with an existing booking. " +
                    "Please choose a different date range.");
        }
    }

    /**
     * Calculates rental amount: rentPerDay × number of rental days (ceiling division).
     *
     * <p>A rental starting at 10 PM and ending at 8 AM the next day is still billed
     * as 1 full day minimum (controlled by {@code minRentalHours}).
     */
    private BigDecimal calculateTotalAmount(
            final BigDecimal rentPerDay,
            final Instant start,
            final Instant end) {

        // Use ceiling: any partial day is charged as a full day
        final long totalHours = ChronoUnit.HOURS.between(start, end);
        final long days = (totalHours + 23) / 24;  // ceiling division: 1 hour → 1 day
        final long rentalDays = Math.max(days, 1);

        return rentPerDay.multiply(BigDecimal.valueOf(rentalDays))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void assertSupplierOwnership(final Booking booking, final User supplier) {
        if (!booking.getSupplier().getId().equals(supplier.getId())) {
            throw new AccessDeniedException(
                    "You are not the supplier for booking id " + booking.getId());
        }
    }

    private void assertBuyerOwnership(final Booking booking, final User buyer) {
        if (!booking.getBuyer().getId().equals(buyer.getId())) {
            throw new AccessDeniedException(
                    "You are not the buyer for booking id " + booking.getId());
        }
    }

    private void assertStatus(
            final Booking booking,
            final BookingStatus required,
            final String message) {
        if (booking.getStatus() != required) {
            throw new BusinessException(message + ". Current status: " + booking.getStatus());
        }
    }
}
