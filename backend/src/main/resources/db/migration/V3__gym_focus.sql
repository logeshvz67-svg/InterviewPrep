-- =====================================================================
-- V3: Problem-Solving Gym, Mistake Notebook, Focus tools
-- =====================================================================

CREATE TABLE problems_solved (
    id                    BIGSERIAL PRIMARY KEY,
    user_id               BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title                 VARCHAR(200) NOT NULL,
    link                  VARCHAR(500),
    platform              VARCHAR(30)  NOT NULL,
    difficulty            VARCHAR(10)  NOT NULL,
    time_taken_minutes    INT          NOT NULL,
    attempts              INT          NOT NULL DEFAULT 1,
    hint_level            VARCHAR(20)  NOT NULL,          -- NONE / SMALL / FULL_SOLUTION
    attempted_before_hint BOOLEAN      NOT NULL DEFAULT TRUE,
    confidence            INT          NOT NULL,
    approach              TEXT         NOT NULL,
    code                  TEXT,
    solved_on             DATE         NOT NULL,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_confidence CHECK (confidence BETWEEN 1 AND 5)
);
CREATE INDEX idx_problems_user ON problems_solved(user_id, solved_on DESC);

-- Many-to-many: one problem can use several patterns.
CREATE TABLE problem_patterns (
    problem_id  BIGINT NOT NULL REFERENCES problems_solved(id) ON DELETE CASCADE,
    pattern_id  BIGINT NOT NULL REFERENCES patterns(id),
    PRIMARY KEY (problem_id, pattern_id)
);

-- Re-solve from scratch after 3 / 7 / 14 days.
CREATE TABLE problem_revisits (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    problem_id       BIGINT      NOT NULL REFERENCES problems_solved(id) ON DELETE CASCADE,
    round            INT         NOT NULL,
    due_date         DATE        NOT NULL,
    completed_at     TIMESTAMPTZ,
    solved_unaided   BOOLEAN,
    notes            TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_revisit_round UNIQUE (problem_id, round)
);
CREATE INDEX idx_revisits_due ON problem_revisits(user_id, due_date) WHERE completed_at IS NULL;

CREATE TABLE mistakes (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_code  VARCHAR(40)  NOT NULL REFERENCES mistake_categories(code),
    problem_id     BIGINT       REFERENCES problems_solved(id) ON DELETE SET NULL,
    topic          VARCHAR(120),
    what_happened  TEXT         NOT NULL,
    lesson_learned TEXT         NOT NULL,
    resolved       BOOLEAN      NOT NULL DEFAULT FALSE,
    resolved_at    TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_mistakes_user ON mistakes(user_id, created_at DESC);

-- Pomodoro sessions. The browser counts down from started_at; the server
-- only checks timestamps when the session is completed (no server timers).
CREATE TABLE focus_sessions (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    mode              VARCHAR(20)  NOT NULL,          -- POMODORO / MVD
    topic             VARCHAR(120) NOT NULL,
    planned_minutes   INT          NOT NULL,
    started_at        TIMESTAMPTZ  NOT NULL,
    ended_at          TIMESTAMPTZ,
    status            VARCHAR(20)  NOT NULL DEFAULT 'RUNNING', -- RUNNING / COMPLETED / CANCELLED
    actual_minutes    INT,
    local_date        DATE         NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_focus_user ON focus_sessions(user_id, local_date);

CREATE TABLE distraction_log (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category    VARCHAR(30)  NOT NULL,
    note        VARCHAR(300),
    local_date  DATE         NOT NULL,
    local_hour  INT          NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_distraction_user ON distraction_log(user_id, local_date);

-- "Pick tomorrow's 3 tasks". tasks = [{"text": "...", "done": false}, ...]
CREATE TABLE daily_commitments (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    for_date    DATE        NOT NULL,
    tasks       JSONB       NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_commitment_day UNIQUE (user_id, for_date)
);

-- ---------------------------------------------------------------------
-- Supabase safety net: Supabase exposes the public schema through its
-- auto-generated REST API. Turning on Row Level Security with NO policies
-- means that API can read nothing. Our Spring app connects as the table
-- owner (postgres), which bypasses RLS, so the app still works.
-- ---------------------------------------------------------------------
DO $$
DECLARE t RECORD;
BEGIN
    FOR t IN SELECT tablename FROM pg_tables
             WHERE schemaname = 'public' AND tablename <> 'flyway_schema_history' LOOP
        EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY', t.tablename);
    END LOOP;
END $$;
