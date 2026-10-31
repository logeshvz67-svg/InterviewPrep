# Turn on the AI mentor (OpenRouter, free models) — optional

Code Quest works **completely without AI**. Every AI feature has a non-AI fallback (lesson search, built-in hints,
seeded quizzes, rubric self-checks, template briefings). Turn AI on when you want richer feedback.

## 1. Get a free key
1. Sign up at https://openrouter.ai (free).
2. **Keys → Create key**. Copy it (starts with `sk-or-v1-`). Never put it in the frontend or in Git.

## 2. Pick the model (never hardcoded in the code)
1. Open https://openrouter.ai/models and filter by **free** (price $0). Search **Nemotron**.
2. Copy the exact model id, e.g. something like `nvidia/<model-name>:free` (ids change, so copy the current one).
3. Pick 1-2 other `:free` models as fallbacks (tried in order when the first is busy or rate-limited).

## 3. Set the variables on Render (code-quest-api → Environment)
| Key | Value |
|---|---|
| `AI_ENABLED` | `true` |
| `OPENROUTER_API_KEY` | your key |
| `OPENROUTER_MODEL` | the Nemotron `:free` id |
| `OPENROUTER_FALLBACK_MODELS` | `other/model-a:free,other/model-b:free` |
| `AI_DAILY_LIMIT_PER_USER` | `30` (default) |
| `AI_MAX_OUTPUT_TOKENS` | `1500` (reasoning models "think" first; keep it generous) |
| `AI_TIMEOUT_SECONDS` | `60` |

Save → the API redeploys. Then open **Admin → AI diagnostics → Test AI connection**.

## 4. What happens when limits hit
- Free models have per-minute and per-day request limits (lower without credits). On 429/5xx the server retries
  twice with backoff (1 s, 2 s), then tries the next fallback model, then shows *"Mentor is resting, try again in a
  few minutes. Your lessons still work!"* and uses the non-AI fallback.
- Your own daily cap (`AI_DAILY_LIMIT_PER_USER`) and a global soft throttle (`AI_GLOBAL_PER_MINUTE`, default 8/min)
  protect the free quota. Identical generic questions ("explain HashMap") are cached in the database.
- Reasoning text / `<think>` blocks are stripped; raw reasoning is never shown.
- JSON features (quiz generator, scoring, reports) validate the output, retry once, and fall back if still invalid.

## 5. Privacy
Free models may log prompts. The app tells you not to paste passwords/keys/private data and automatically removes
obvious secrets (API keys, tokens, `password=...`) before sending. Only the last 6 chat messages and a one-line
weak-topic summary are sent, never your whole history. Call logs store latency/status only, never prompt content.
