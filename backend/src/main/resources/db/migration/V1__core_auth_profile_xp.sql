-- =====================================================================
-- V1: users, auth, profile, roadmap, XP and streaks
-- Every table has created_at / updated_at (UTC, timestamptz).
-- User-owned tables carry user_id; the service layer always filters by it.
-- =====================================================================

CREATE TABLE users (
    id             BIGSERIAL PRIMARY KEY,
    email          VARCHAR(255) NOT NULL UNIQUE,
    password_hash  VARCHAR(100) NOT NULL,
    display_name   VARCHAR(80)  NOT NULL,
    role           VARCHAR(20)  NOT NULL DEFAULT 'USER',
    enabled        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Refresh tokens are stored as SHA-256 hashes, never as plain text.
CREATE TABLE refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(64)  NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);

CREATE TABLE profiles (
    id                   BIGSERIAL PRIMARY KEY,
    user_id              BIGINT       NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    character_name       VARCHAR(60),
    avatar               VARCHAR(40)  NOT NULL DEFAULT 'knight',
    primary_company      VARCHAR(30)  NOT NULL DEFAULT 'INFOSYS_SP',
    later_companies      JSONB        NOT NULL DEFAULT '[]'::jsonb,
    test_date            DATE,
    timezone             VARCHAR(60)  NOT NULL DEFAULT 'Asia/Kolkata',
    daily_minutes        INT          NOT NULL DEFAULT 60,
    why_statement        TEXT,
    self_assessment      JSONB        NOT NULL DEFAULT '{}'::jsonb,
    onboarding_complete  BOOLEAN      NOT NULL DEFAULT FALSE,
    total_xp             INT          NOT NULL DEFAULT 0,
    gold                 INT          NOT NULL DEFAULT 0,
    sound_enabled        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE roadmaps (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    start_date    DATE         NOT NULL,
    end_date      DATE,
    total_weeks   INT          NOT NULL,
    weeks         JSONB        NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_roadmaps_user ON roadmaps(user_id, active);

-- Audit trail for every XP change. XP is ONLY ever computed on the server.
-- (user_id, action, source_key) is unique so the same thing can't be rewarded twice.
CREATE TABLE xp_events (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action      VARCHAR(40)  NOT NULL,
    amount      INT          NOT NULL,
    gold        INT          NOT NULL DEFAULT 0,
    source_key  VARCHAR(120) NOT NULL,
    local_date  DATE         NOT NULL,
    note        VARCHAR(255),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_xp_once UNIQUE (user_id, action, source_key)
);
CREATE INDEX idx_xp_events_user_date ON xp_events(user_id, local_date);

CREATE TABLE streaks (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT      NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    current_streak    INT         NOT NULL DEFAULT 0,
    longest_streak    INT         NOT NULL DEFAULT 0,
    last_active_date  DATE,
    freezes           INT         NOT NULL DEFAULT 0,
    comeback_pending  BOOLEAN     NOT NULL DEFAULT FALSE,
    comeback_count    INT         NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- One row per day the user showed up (used by streaks + study heatmap).
CREATE TABLE activity_days (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    local_date  DATE        NOT NULL,
    via_freeze  BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_activity_day UNIQUE (user_id, local_date)
);
