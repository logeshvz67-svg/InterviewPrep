# Code walkthrough (Phase 1)

This project is also your Spring Boot learning project. Read it in this order.

## 1. The journey of one request

```
Browser (React)
  │  POST /api/problems   Authorization: Bearer eyJ...
  ▼
Security filter chain ──► JwtAuthFilter      (security/JwtAuthFilter.java)
  │                         checks the token signature, puts UserPrincipal in the SecurityContext
  ▼
DispatcherServlet ──► GymController.log()    (controller/)    @Valid checks the JSON body
  ▼
GymService.log()        @Transactional       (service/)       business rules, XP, quests
  ▼
ProblemSolvedRepository.save()               (repository/)    Spring Data writes the SQL
  ▼
PostgreSQL (Supabase)   tables made by Flyway (db/migration/)
```

If anything throws, `GlobalExceptionHandler` (`exception/`) turns it into JSON with the right HTTP status.

## 2. Folder map (`backend/src/main/java/com/codequest`)

| Package | What lives there | Rule of thumb |
|---|---|---|
| `controller` | `@RestController` classes | Thin. Read request, call ONE service method, return a DTO. |
| `service` | `@Service` classes | All business rules and `@Transactional` boundaries. |
| `repository` | Spring Data interfaces | Only database access. Method names become queries. |
| `entity` | `@Entity` classes = tables | Never returned directly to the browser. |
| `dto` | Java `record`s = JSON shapes | What the API accepts/returns, with validation annotations. |
| `mapper` | `Mappers` | Entity → DTO, written by hand so you can read every line. |
| `domain` | Pure Java logic (XP, ranks, streak math, roadmap, readiness) | No Spring, no DB, so it's easy to unit-test. |
| `security` | JWT creation/parsing, the filter | |
| `config` | Security rules, CORS, Clock, Swagger, typed settings | |
| `exception` | `ApiException`, `GlobalExceptionHandler` | |

## 3. Spring concepts used, and where to look

### Dependency Injection (DI) and the IoC container
Spring creates one object ("bean") of each `@Service`, `@Repository`, `@RestController` and `@Configuration`, and passes beans into each other's constructors. Look at any service constructor, e.g. `XpService(XpEventRepository events, ProfileRepository profiles, ...)`. We use **constructor injection** (not `@Autowired` on fields) because the fields can be `final`, and tests can just call `new XpService(mocks...)` (see `XpServiceTest`).

*Analogy*: a restaurant kitchen. You (the class) say "I need a knife and a pan" in your constructor; the head chef (the IoC container) hands them to you. You never go shopping yourself.

### `@Transactional`
A transaction is "all or nothing". In `GymService.log()`, saving the problem, creating 3 revisits and writing XP events all commit together, or all roll back if something fails. `XpService.award()` is also `@Transactional`, but it **joins** the caller's transaction (propagation `REQUIRED`, the default) instead of starting a new one.

`@Transactional(readOnly = true)` on read methods tells Hibernate to skip change-tracking (faster), and keeps the session open so lazy fields can load.

### Dirty checking
In `XpService.award()` we change `profile.setTotalXp(...)` and **never call save()**. Inside a transaction, Hibernate remembers the loaded entity and writes any changes automatically at commit. This surprises everyone the first time!

### Lazy loading, `LazyInitializationException`, and N+1
`JavaTopicProgress.topic` is `@ManyToOne(fetch = LAZY)`: Hibernate loads the topic only when you touch it. If you touch it **after** the transaction closed, you get `LazyInitializationException`. We avoid that by:
- building DTOs inside `@Transactional` service methods, and
- `spring.jpa.open-in-view: false`, so mistakes show up in development instead of silently holding DB connections open.

**N+1 problem**: loading 78 topics and then each topic's company tags = 1 + 78 queries. `JavaTopicRepository` uses `@EntityGraph(attributePaths = "companyCodes")` to fetch them in one join.

