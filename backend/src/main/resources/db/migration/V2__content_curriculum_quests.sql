-- =====================================================================
-- V2: shared content catalogs (worlds, patterns, companies, Java topics,
-- quests) + per-user progress tables.
-- Catalog tables have no user_id because they are the same for everyone;
-- progress tables (java_topic_progress, user_quests) do.
-- =====================================================================

CREATE TABLE company_tags (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(30)  NOT NULL UNIQUE,
    name        VARCHAR(60)  NOT NULL,
    sort_order  INT          NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE worlds (
    id           BIGSERIAL PRIMARY KEY,
    code         VARCHAR(30)  NOT NULL UNIQUE,
    name         VARCHAR(80)  NOT NULL,
    description  TEXT         NOT NULL,
    sort_order   INT          NOT NULL,
    unlock_quiz_percent INT   NOT NULL DEFAULT 80,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE patterns (
    id           BIGSERIAL PRIMARY KEY,
    code         VARCHAR(40)  NOT NULL UNIQUE,
    name         VARCHAR(80)  NOT NULL,
    description  TEXT         NOT NULL,
    sort_order   INT          NOT NULL,
    sp_core      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE mistake_categories (
    id           BIGSERIAL PRIMARY KEY,
    code         VARCHAR(40)  NOT NULL UNIQUE,
    name         VARCHAR(80)  NOT NULL,
    tip          TEXT         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- The Java curriculum index (sections A..J). Lessons attach to these in Phase 2.
CREATE TABLE java_topics (
    id               BIGSERIAL PRIMARY KEY,
    section          VARCHAR(1)   NOT NULL,
    section_name     VARCHAR(60)  NOT NULL,
    sort_order       INT          NOT NULL,
    slug             VARCHAR(80)  NOT NULL UNIQUE,
    title            VARCHAR(120) NOT NULL,
    summary          TEXT         NOT NULL,
    rebuild          BOOLEAN      NOT NULL DEFAULT FALSE,
    infosys_relevance VARCHAR(255) NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_java_topics_order ON java_topics(section, sort_order);

CREATE TABLE java_topic_companies (
    id            BIGSERIAL PRIMARY KEY,
    topic_id      BIGINT      NOT NULL REFERENCES java_topics(id) ON DELETE CASCADE,
    company_code  VARCHAR(30) NOT NULL REFERENCES company_tags(code),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_topic_company UNIQUE (topic_id, company_code)
);

-- Per-user status for each Java topic. MASTERED is only reachable via a "Prove it" test (Phase 2).
CREATE TABLE java_topic_progress (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    topic_id       BIGINT      NOT NULL REFERENCES java_topics(id) ON DELETE CASCADE,
    status         VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',
    mastery_score  NUMERIC(5,2) NOT NULL DEFAULT 0,
    last_reviewed  TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_topic_progress UNIQUE (user_id, topic_id)
);

-- Quest templates. trigger_type = which user action advances the quest.
CREATE TABLE quests (
    id            BIGSERIAL PRIMARY KEY,
    code          VARCHAR(40)  NOT NULL UNIQUE,
    kind          VARCHAR(20)  NOT NULL,           -- DAILY / WEEKLY / STORY
    title         VARCHAR(120) NOT NULL,
    description   TEXT         NOT NULL,
    trigger_type  VARCHAR(40)  NOT NULL,
    target        INT          NOT NULL DEFAULT 1,
    gold_reward   INT          NOT NULL DEFAULT 10,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE user_quests (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    quest_id      BIGINT      NOT NULL REFERENCES quests(id),
    for_date      DATE        NOT NULL,
    progress      INT         NOT NULL DEFAULT 0,
    completed_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_quest_day UNIQUE (user_id, quest_id, for_date)
);
CREATE INDEX idx_user_quests_day ON user_quests(user_id, for_date);
