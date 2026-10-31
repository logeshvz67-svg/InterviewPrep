# ⚔️ Code Quest

A gamified, visual, voice-enabled Java interview-prep and learning tracker, **Infosys-first** (Specialist Programmer and Digital Specialist Engineer), then TCS, Wipro, Cognizant, Accenture, Capgemini and product companies.

You are a developer-in-training travelling to the **Offer Letter Castle**. Bugs are monsters, concepts are spells, interviews are boss fights. The game rewards **process** (showing up, recalling, explaining, trying before peeking), never raw volume or speed.

| Part | Tech | Hosting (free) |
|---|---|---|
| Frontend | React 19 + Vite + TypeScript + Tailwind | Render static site |
| Backend | Spring Boot 3.5, Java 21, Maven, Spring Security + JWT, JPA, Flyway | Render Docker web service |
| Database | PostgreSQL | Supabase free tier |
| AI (Phase 3, optional) | OpenRouter, Nemotron `:free` model, called from the backend only | OpenRouter free tier |
| Voice (Phase 3) | Browser Web Speech API | free, in the browser |

**Only free services are used.** Nothing in this repo needs a credit card.

## Build phases

- [x] **Phase 1: foundation and daily habit.** Auth (JWT access + refresh, BCrypt, USER/ADMIN), Infosys-first onboarding and roadmap, Command Center, Focus tools (Pomodoro, Minimum Viable Day, distraction log, daily commitment, "Do this now"), Problem Gym (log problems, pattern map, revisit queue at 3/7/14 days, Mistake Notebook), server-side XP engine and streaks (freezes and Comeback Mode), Java curriculum index (78 topics, A to J) with status tracking and company tags, deployment files, docs.
- [x] **Phase 2: learning and recall.** 7 worlds; **84 fully written lessons** (all 78 Java topics A–J + the first 6 DSA lessons), each Why → Analogy → Visual → Lesson → Tiny code → Try it (typed, paste disabled) → Predict → Traps & interview box → Quiz (6–8 questions, server-graded, 80% gate) → Explain it back → auto flashcards. Prove-it tests (90% = Mastered). SM-2 flashcards (1/3/7/14/30 days) and daily Warm-up Recall. 6 interactive visualizers with "predict the next step". Weekly/story quests, 14 badges, Hall of Records, JSON/CSV export. **218 curated free practice problems** with pattern-based suggestions.
- [x] **Phase 3: AI and voice.** OpenRouter (backend only) with retries, model fallback, per-user daily limit, global throttle, cache, secret scrubbing, reasoning stripping, JSON validation and a non-AI fallback for every feature. Mentor chat (doubt / Socratic / code review / mistake explainer) with searchable Doubt History, Resolved toggle and Convert-to-flashcard; hint ladder; quiz generator; explain-back scoring; briefings. Admin AI diagnostics with "Test AI connection". Communication Gym with the Web Speech API (live transcript, WPM, fillers, pauses, frameworks, library, trends) and Mock Interview boss fights with text-to-speech.
- [x] **Phase 4: interview, Spring, remaining content.** Interview Colosseum: 250 practice Q&As (150 Java + 100 SQL/DBMS/OS/CN/DSA/Spring/HR), Aptitude Arena (75 items), **exam pattern editor (you enter the official pattern; nothing hardcoded)** with timed mock tests, Interview Diary, readiness per company. Spring Boot track: 25-topic concept map with checklist/quiz/explain-back, 30 annotation spell cards, 10 debug challenges, 4 projects with milestones and build log. Weekly/monthly reports, readiness history, PWA (installable, offline shell), gentle reminders, optional Judge0 code runner, admin lesson editor.

## Run it locally

You need Java 21, Node 20+ and a Postgres database (a local one, or your Supabase one).

```bash
cd backend
cp .env.example .env   # then edit the values
set -a && source .env && set +a
mvn spring-boot:run
```

```bash
cd frontend
cp .env.example .env   # VITE_API_URL=http://localhost:8080
npm install
npm run dev            # http://localhost:5173
```

