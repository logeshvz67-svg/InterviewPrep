# Supabase setup (free Postgres)

We use Supabase only as a **Postgres database**. The Spring Boot backend creates all tables itself with Flyway, so you never run SQL by hand.

## 1. Create the project

1. Go to https://supabase.com and sign up (GitHub login is fine). No card needed.
2. **New project**:
   - Name: `code-quest`
   - Database password: click **Generate**, then **save it in a password manager**. You need it in step 2.
   - Region: the closest to you (e.g. *South Asia (Mumbai)* for India). Pick the region nearest your Render region too.
   - Plan: Free.
3. Wait ~2 minutes for the project to start.

## 2. Get the connection string (use the SESSION pooler)

1. Click **Connect** (top bar), or go to Project Settings → Database.
2. Choose the **Session pooler** connection (port **5432**, host like `aws-0-ap-south-1.pooler.supabase.com`).
   - Why not "Direct connection"? On the free plan the direct host is IPv6-only, and Render's free services connect over IPv4. The pooler supports IPv4.
   - Why "Session" and not "Transaction" (port 6543)? Session mode behaves like a normal Postgres connection, so Hibernate's prepared statements and Flyway migrations work without extra flags.
3. Turn the URI into the three env vars our app uses:

| Supabase shows | Put in env var |
|---|---|
| `postgresql://postgres.abcd1234:[YOUR-PASSWORD]@aws-0-ap-south-1.pooler.supabase.com:5432/postgres` | |
| host + port + db | `DATABASE_URL=jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require` |
| the user part (note the `.project-ref`) | `DATABASE_USERNAME=postgres.abcd1234` |
| your password | `DATABASE_PASSWORD=...` |

Note the `jdbc:` prefix, and that the user and password are **not** inside `DATABASE_URL`.

## 3. Connection pool size (why 3?)

The free plan allows only a small number of pooled client connections in total. HikariCP (Spring's connection pool) keeps connections open and reuses them, so we keep it tiny:

```yaml
spring.datasource.hikari.maximum-pool-size: 3   # DB_POOL_SIZE
minimum-idle: 1
max-lifetime: 600000   # recycle every 10 min; poolers drop old idle connections
```

For one user, 3 connections handle many requests per second. A bigger pool would waste the free connection quota and Render's 512 MB of RAM.

## 4. First run

When the backend starts it runs the Flyway migrations in `backend/src/main/resources/db/migration`:

- `V1` users, auth, profile, roadmap, XP, streaks
- `V2` worlds, patterns, companies, Java topics, quests
- `V3` problem gym, mistakes, focus tools (plus Row Level Security on every table)
- `V4` seed catalogs (worlds, patterns, mistake categories, quests)
- `V5` seed the Java curriculum (78 topics, A to J) with company tags

Check in Supabase → **Table Editor**: you should see the tables, and `java_topics` should have 78 rows.

## 5. Row Level Security (RLS), and why it matters

Supabase automatically exposes your `public` tables through a REST API, using the "anon" key that is visible in browsers. Migration V3 **enables RLS on every table with no policies**, which means that API returns nothing. Our backend connects as the `postgres` owner role, which bypasses RLS, so the app works normally.

If Supabase warns "RLS enabled, no policies", that's intended. **Don't add policies** unless you know you want the public API.

## 6. When Supabase pauses

Free projects pause after about a week with no database activity. If login suddenly fails:

1. Open the Supabase dashboard → your project → **Restore project**.
2. Wait 1-2 minutes, then reload the app.

UptimeRobot pinging `/health` (see `UPTIMEROBOT.md`) prevents pausing, because the health check runs a quick database query.
