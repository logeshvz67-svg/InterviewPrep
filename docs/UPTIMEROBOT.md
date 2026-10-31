# Keep the free server awake with UptimeRobot (optional)

Render's free web service sleeps after ~15 minutes without requests. UptimeRobot (free) can call your `/health` endpoint every 5 minutes so it never falls asleep. This **also stops Supabase from pausing**, because `/health` runs a tiny database query.

## Setup (2 minutes)

1. Sign up at https://uptimerobot.com (free plan).
2. **+ New monitor**
   - Monitor type: **HTTP(s)**
   - Friendly name: `Code Quest API`
   - URL: `https://code-quest-api.onrender.com/health`
   - Monitoring interval: **5 minutes** (the free minimum)
3. Save. After a few minutes it should show **Up**.

## Things to know

- **Free hours**: Render gives ~750 free instance-hours a month per workspace. One service awake 24/7 uses about 744 hours, so it fits, but **only one** always-on free service fits. If you add more free services later, stop pinging this one or accept cold starts.
- **It's optional.** Without it, the app still works; you just see the wake screen after long breaks.
- **Quieter option**: ping only during your study hours (UptimeRobot's free plan has no schedules, so you'd pause the monitor manually), or skip it and let the wake screen handle cold starts.
- UptimeRobot can email you if the API goes down. That's a nice early warning if Supabase paused or a deploy broke.
