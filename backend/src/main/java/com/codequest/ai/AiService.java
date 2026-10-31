package com.codequest.ai;

import com.codequest.ai.ChatClient.AiCallException;
import com.codequest.ai.ChatClient.Message;
import com.codequest.security.JwtService;
import com.codequest.service.TimeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * The single gateway to the AI. Everything that can go wrong with a FREE model is handled here:
 *  - AI off / no key / no model      -> friendly "AI is off" result (every feature has a non-AI fallback)
 *  - per-user daily limit (ai_usage) -> "Mentor is resting"
 *  - global soft throttle (calls in the last minute, counted in the DB, no in-memory state)
 *  - 429 / 5xx / timeouts            -> retry with exponential backoff (max 2 retries), then next fallback model
 *  - reasoning output                -> stripped; raw reasoning is never shown
 *  - secrets in prompts              -> scrubbed before sending
 *  - identical generic requests      -> served from ai_cache (saves free quota)
 *  - every call logged (latency, status, model) WITHOUT prompt content
 */
@Service
public class AiService {
    private static final Logger log = LoggerFactory.getLogger(AiService.class);
    public static final String RESTING = "Mentor is resting, try again in a few minutes. Your lessons still work!";
    public static final String OFF = "The AI mentor is switched off on this server. Everything else works, and you'll see the non-AI version of this feature.";
    static final int MAX_RETRIES = 2;

    private final AiProperties props;
    private final ChatClient client;
    private final JdbcTemplate jdbc;
    private final TimeService time;
    private final ObjectMapper json;
    private final Sleeper sleeper;

    /** Lets tests skip real waiting during backoff. */
    public interface Sleeper { void sleep(long ms); }

    public AiService(AiProperties props, ChatClient client, JdbcTemplate jdbc, TimeService time, ObjectMapper json,
                     java.util.Optional<Sleeper> sleeper) {
        this.props = props;
        this.client = client;
        this.jdbc = jdbc;
        this.time = time;
        this.json = json;
        this.sleeper = sleeper.orElse(ms -> {
            try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
    }

    public record AiResult(boolean ok, String text, String model, boolean cached, String userMessage) {
        static AiResult fail(String msg) { return new AiResult(false, null, null, false, msg); }
    }

    public boolean usable() { return props.usable(); }

    /**
     * @param feature  short label for logs/cache, e.g. "doubt", "quiz-gen"
     * @param cacheKey non-null for generic requests worth caching (e.g. "explain HashMap"), null otherwise
     */
    public AiResult ask(Long userId, String feature, String system, List<Message> conversation, String cacheKey) {
        if (!props.usable()) return AiResult.fail(OFF);

        String key = cacheKey == null ? null : JwtService.sha256(feature + "|" + cacheKey.trim().toLowerCase().replaceAll("\\s+", " "));
        if (key != null) {
            List<String> hit = jdbc.queryForList("select response from ai_cache where cache_key = ?", String.class, key);
            if (!hit.isEmpty()) {
                jdbc.update("update ai_cache set hits = hits + 1, updated_at = now() where cache_key = ?", key);
                return new AiResult(true, hit.get(0), "cache", true, null);
            }
        }
        if (userId != null && usageToday(userId) >= props.dailyLimitPerUser()) return AiResult.fail(RESTING + " (daily AI limit reached; it resets at midnight)");
        Integer recent = jdbc.queryForObject("select count(*) from ai_call_log where created_at > ?", Integer.class,
                Timestamp.from(time.now().minus(Duration.ofMinutes(1))));
        if (recent != null && recent >= props.globalPerMinute()) return AiResult.fail(RESTING);

        List<Message> msgs = new ArrayList<>();
        msgs.add(new Message("system", system));
        for (Message m : conversation) msgs.add(new Message(m.role(), "user".equals(m.role()) ? AiText.scrubSecrets(m.content()) : m.content()));

        for (String model : props.modelChain()) {
            for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
                long t0 = System.nanoTime();
                try {
                    String raw = client.complete(model, msgs, props.maxOutputTokens(), props.timeoutSeconds());
                    String text = AiText.stripReasoning(raw);
                    if (text.isBlank()) throw new AiCallException(200, true, "Empty answer (model only reasoned)");
                    logCall(userId, feature, model, true, 200, t0, null);
                    if (userId != null) countUsage(userId);
                    if (key != null) {
                        jdbc.update("insert into ai_cache (cache_key, response, model) values (?, ?, ?) on conflict (cache_key) do nothing", key, text, model);
                    }
                    return new AiResult(true, text, model, false, null);
                } catch (AiCallException e) {
                    logCall(userId, feature, model, false, e.status(), t0, e.getMessage());
                    if (e.status() == 401) return AiResult.fail("The AI key was rejected. Ask the admin to check OPENROUTER_API_KEY. " + RESTING);
                    if (!e.retryable()) break;                      // try the next model
                    if (attempt < MAX_RETRIES) sleeper.sleep((long) (1000 * Math.pow(2, attempt))); // 1s, 2s
                }
            }
        }
        return AiResult.fail(RESTING);
    }

    /**
     * Ask for JSON. Free models are unreliable at strict JSON, so: strip fences, parse, validate,
     * retry ONCE with a correction message, and return empty if it still fails (caller falls back).
     */
    public Optional<JsonNode> askJson(Long userId, String feature, String system, List<Message> conversation,
                                      Predicate<JsonNode> valid, String cacheKey) {
        AiResult r = ask(userId, feature, system + "\nReturn ONLY valid JSON. No markdown, no explanation.", conversation, cacheKey);
        if (!r.ok()) return Optional.empty();
        Optional<JsonNode> parsed = AiText.extractJson(r.text(), json).filter(valid);
        if (parsed.isPresent()) return parsed;
        List<Message> retry = new ArrayList<>(conversation);
        retry.add(new Message("assistant", r.text().length() > 1500 ? r.text().substring(0, 1500) : r.text()));
        retry.add(new Message("user", "That was not valid JSON in the required shape. Reply again with ONLY the JSON."));
        AiResult r2 = ask(userId, feature, system + "\nReturn ONLY valid JSON.", retry, null);
        if (!r2.ok()) return Optional.empty();
        return AiText.extractJson(r2.text(), json).filter(valid);
    }

    public int usageToday(Long userId) {
        List<Integer> n = jdbc.queryForList("select count from ai_usage where user_id = ? and usage_date = ?", Integer.class,
                userId, Date.valueOf(time.today(userId)));
        return n.isEmpty() ? 0 : n.get(0);
    }

    private void countUsage(Long userId) {
        jdbc.update("""
                insert into ai_usage (user_id, usage_date, count) values (?, ?, 1)
                on conflict (user_id, usage_date) do update set count = ai_usage.count + 1, updated_at = now()""",
                userId, Date.valueOf(time.today(userId)));
    }

    private void logCall(Long userId, String feature, String model, boolean ok, int status, long t0, String error) {
        int ms = (int) ((System.nanoTime() - t0) / 1_000_000);
        try {
            jdbc.update("insert into ai_call_log (user_id, feature, model, success, http_status, latency_ms, error) values (?,?,?,?,?,?,?)",
                    userId, feature, model, ok, status, ms, error == null ? null : error.substring(0, Math.min(300, error.length())));
        } catch (Exception e) {
            log.warn("Could not write ai_call_log: {}", e.getMessage());
        }
        if (ok) log.info("AI ok feature={} model={} {}ms", feature, model, ms);
        else log.warn("AI fail feature={} model={} status={} {}ms: {}", feature, model, status, ms, error);
    }
}
