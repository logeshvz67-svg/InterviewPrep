-- =====================================================================
-- V6 (Phase 2): lessons, quizzes, progress, flashcards (SM-2), quests,
-- achievements, curated problem bank.
-- =====================================================================

-- Lessons are imported from YAML files by ContentSeeder (and editable on the admin page).
CREATE TABLE lessons (
    id                 BIGSERIAL PRIMARY KEY,
    world_code         VARCHAR(30)  NOT NULL REFERENCES worlds(code),
    slug               VARCHAR(80)  NOT NULL UNIQUE,
    section            VARCHAR(10)  NOT NULL,
    sort_order         INT          NOT NULL,
    title              VARCHAR(160) NOT NULL,
    topic_id           BIGINT       REFERENCES java_topics(id),
    pattern_code       VARCHAR(40),
    estimated_minutes  INT          NOT NULL DEFAULT 20,
    content            JSONB        NOT NULL,
    content_version    INT          NOT NULL DEFAULT 1,
    admin_edited       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_lessons_world ON lessons(world_code, section, sort_order);

CREATE TABLE lesson_company_relevance (
    id            BIGSERIAL PRIMARY KEY,
    lesson_id     BIGINT      NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    company_code  VARCHAR(30) NOT NULL REFERENCES company_tags(code),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_lesson_company UNIQUE (lesson_id, company_code)
);

-- Answers live only on the server; the browser never receives them before submitting.
CREATE TABLE quiz_questions (
    id           BIGSERIAL PRIMARY KEY,
    lesson_id    BIGINT       NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    ext_id       VARCHAR(20)  NOT NULL,
    sort_order   INT          NOT NULL,
    type         VARCHAR(20)  NOT NULL,
    prompt       TEXT         NOT NULL,
    code         TEXT,
    options      JSONB,
    answer       JSONB        NOT NULL,
    explanation  TEXT         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_quiz_ext UNIQUE (lesson_id, ext_id)
);

CREATE TABLE lesson_progress (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    lesson_id           BIGINT       NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    status              VARCHAR(20)  NOT NULL DEFAULT 'IN_PROGRESS',
    steps_done          JSONB        NOT NULL DEFAULT '[]'::jsonb,
    best_quiz_percent   NUMERIC(5,2) NOT NULL DEFAULT 0,
    quiz_passed_at      TIMESTAMPTZ,
    completed_at        TIMESTAMPTZ,
    completed_via       VARCHAR(20),
    explain_back        TEXT,
    explain_back_checks JSONB,
    explain_back_score  NUMERIC(5,2),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_lesson_progress UNIQUE (user_id, lesson_id)
);

-- kind: LESSON | PROVE_IT | WARMUP. details = per-question result (ids, given, correct).
CREATE TABLE quiz_attempts (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    lesson_id      BIGINT       REFERENCES lessons(id) ON DELETE CASCADE,
    kind           VARCHAR(20)  NOT NULL,
    total          INT          NOT NULL,
    correct        INT          NOT NULL,
    score_percent  NUMERIC(5,2) NOT NULL,
    passed         BOOLEAN      NOT NULL,
    details        JSONB        NOT NULL,
    local_date     DATE         NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_quiz_attempts_user ON quiz_attempts(user_id, created_at DESC);

-- Spaced repetition (SM-2 style). next_review_on is the user's LOCAL date.
CREATE TABLE flashcards (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    front            TEXT         NOT NULL,
    back             TEXT         NOT NULL,
    topic            VARCHAR(160),
    source_type      VARCHAR(20)  NOT NULL,      -- LESSON | MISTAKE | MANUAL | QUIZ | AI
    source_ref       VARCHAR(160) NOT NULL,
    ease             NUMERIC(4,2) NOT NULL DEFAULT 2.50,
    interval_days    INT          NOT NULL DEFAULT 0,
    repetitions      INT          NOT NULL DEFAULT 0,
    lapses           INT          NOT NULL DEFAULT 0,
    next_review_on   DATE         NOT NULL,
    last_reviewed_at TIMESTAMPTZ,
    suspended        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_flashcard_source UNIQUE (user_id, source_type, source_ref)
);
CREATE INDEX idx_flashcards_due ON flashcards(user_id, next_review_on) WHERE suspended = FALSE;

CREATE TABLE flashcard_reviews (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    flashcard_id    BIGINT       NOT NULL REFERENCES flashcards(id) ON DELETE CASCADE,
    grade           INT          NOT NULL,      -- 0 again, 3 hard, 4 good, 5 easy
    interval_before INT          NOT NULL,
    interval_after  INT          NOT NULL,
    ease_after      NUMERIC(4,2) NOT NULL,
    local_date      DATE         NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_reviews_user_date ON flashcard_reviews(user_id, local_date);

CREATE TABLE achievements (
    id           BIGSERIAL PRIMARY KEY,
    code         VARCHAR(40)  NOT NULL UNIQUE,
    name         VARCHAR(80)  NOT NULL,
    description  TEXT         NOT NULL,
    icon         VARCHAR(16)  NOT NULL,
    sort_order   INT          NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE user_achievements (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    achievement_id  BIGINT      NOT NULL REFERENCES achievements(id),
    unlocked_at     TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_achievement UNIQUE (user_id, achievement_id)
);

-- Weekly and story quests are "computed": their progress is measured from your data (metric),
-- so no event wiring is needed. for_date = Monday of the week (WEEKLY) or 2000-01-01 (STORY).
ALTER TABLE quests ADD COLUMN metric VARCHAR(40);

-- Curated practice problems on free public sites (imported from content/banks/problems.yaml).
CREATE TABLE problem_bank (
    id            BIGSERIAL PRIMARY KEY,
    slug          VARCHAR(120) NOT NULL UNIQUE,
    title         VARCHAR(200) NOT NULL,
    url           VARCHAR(500) NOT NULL,
    platform      VARCHAR(30)  NOT NULL,
    difficulty    VARCHAR(10)  NOT NULL,
    pattern_code  VARCHAR(40)  NOT NULL REFERENCES patterns(code),
    companies     JSONB        NOT NULL,
    why           TEXT         NOT NULL,
    sort_order    INT          NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_problem_bank_pattern ON problem_bank(pattern_code, difficulty);

ALTER TABLE problems_solved ADD COLUMN bank_problem_id BIGINT REFERENCES problem_bank(id) ON DELETE SET NULL;

-- Remembers which content files were imported (by hash) so unchanged banks are skipped on startup.
CREATE TABLE content_imports (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(200) NOT NULL UNIQUE,
    sha256      VARCHAR(64)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Per-user settings (voice, notifications, thresholds) in one JSON column.
ALTER TABLE profiles ADD COLUMN settings JSONB NOT NULL DEFAULT '{}'::jsonb;

-- ---------------------------------------------------------------- seeds
INSERT INTO achievements (code, name, description, icon, sort_order) VALUES
 ('FIRST_BLOOD',        'First Blood',        'Log your first solved problem.',                         '🗡️', 1),
 ('PATTERN_HUNTER',     'Pattern Hunter',     'Solve problems in 8 different patterns.',               '🧭', 2),
 ('RECALL_MASTER',      'Recall Master',      'Review flashcards on 30 different days.',               '🧠', 3),
 ('VOICE_OF_CONFIDENCE','Voice of Confidence','Finish 20 speaking sessions.',                          '🎙️', 4),
 ('BUG_SLAYER',         'Bug Slayer',         'Resolve 10 mistakes in your notebook.',                  '🐞', 5),
 ('SPRING_INITIATOR',   'Spring Initiator',   'Complete your first Spring milestone or topic.',        '🌱', 6),
 ('BOSS_SLAYER',        'Boss Slayer',        'Finish your first full mock interview.',                 '🐉', 7),
 ('COMEBACK_KID',       'Comeback Kid',       'Come back after 3+ days away. Welcome back!',           '🌅', 8),
 ('JAVA_SCHOLAR',       'Java Scholar',       'Complete every lesson in one Java section.',            '📜', 9),
 ('INFOSYS_READY',      'Infosys Ready',      'Reach your chosen Infosys readiness threshold.',        '🏰', 10),
 ('SCHOLAR_10',         'Bookworm',           'Complete 10 lessons.',                                   '📚', 11),
 ('PROVEN',             'Proven',             'Pass your first Prove-it test (90%+).',                  '🎖️', 12),
 ('STREAK_7',           'One Week Strong',    'Reach a 7-day streak.',                                  '🔥', 13),
 ('FOCUS_10',           'Deep Diver',         'Finish 10 focus sessions.',                              '⏱️', 14);

INSERT INTO quests (code, kind, title, description, trigger_type, target, gold_reward, metric) VALUES
 ('W_LESSONS_3',   'WEEKLY', 'Scholar week',          'Complete 3 lessons this week.',                    'COMPUTED', 3,  40, 'LESSONS_COMPLETED'),
 ('W_RECALL_4',    'WEEKLY', 'Memory keeper',         'Do flashcard recall on 4 days this week.',         'COMPUTED', 4,  40, 'RECALL_DAYS'),
 ('W_PROBLEMS_5',  'WEEKLY', 'Monster hunter',        'Log 5 problems with your own approach.',            'COMPUTED', 5,  50, 'PROBLEMS_LOGGED'),
 ('W_FOCUS_5',     'WEEKLY', 'Deep work',             'Finish 5 focus sessions.',                          'COMPUTED', 5,  30, 'FOCUS_SESSIONS'),
 ('W_SPEAK_3',     'WEEKLY', 'Find your voice',       'Do 3 speaking practice sessions.',                  'COMPUTED', 3,  40, 'SPEAKING_SESSIONS'),
 ('S_FIRST_LESSON','STORY',  'The journey begins',    'Complete your first lesson on Java Core Island.',  'COMPUTED', 1,  25, 'LESSONS_COMPLETED_TOTAL'),
 ('S_SECTION_A',   'STORY',  'Foundations laid',      'Complete all Section A (Foundations) lessons.',     'COMPUTED', 13, 100,'SECTION_A_DONE'),
 ('S_PROVE_IT',    'STORY',  'Prove your power',      'Pass a Prove-it test with 90%+.',                   'COMPUTED', 1,  50, 'PROVE_IT_PASSED'),
 ('S_TEN_PROBLEMS','STORY',  'Bug hunter',            'Log 10 solved problems.',                           'COMPUTED', 10, 60, 'PROBLEMS_LOGGED_TOTAL'),
 ('S_FIRST_MOCK',  'STORY',  'Enter the Colosseum',   'Finish your first timed mock test.',                'COMPUTED', 1,  60, 'MOCK_TESTS_TOTAL'),
 ('S_FIRST_BOSS',  'STORY',  'Face the dragon',       'Finish your first mock interview.',                 'COMPUTED', 1,  80, 'MOCK_INTERVIEWS_TOTAL');

DO $$
DECLARE t RECORD;
BEGIN
    FOR t IN SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename <> 'flyway_schema_history' LOOP
        EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY', t.tablename);
    END LOOP;
END $$;
