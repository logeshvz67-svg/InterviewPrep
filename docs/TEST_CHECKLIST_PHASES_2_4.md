# Phases 2-4 test checklist

## Automated
- [ ] `cd backend && mvn test` → all tests pass (84)
- [ ] `python3 scripts/validate_content.py` → 153/153 valid; `python3 scripts/validate_banks.py` → all banks valid
- [ ] `cd frontend && npm run build` → no errors

## Learning (Phase 2)
- [ ] Learn → Java Core Island shows sections A-J; only the first lesson of each section is open, the rest 🔒
- [ ] Open "How Java works": step through Why → … → Summary; the Visual step shows the diagram
- [ ] Try it: pasting is blocked; typing ≥ 90% match ticks the step
- [ ] Predict: reveal compares your guess with the real output
- [ ] Quiz < 80% → "try again, no penalty"; ≥ 80% after Try it + Predict → lesson complete, next lesson unlocks, XP toast
- [ ] A wrong answer offers "Why did I get this wrong?" → saved in the Mistake Notebook
- [ ] Explain it back (AI off) → self-check score + model answer; ≥ 60% gives +20 XP
- [ ] Prove it on any lesson with 90%+ → topic shows Mastered (🏅) in the tracker
- [ ] Next day: Recall → Warm-up shows the lesson's 5 flashcards; grading moves them to 3 days; "Finish recall session" gives +15 XP once
- [ ] Visualizers: each of the 6 plays/pauses/steps with captions; "predict the next step" counts correct guesses
- [ ] Quests: weekly + story progress updates; badges unlock (First Blood after logging a problem)
- [ ] Hall of Records: heatmap, charts, weekly report, JSON/CSV export download

- [ ] Every world (SQL, CS Core, Spring, Interview, Aptitude, System Design) opens its first lesson; SQL lessons say "Predict the result", Aptitude/Interview/CS say "Predict the answer"

## Problems
- [ ] Gym → Suggested shows 3 problems with reasons; "I solved it: log it" pre-fills the form; it then shows as solved in the Problem bank
- [ ] Hint ladder gives rung 1 → 4 (built-in hints when AI is off)

## AI + voice (Phase 3)
- [ ] AI off: Mentor chat answers with lesson/practice-answer search and says the AI is off
- [ ] AI on (see docs/SETUP_OPENROUTER.md): Admin → Test AI connection succeeds; chat answers; quiz generator returns 5 questions
- [ ] Doubt history search finds an old question; Resolved toggle; Convert to flashcard
- [ ] Speak (Chrome/Edge): live transcript, WPM, fillers; results show scores; with AI: feedback + improved answer
- [ ] Speak in a browser without speech recognition (e.g. Firefox): typed fallback works
- [ ] Mock interview: the interviewer speaks questions; answering 5 gives a scorecard and +60 XP

## Interview + Spring (Phase 4)
- [ ] Colosseum → Readiness: switch company and see different weights
- [ ] Question bank: answer, reveal, tick key points → score
- [ ] Aptitude: 10-question set with explanations; stats table
- [ ] Mock tests: enter YOUR exam pattern (from the official site) → start → timer counts down → auto-submit at 0 → per-section results
- [ ] Interview diary add/edit/delete
- [ ] Spring: concept map statuses; quiz 2/3 → Understood; explain-back → Can explain; milestone tick → +40 XP
- [ ] Settings: voice preview, reminder, readiness threshold; delete account (on a test account!)
- [ ] PWA: Chrome shows "Install app"; the shell opens offline (data needs the server)
