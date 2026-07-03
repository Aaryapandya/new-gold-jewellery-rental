-- ============================================================
-- V1: Create users and family_members tables
-- ============================================================

CREATE TABLE users (
    id                  BIGSERIAL PRIMARY KEY,
    email               VARCHAR(255) NOT NULL UNIQUE,
    password            VARCHAR(255) NOT NULL,
    name                VARCHAR(255) NOT NULL,
    mobile_number       VARCHAR(20),
    role                VARCHAR(20)  NOT NULL DEFAULT 'BUYER',

    -- Verification flags
    email_verified      BOOLEAN      NOT NULL DEFAULT FALSE,
    mobile_verified     BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,

    -- Address
    address             TEXT,
    city                VARCHAR(100),
    pincode             VARCHAR(10),
    latitude            DOUBLE PRECISION,
    longitude           DOUBLE PRECISION,

    -- Document URLs
    profile_photo_url   TEXT,
    aadhaar_front_url   TEXT,
    aadhaar_back_url    TEXT,
    pan_front_url       TEXT,
    pan_back_url        TEXT,

    -- Audit
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE family_members (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    relation    VARCHAR(100) NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes for common lookup patterns
CREATE INDEX idx_users_email      ON users(email);
CREATE INDEX idx_users_role       ON users(role);
CREATE INDEX idx_users_city       ON users(city);
CREATE INDEX idx_family_user_id   ON family_members(user_id);
