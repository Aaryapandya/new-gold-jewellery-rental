-- ============================================================
-- V4: Additional indexes and a unique partial index to enforce
--     the "one ACTIVE/APPROVED booking per jewellery" invariant
--     at the database level (defence-in-depth, supplements the
--     service-layer check).
-- ============================================================

-- Unique partial index: only one non-rejected / non-cancelled /
-- non-completed booking may exist for a jewellery item at a time
-- in the APPROVED or ACTIVE state.
-- This prevents double-booking even under concurrent transactions.
CREATE UNIQUE INDEX IF NOT EXISTS uq_booking_jewellery_active
    ON bookings (jewellery_id)
    WHERE status IN ('APPROVED', 'ACTIVE');

-- Index to speed up the calendar window query
CREATE INDEX IF NOT EXISTS idx_bookings_calendar
    ON bookings (jewellery_id, start_date_time, end_date_time)
    WHERE status IN ('REQUESTED', 'APPROVED', 'ACTIVE');

-- Index to speed up supplier + status queries
CREATE INDEX IF NOT EXISTS idx_jewellery_supplier_status
    ON jewellery (supplier_id, status);
