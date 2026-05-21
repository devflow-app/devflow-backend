-- ============================================================
--  Auth Service — V1: Users table
-- ============================================================

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255)    NOT NULL UNIQUE,
    password        VARCHAR(255),
    first_name      VARCHAR(100)    NOT NULL,
    last_name       VARCHAR(100)    NOT NULL,
    avatar_url      VARCHAR(500),
    role            VARCHAR(50)     NOT NULL DEFAULT 'MEMBER',
    is_enabled      BOOLEAN         NOT NULL DEFAULT TRUE,
    is_email_verified BOOLEAN       NOT NULL DEFAULT FALSE,
    is_totp_enabled BOOLEAN         NOT NULL DEFAULT FALSE,
    totp_secret     VARCHAR(255),
    oauth_provider  VARCHAR(50),
    oauth_provider_id VARCHAR(255),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_oauth ON users(oauth_provider, oauth_provider_id);
CREATE INDEX idx_users_deleted_at ON users(deleted_at) WHERE deleted_at IS NULL;