- API docs (Swagger UI): http://localhost:8080/swagger-ui.html
- Health check: http://localhost:8080/health

Run the tests (they start their own embedded Postgres; Docker isn't needed):

```bash
cd backend && mvn test
```

## Deploy

1. [Set up Supabase](docs/SETUP_SUPABASE.md)
2. [Deploy to Render](docs/DEPLOY_RENDER.md) (backend Docker + static frontend from `render.yaml`)
3. [Keep it awake with UptimeRobot](docs/UPTIMEROBOT.md) (optional)
4. [Turn on the AI mentor with OpenRouter](docs/SETUP_OPENROUTER.md) (optional)

## ⚠️ Free-tier limits, and what to do when you hit them

Limits change; check each provider's pricing page. These were correct when this was written.

| Service | Limit | What you'll notice | What to do |
|---|---|---|---|
| **Render free web service** (backend) | Sleeps after ~15 min with no traffic. ~750 free instance-hours per month. 512 MB RAM. | First request after a nap takes ~30-60 s. The app shows a "Waking the server…" screen and retries automatically. | Wait, or set up [UptimeRobot](docs/UPTIMEROBOT.md) to ping `/health` every 5 min. One service running 24/7 uses ~744 h, which fits inside 750 h. Don't run a second always-on free service on the same account. |
| **Render static site** (frontend) | Generous bandwidth on free; doesn't sleep. | Nothing usually. | Nothing. |
| **Supabase free** | Project is **paused after ~7 days with no database activity**. 500 MB database. Limited pooler connections. | The backend can't connect: login fails and the wake screen says "still asleep". | Supabase dashboard → your project → **Restore/Resume**. UptimeRobot hitting `/health` also prevents this, because the health check runs a DB query. Export your data (Settings, Phase 4) now and then. |
| **OpenRouter free models** (Phase 3) | Per-minute and per-day request limits on `:free` models (lower without credits). Free models can be slow, busy, or removed. | "Mentor is resting, try again in a few minutes. Your lessons still work!" | Wait, or add fallback models in `OPENROUTER_FALLBACK_MODELS`. Set `AI_ENABLED=false` and everything still works with static fallbacks. Check https://openrouter.ai/models for the current free Nemotron id. |
| **Web Speech API** (Phase 3) | Speech recognition works best in Chrome/Edge and usually needs internet. | Mic button is missing or doesn't transcribe. | Type your answer instead (there's always a typed fallback). |
| **Judge0 CE code runner** (optional, off by default) | The public RapidAPI plan has a small daily quota and needs a key; self-hosting needs more RAM than Render free. | "Code runner quota reached". | Run code in your IDE or an online compiler; the app never depends on it. |

## Privacy and honesty rules baked into the app

- Your data is private: every query is filtered by your user id on the server, and Supabase's public REST API is blocked with Row Level Security.
- XP, streaks and scores are computed **only on the server**. There is no "add XP" endpoint.
- "Mastered" can only be earned with a 90%+ Prove-it test, never by clicking.
- Question banks are labelled **"practice questions in the Infosys style"**. Nothing is claimed to be an actual test question. The Infosys exam pattern is never hardcoded: you enter it from the official careers page (Phase 4).
- No guilt notifications and no punishment for missed days: streaks pause, freezes cover gaps, and Comeback Mode welcomes you back.

## Docs

- [Code walkthrough + what to learn from this code](docs/CODE_WALKTHROUGH.md)
- [Phase 1 test checklist](docs/TEST_CHECKLIST_PHASE1.md) · [Phases 2-4 test checklist](docs/TEST_CHECKLIST_PHASES_2_4.md)
- [Lesson YAML format](docs/CONTENT_FORMAT.md) · [Question bank format](docs/BANK_FORMAT.md) — validate with `python3 scripts/validate_content.py` and `python3 scripts/validate_banks.py`
- Theme, labels, ranks, world names: `frontend/src/theme/gameConfig.ts` (one file)
