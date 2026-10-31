-- =====================================================================
-- V8 (Phase 4): question bank, aptitude, exam pattern editor, timed mock
-- tests, interview diary, interview practice, Spring track, reports.
-- =====================================================================

CREATE TABLE question_bank (
    id          BIGSERIAL PRIMARY KEY,
    ext_id      VARCHAR(20)  NOT NULL UNIQUE,
    category    VARCHAR(20)  NOT NULL,
    topic       VARCHAR(120) NOT NULL,
    difficulty  VARCHAR(10)  NOT NULL,
    companies   JSONB        NOT NULL,
    question    TEXT         NOT NULL,
    answer      TEXT         NOT NULL,
    key_points  JSONB        NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_question_bank_cat ON question_bank(category, difficulty);

-- Self-scored practice of bank questions (how many key points you covered).
CREATE TABLE interview_practice (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    question_id  BIGINT       NOT NULL REFERENCES question_bank(id) ON DELETE CASCADE,
    my_answer    TEXT,
    score        NUMERIC(5,2) NOT NULL,
    local_date   DATE         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_interview_practice_user ON interview_practice(user_id, created_at DESC);

CREATE TABLE aptitude_questions (
    id           BIGSERIAL PRIMARY KEY,
    ext_id       VARCHAR(20)  NOT NULL UNIQUE,
    section      VARCHAR(10)  NOT NULL,
    topic        VARCHAR(80)  NOT NULL,
    difficulty   VARCHAR(10)  NOT NULL,
    companies    JSONB        NOT NULL,
    question     TEXT         NOT NULL,
    options      JSONB        NOT NULL,
    answer       INT          NOT NULL,
    explanation  TEXT         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE aptitude_attempts (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    question_id  BIGINT      NOT NULL REFERENCES aptitude_questions(id) ON DELETE CASCADE,
    chosen       INT,
    correct      BOOLEAN     NOT NULL,
    seconds      INT,
    source       VARCHAR(20) NOT NULL,    -- PRACTICE | MOCK
    local_date   DATE        NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_aptitude_attempts_user ON aptitude_attempts(user_id, created_at DESC);

-- YOU enter the current official pattern here. Nothing is hardcoded.
-- sections = [{name, kind: APTITUDE|TECH_MCQ|CODING, questions, minutes, cutoffPercent}]
CREATE TABLE exam_pattern_config (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company_code   VARCHAR(30)  NOT NULL REFERENCES company_tags(code),
    name           VARCHAR(120) NOT NULL,
    sections       JSONB        NOT NULL,
    source_note    VARCHAR(300),
    verified_on    DATE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_exam_pattern UNIQUE (user_id, company_code, name)
);

CREATE TABLE mock_tests (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company_code  VARCHAR(30)  NOT NULL,
    title         VARCHAR(160) NOT NULL,
    pattern       JSONB        NOT NULL,     -- snapshot of the exam pattern used
    paper         JSONB        NOT NULL,     -- question ids per section (no answers)
    total_minutes INT          NOT NULL,
    status        VARCHAR(20)  NOT NULL,     -- RUNNING | SUBMITTED | EXPIRED
    started_at    TIMESTAMPTZ  NOT NULL,
    ends_at       TIMESTAMPTZ  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_mock_tests_user ON mock_tests(user_id, started_at DESC);

CREATE TABLE mock_test_attempts (
    id              BIGSERIAL PRIMARY KEY,
    mock_test_id    BIGINT       NOT NULL UNIQUE REFERENCES mock_tests(id) ON DELETE CASCADE,
    user_id         BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    answers         JSONB        NOT NULL,
    section_scores  JSONB        NOT NULL,
    score_percent   NUMERIC(5,2) NOT NULL,
    submitted_at    TIMESTAMPTZ  NOT NULL,
    late            BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Real questions YOU faced. The only place "actually asked" content lives, and it's yours.
CREATE TABLE interview_diary (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company        VARCHAR(80)  NOT NULL,
    round          VARCHAR(40)  NOT NULL,
    interview_date DATE         NOT NULL,
    question       TEXT         NOT NULL,
    my_answer      TEXT,
    better_answer  TEXT,
    tags           VARCHAR(200),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_diary_user ON interview_diary(user_id, interview_date DESC);

CREATE TABLE spring_topics (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(40)  NOT NULL UNIQUE,
    title       VARCHAR(120) NOT NULL,
    topic_group VARCHAR(20)  NOT NULL,
    sort_order  INT          NOT NULL,
    content     JSONB        NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE spring_topic_progress (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    topic_id         BIGINT       NOT NULL REFERENCES spring_topics(id) ON DELETE CASCADE,
    status           VARCHAR(20)  NOT NULL DEFAULT 'NOT_STARTED',
    checklist_done   JSONB        NOT NULL DEFAULT '[]'::jsonb,
    best_quiz_percent NUMERIC(5,2) NOT NULL DEFAULT 0,
    explain_back     TEXT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_spring_progress UNIQUE (user_id, topic_id)
);

CREATE TABLE spring_projects (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code        VARCHAR(40)  NOT NULL,
    title       VARCHAR(120) NOT NULL,
    description TEXT         NOT NULL,
    github_url  VARCHAR(300),
    status      VARCHAR(20)  NOT NULL DEFAULT 'NOT_STARTED',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_spring_project UNIQUE (user_id, code)
);

CREATE TABLE project_milestones (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    project_id  BIGINT       NOT NULL REFERENCES spring_projects(id) ON DELETE CASCADE,
    title       VARCHAR(200) NOT NULL,
    sort_order  INT          NOT NULL,
    done_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- entry_type: LOG | BLOCKER | LEARNED
CREATE TABLE build_logs (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    project_id  BIGINT       NOT NULL REFERENCES spring_projects(id) ON DELETE CASCADE,
    entry_type  VARCHAR(10)  NOT NULL,
    text        TEXT         NOT NULL,
    resolved    BOOLEAN      NOT NULL DEFAULT FALSE,
    local_date  DATE         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Weekly/monthly reports and daily readiness snapshots (kind = READINESS).
CREATE TABLE reports (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    kind          VARCHAR(20)  NOT NULL,     -- WEEKLY | MONTHLY | READINESS
    company_code  VARCHAR(30)  NOT NULL DEFAULT 'ALL',
    period_start  DATE         NOT NULL,
    period_end    DATE         NOT NULL,
    data          JSONB        NOT NULL,
    ai_summary    TEXT,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_report UNIQUE (user_id, kind, company_code, period_start)
);

DO $$
DECLARE t RECORD;
BEGIN
    FOR t IN SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename <> 'flyway_schema_history' LOOP
        EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY', t.tablename);
    END LOOP;
END $$;
