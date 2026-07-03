-- ============================================================
-- V2: Create jewellery and jewellery_images/bills tables
-- ============================================================

CREATE TABLE jewellery (
    id              BIGSERIAL PRIMARY KEY,
    supplier_id     BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title           VARCHAR(255)    NOT NULL,
    description     TEXT,
    weight_in_grams DECIMAL(10, 3)  NOT NULL,
    rent_per_day    DECIMAL(12, 2)  NOT NULL,
    category        VARCHAR(100)    NOT NULL,

    -- Location
    address_line    TEXT,
    city            VARCHAR(100),
    pincode         VARCHAR(10),
    latitude        DOUBLE PRECISION NOT NULL,
    longitude       DOUBLE PRECISION NOT NULL,

    -- Availability
    is_available    BOOLEAN         NOT NULL DEFAULT TRUE,
    status          VARCHAR(30)     NOT NULL DEFAULT 'UNDER_VERIFICATION',

    -- Audit
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Separate tables for multi-valued image/bill URLs (avoids serialization, enables indexing)
CREATE TABLE jewellery_images (
    id              BIGSERIAL PRIMARY KEY,
    jewellery_id    BIGINT  NOT NULL REFERENCES jewellery(id) ON DELETE CASCADE,
    image_url       TEXT    NOT NULL,
    display_order   INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE jewellery_bills (
    id              BIGSERIAL PRIMARY KEY,
    jewellery_id    BIGINT  NOT NULL REFERENCES jewellery(id) ON DELETE CASCADE,
    bill_url        TEXT    NOT NULL
);

-- Indexes
CREATE INDEX idx_jewellery_supplier_id  ON jewellery(supplier_id);
CREATE INDEX idx_jewellery_status       ON jewellery(status);
CREATE INDEX idx_jewellery_is_available ON jewellery(is_available);
CREATE INDEX idx_jewellery_category     ON jewellery(category);
CREATE INDEX idx_jewellery_city         ON jewellery(city);
-- Spatial index for lat/long radius queries
CREATE INDEX idx_jewellery_lat_lng      ON jewellery(latitude, longitude);
