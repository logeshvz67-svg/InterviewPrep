-- =====================================================================
-- V7 (Phase 3): AI mentor (OpenRouter), usage limits, cache, diagnostics,
-- Communication Gym (speaking), mock interviews.
-- No raw audio is ever stored: only transcripts and scores.
-- =====================================================================

CREATE TABLE ai_conversations (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    mode        VARCHAR(20)  NOT NULL,   -- DOUBT | SOCRATIC | CODE_REVIEW | MISTAKE | BRIEFING
    title       VARCHAR(200) NOT NULL,
    topic       VARCHAR(120),
    resolved    BOOLEAN      NOT NULL DEFAULT FALSE,
    hint_level  INT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_ai_conv_user ON ai_conversations(user_id, updated_at DESC);

CREATE TABLE ai_messages (
    id               BIGSERIAL PRIMARY KEY,
    conversation_id  BIGINT      NOT NULL REFERENCES ai_conversations(id) ON DELETE CASCADE,
    user_id          BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role             VARCHAR(12) NOT NULL,     -- user | assistant
    content          TEXT        NOT NULL,
    model            VARCHAR(160),
    fallback         BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_ai_msg_conv ON ai_messages(conversation_id, id);
CREATE INDEX idx_ai_msg_search ON ai_messages USING gin (to_tsvector('english', content));

CREATE TABLE ai_usage (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    usage_date  DATE        NOT NULL,
    count       INT         NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_ai_usage UNIQUE (user_id, usage_date)
);

CREATE TABLE ai_cache (
    id          BIGSERIAL PRIMARY KEY,
    cache_key   VARCHAR(64) NOT NULL UNIQUE,   -- SHA-256 of (feature + normalized request)
    response    TEXT        NOT NULL,
    model       VARCHAR(160),
    hits        INT         NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Latency/failure log WITHOUT prompt content (for the diagnostics page).
CREATE TABLE ai_call_log (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT,
    feature      VARCHAR(30)  NOT NULL,
    model        VARCHAR(160),
    success      BOOLEAN      NOT NULL,
    http_status  INT,
    latency_ms   INT          NOT NULL,
    error        VARCHAR(300),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_ai_call_log_time ON ai_call_log(created_at DESC);

CREATE TABLE speaking_prompts (
    id                  BIGSERIAL PRIMARY KEY,
    ext_id              VARCHAR(20)  NOT NULL UNIQUE,
    category            VARCHAR(20)  NOT NULL,
    prompt              TEXT         NOT NULL,
    framework           VARCHAR(10)  NOT NULL,
    time_limit_seconds  INT          NOT NULL,
    tips                JSONB        NOT NULL,
    sample_answer       TEXT         NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE speaking_sessions (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    prompt_id         BIGINT       REFERENCES speaking_prompts(id),
    mode              VARCHAR(20)  NOT NULL DEFAULT 'PRACTICE',  -- PRACTICE | EXPLAIN_ALOUD
    prompt_text       TEXT         NOT NULL,
    transcript        TEXT         NOT NULL,
    input_method      VARCHAR(10)  NOT NULL,      -- VOICE | TYPED
    duration_seconds  INT          NOT NULL,
    word_count        INT          NOT NULL,
    wpm               INT          NOT NULL,
    filler_count      INT          NOT NULL,
    fillers           JSONB        NOT NULL,
    long_pauses       INT          NOT NULL,
    scores            JSONB        NOT NULL,      -- structure, clarity, grammar, vocabulary, confidence (0-10)
    overall_score     NUMERIC(5,2) NOT NULL,
    feedback          TEXT,
    improved_answer   TEXT,
    ai_used           BOOLEAN      NOT NULL DEFAULT FALSE,
    local_date        DATE         NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_speaking_user ON speaking_sessions(user_id, created_at DESC);

CREATE TABLE mock_interviews (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    round_type     VARCHAR(20)  NOT NULL,     -- HR | TECHNICAL | SPRING
    status         VARCHAR(20)  NOT NULL,     -- RUNNING | COMPLETED
    question_ids   JSONB        NOT NULL,
    transcript     JSONB        NOT NULL,     -- [{role, text}]
    scorecard      JSONB,
    overall_score  NUMERIC(5,2),
    ai_used        BOOLEAN      NOT NULL DEFAULT FALSE,
    started_at     TIMESTAMPTZ  NOT NULL,
    ended_at       TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_mock_interviews_user ON mock_interviews(user_id, started_at DESC);

DO $$
DECLARE t RECORD;
BEGIN
    FOR t IN SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename <> 'flyway_schema_history' LOOP
        EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY', t.tablename);
    END LOOP;
END $$;
