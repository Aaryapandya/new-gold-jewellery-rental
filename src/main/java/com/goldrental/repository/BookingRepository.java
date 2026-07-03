package com.goldrental.repository;

import com.goldrental.domain.entity.Booking;
import com.goldrental.domain.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Page<Booking> findAllByBuyerIdOrderByCreatedAtDesc(Long buyerId, Pageable pageable);

    Page<Booking> findAllBySupplierIdOrderByCreatedAtDesc(Long supplierId, Pageable pageable);

    Page<Booking> findAllByJewelleryIdOrderByStartDateTimeAsc(Long jewelleryId, Pageable pageable);

    /**
     * Checks whether a CONFLICTING booking exists for a jewellery item.
     *
     * <p>Conflict = any booking in APPROVED or ACTIVE state whose date range
     * overlaps with the requested [start, end] window.
     *
     * <p>Overlap condition: existing.start < requested.end AND existing.end > requested.start
     */
    @Query("""
            SELECT COUNT(b) > 0 FROM Booking b
            WHERE b.jewellery.id = :jewelleryId
              AND b.status IN ('APPROVED', 'ACTIVE')
              AND b.startDateTime < :endDateTime
              AND b.endDateTime > :startDateTime
            """)
    boolean existsConflictingBooking(
            @Param("jewelleryId") Long jewelleryId,
            @Param("startDateTime") Instant startDateTime,
            @Param("endDateTime") Instant endDateTime);

    /**
     * Same as above but excludes a specific booking ID (used when rescheduling).
     */
    @Query("""
            SELECT COUNT(b) > 0 FROM Booking b
            WHERE b.jewellery.id = :jewelleryId
              AND b.status IN ('APPROVED', 'ACTIVE')
              AND b.startDateTime < :endDateTime
              AND b.endDateTime > :startDateTime
              AND b.id <> :excludeId
            """)
    boolean existsConflictingBookingExcluding(
            @Param("jewelleryId") Long jewelleryId,
            @Param("startDateTime") Instant startDateTime,
            @Param("endDateTime") Instant endDateTime,
            @Param("excludeId") Long excludeId);

    /**
     * Finds the currently ACTIVE booking for a specific jewellery item.
     */
    Optional<Booking> findFirstByJewelleryIdAndStatus(Long jewelleryId, BookingStatus status);

    /**
     * Returns all bookings for a jewellery item within a date window (for calendar view).
     */
    @Query("""
            SELECT b FROM Booking b
            WHERE b.jewellery.id = :jewelleryId
              AND b.status IN ('REQUESTED', 'APPROVED', 'ACTIVE')
              AND b.startDateTime <= :windowEnd
              AND b.endDateTime >= :windowStart
            ORDER BY b.startDateTime ASC
            """)
    List<Booking> findBookingsForCalendar(
            @Param("jewelleryId") Long jewelleryId,
            @Param("windowStart") Instant windowStart,
            @Param("windowEnd") Instant windowEnd);

    /**
     * Admin: count all bookings by status.
     */
    long countByStatus(BookingStatus status);

    Page<Booking> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
