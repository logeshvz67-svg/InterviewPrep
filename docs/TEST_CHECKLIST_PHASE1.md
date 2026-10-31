# Phase 1 test checklist

Tick these after deploying (or locally). ~15 minutes.

## Automated
- [ ] `cd backend && mvn test` → **51 tests, 0 failures**
- [ ] `cd frontend && npm run build` → builds with no TypeScript errors

## Server and wake-up
- [ ] `GET /health` → `{"status":"UP"}`
- [ ] `/swagger-ui.html` opens and lists all endpoints
- [ ] After the API has slept (~15 min), opening the site shows **"Waking the server…"**, then the app loads by itself

## Auth
- [ ] Register → you land on onboarding
- [ ] Wrong password → "Email or password is wrong"
- [ ] Reload the page → still logged in (refresh token works)
- [ ] Log out → back to login; Back button doesn't show your data
- [ ] Set `REGISTRATION_ENABLED=false` → "Create an account" link disappears

## Onboarding and roadmap
- [ ] Pick Infosys SP or DSE, a test date, a time budget and self-ratings → roadmap is created
- [ ] Roadmap page shows "you are here" on week 1; SP and DSE week 9 differ
- [ ] Settings → change the test date → roadmap length changes

## Command Center
- [ ] Shows max 3 quests, XP bar, streak with freezes, recall queue, countdown, radar, readiness
- [ ] "Do this now" card links to one clear next step
- [ ] Readiness says which parts are "not tracked yet" (honest)

## Gym
- [ ] Log a problem with an approach under 20 characters → friendly error
- [ ] Log a proper one → **+15 XP** (approach) and **+10 XP** if "tried first" is ticked; 3 revisit dates shown
- [ ] Pattern map updates and recommends a next pattern
- [ ] Record a mistake → **+10 XP**; weekly top mistakes appear
- [ ] Mark a mistake resolved / unresolved

## Focus
- [ ] Start a 25-min Pomodoro → "Complete" is disabled until the timer ends
- [ ] Close the tab mid-session and reopen → the timer continues from the server time
- [ ] Complete → **+10 XP**, a 5-min break timer appears, and the streak counts today
- [ ] Minimum Viable Day (20 min) → completes the streak for today (no XP, by design)
- [ ] Log a distraction → weekly insight + tip appear
- [ ] Save tomorrow's 3 tasks → **+10 XP** once per day (saving again says "Already rewarded")

## Curriculum
- [ ] 78 Java topics in sections A-J; filter by company changes the list
- [ ] Setting a topic to Learning/Understood completes the "Move a Java topic forward" quest
- [ ] "Mastered" is locked (only via Prove-it test in Phase 2)

## Streak edge cases (manual, optional)
- [ ] Study at 11:50 pm and again at 12:05 am your time → streak goes up by 1 (your midnight, not UTC)
- [ ] Skip 3+ days → kind "Welcome back" card and lighter quests (Comeback Mode)

## Security
- [ ] In Supabase Table Editor every table shows RLS enabled
- [ ] Calling `/api/command-center` without a token → 401 JSON
