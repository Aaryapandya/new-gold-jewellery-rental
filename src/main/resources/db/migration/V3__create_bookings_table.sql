-- ============================================================
-- V3: Create bookings table
-- ============================================================

CREATE TABLE bookings (
    id                      BIGSERIAL PRIMARY KEY,
    jewellery_id            BIGINT          NOT NULL REFERENCES jewellery(id),
    buyer_id                BIGINT          NOT NULL REFERENCES users(id),
    supplier_id             BIGINT          NOT NULL REFERENCES users(id),

    start_date_time         TIMESTAMP WITH TIME ZONE NOT NULL,
    end_date_time           TIMESTAMP WITH TIME ZONE NOT NULL,
    actual_return_date_time TIMESTAMP WITH TIME ZONE,

    total_amount            DECIMAL(12, 2)  NOT NULL,
    status                  VARCHAR(30)     NOT NULL DEFAULT 'REQUESTED',

    -- Optional notes
    buyer_notes             TEXT,
    rejection_reason        TEXT,
    cancellation_reason     TEXT,

    -- Audit
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    -- Business rule: end must be after start
    CONSTRAINT chk_booking_dates CHECK (end_date_time > start_date_time)
);

-- Indexes for performance
CREATE INDEX idx_bookings_jewellery_id  ON bookings(jewellery_id);
CREATE INDEX idx_bookings_buyer_id      ON bookings(buyer_id);
CREATE INDEX idx_bookings_supplier_id   ON bookings(supplier_id);
CREATE INDEX idx_bookings_status        ON bookings(status);
-- Composite index for availability overlap queries
CREATE INDEX idx_bookings_dates         ON bookings(jewellery_id, start_date_time, end_date_time);