### The JWT filter chain
`SecurityConfig` builds the chain: stateless (no sessions), CSRF off (we don't use cookies), CORS restricted to your frontend, public URLs listed explicitly, `/api/admin/**` needs `ROLE_ADMIN`, everything else needs a valid token. `JwtAuthFilter` runs before Spring's own login filter and sets the logged-in user.

- **Access token** (15 min): a signed JWT. The server doesn't store it; the HMAC signature proves we issued it.
- **Refresh token** (14 days): a random string, stored **hashed** (SHA-256) in `refresh_tokens`. Each refresh **rotates** it (old one revoked). Reusing a revoked token logs you out everywhere, in case it was stolen.
- Passwords: **BCrypt** (cost 10), deliberately slow to make cracking expensive.

### Validation and errors
DTO records carry annotations like `@NotBlank`, `@Size`, `@Min`. `@Valid` on the controller parameter triggers them; failures become a `400` with an `errors` map per field. Status codes used: `200` OK, `201` Created, `204` No Content, `400` bad input, `401` not logged in, `403` not allowed, `404` not found (also used when a row belongs to someone else, so we don't reveal it exists), `405` wrong method, `409` conflict.

### Per-user data isolation
Every repository method that reads user data takes `userId`, e.g. `findByIdAndUserId(id, userId)`. The `userId` comes from the verified token, never from the request body. Integration test `usersCannotSeeEachOthersData` proves it.

### Time and money types
- `Instant` (UTC) for moments in time: `created_at`, `started_at`. DB columns are `timestamptz`.
- `LocalDate` for "which day was this for the user": computed by `TimeService` using **your** timezone, so a streak day ends at midnight in India, not midnight UTC (test: `streakFollowsIndianMidnightNotUtcMidnight`).
- `BigDecimal` for scores (readiness, mastery): exact decimals, no `0.1 + 0.2` errors.
- An injected `Clock` bean instead of `Instant.now()`, so tests can move time.

### Flyway
Each `V<n>__name.sql` runs once, in order, and is recorded in `flyway_schema_history`. **Never edit a migration that already ran on Supabase**; add a new `V6__...` instead. Hibernate runs with `ddl-auto: validate`: it only checks the tables match the entities, it never changes them.

## 4. The game rules (where they live)

| Rule | File |
|---|---|
| XP per action + daily caps | `domain/XpAction.java` |
| Ranks: Intern 0, Trainee 500, Developer 1500, Senior Dev 3500, Tech Lead 7000, Architect 12000 | `domain/Rank.java` |
| Levels: level n starts at 50·n·(n-1) XP | `domain/LevelInfo.java` |
| Streaks, freezes (1 per 7 days, max 2), Comeback Mode (3+ days away) | `domain/StreakCalculator.java` |
| Revisit schedule 3/7/14 days | `domain/RevisitSchedule.java` |
| Roadmap (4:5:3 phase split, fits a test date) | `domain/RoadmapGenerator.java` |
| Readiness weights (SP is DSA-heavier) | `domain/ReadinessCalculator.java` |
| Pattern strength | `GymService.strengthScore` |

## 5. Tests

```bash
cd backend && mvn test
```

- `domain/*Test`: pure unit tests for streaks, levels, roadmap, readiness, revisits.
- `service/XpServiceTest`: Mockito, with repositories replaced by fakes.
- `integration/ApiFlowIntegrationTest`: real HTTP → security → controller → service → **real Postgres** (embedded, started by the test, no Docker needed).

## 6. What you should learn from this code (Phase 1 checklist)

Try to explain each one out loud in 3 sentences:

1. Why does a controller return a DTO instead of an entity?
2. What would break if `GymService.log()` weren't `@Transactional`?
3. Why does `XpService.award()` never call `profileRepository.save()`?
4. What is `LazyInitializationException`, and how does `@Transactional(readOnly = true)` avoid it?
5. What is the N+1 problem, and how does `@EntityGraph` fix it?
6. Walk through what `JwtAuthFilter` does for a request with an expired token.
7. Why is the refresh token hashed in the database but the access token not stored at all?
8. Why do we inject a `Clock` instead of calling `Instant.now()`?
9. Why `LocalDate` for streak days but `Instant` for `created_at`?
10. Why is the Hikari pool only 3 connections?

---

# Phases 2-4: what's new in the code

## Where things live
| Feature | Backend | Frontend |
|---|---|---|
| Lessons, quiz gate, Prove-it, explain-back | `service/LessonService`, `content/ContentSeeder` (YAML → DB), `domain/QuizGrader` | `pages/learn/*`, `components/QuizRunner.tsx` |
| Flashcards + warm-up recall | `service/FlashcardService`, `service/RecallService`, `domain/Sm2Scheduler` | `pages/recall/RecallPage.tsx` |
| Problem bank + suggestions | `service/ProblemBankService` | `pages/GymPage.tsx` (Suggested / Problem bank tabs) |
| Quests, badges, records, reports | `service/QuestBoardService`, `AchievementService`, `StatsService`, `RecordsService`, `ReportService` | `pages/progress/*` |
| AI gateway | `ai/AiService` (+ `OpenRouterClient`, `AiText`, `AiProperties`) | — |
| AI features | `service/MentorService`, `AiDiagnosticsService` | `pages/mentor/MentorPage.tsx`, `pages/AdminPage.tsx` |
| Speaking + mock interviews | `domain/SpeakingMetrics`, `service/SpeakingService`, `MockInterviewService` | `pages/speak/*` |
| Colosseum, aptitude, exam patterns, mock tests, diary | `service/PracticeService`, `ExamService`, `DiaryService` | `pages/colosseum/*` |
| Spring track | `service/SpringTrackService`, `content/ContentCatalog` | `pages/colosseum/Spring*.tsx` |
| Visualizers | — (pure frontend) | `components/viz/*`, `pages/viz/*` |

## New Spring/Java concepts to study (and where)

1. **`ApplicationRunner`** (`ContentSeeder`): code that runs once after the app starts. Runs *after* Flyway created the
   tables and *after* Tomcat opened the port, so Render's health check passes while content imports.
2. **`TransactionTemplate`** (`ContentSeeder`): a transaction started by code instead of `@Transactional`. Each file gets
   its own transaction, so one broken file can't undo the others.
3. **Two data-access styles on purpose**: JPA entities for things you load, change and save (lessons, flashcards,
   mock tests); `JdbcTemplate` + plain SQL for *aggregate questions* ("count X per week") and append-only logs
   (`StatsService`, `RecordsService`, `ai_call_log`). Rule of thumb: if you'd never edit the row as an object, SQL is simpler.
4. **JSONB columns** (`@JdbcTypeCode(SqlTypes.JSON)`): lesson content, quiz answers, transcripts. Flexible documents
   inside a relational DB. The quiz `answer` column never leaves the server before submission.
5. **`@ConfigurationProperties` records** (`AiProperties`): typed config from env vars. Model names come only from
   `OPENROUTER_MODEL` / `OPENROUTER_FALLBACK_MODELS`.
6. **Programming to an interface** (`ChatClient` ← `OpenRouterClient`): `AiServiceFallbackTest` swaps in a scripted
   fake model to test retries, fallbacks and limits without any network. This is *why* interfaces + DI matter.
7. **Resilience patterns** (`AiService.ask`): retry with exponential backoff, fallback chain, rate limiting from DB
   counts (no in-memory state, survives the free server sleeping), caching by hash, graceful degradation.
8. **Defensive parsing** (`AiText.extractJson`): never trust model output; strip fences, parse, validate, retry once, fall back.
9. **Pure domain logic** (`Sm2Scheduler`, `QuizGrader`, `SpeakingMetrics`): no Spring, no DB, 100% unit-testable.
   The server **re-computes** speaking metrics instead of trusting the browser.
10. **Optional<T> injection** (`AiService(…, Optional<Sleeper>)`): an optional dependency; tests pass a no-wait sleeper.
11. **`ON CONFLICT … DO UPDATE` (upsert)** in Postgres (`ai_usage`, `reports`, `content_imports`): atomic counters
    without race conditions.
12. **Full-text search** (`AiConversationRepository.search`): `to_tsvector … @@ plainto_tsquery` with a GIN index.

## Phases 2-4 "explain it out loud" checklist
1. Why does the lesson API never send quiz answers, and where is grading done?
2. How does the 80% gate decide if a lesson is locked? What does Prove-it change?
3. Walk through SM-2: what happens to a card's interval on AGAIN vs GOOD vs EASY?
4. What exactly happens in `AiService.ask` when the first model returns HTTP 429 three times?
5. Why is the AI throttle counted in the database and not in a Java variable?
6. Why do we strip `<think>` blocks, and what if a reply is *only* reasoning?
7. Why is the Infosys exam pattern a user-entered table instead of constants in code?
8. When would you choose `JdbcTemplate` over a JPA repository?
