package com.codequest.service;

import com.codequest.ai.AiProperties;
import com.codequest.ai.AiService;
import com.codequest.ai.AiText;
import com.codequest.ai.ChatClient;
import com.codequest.dto.AiDtos.Diagnostics;
import com.codequest.dto.AiDtos.TestResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Admin page: is AI on, which model, today's usage, last error, recent calls (no prompt content). */
@Service
public class AiDiagnosticsService {
    private final AiProperties props;
    private final AiService ai;
    private final ChatClient client;
    private final JdbcTemplate jdbc;
    private final TimeService time;
    private final ObjectMapper json;

    public AiDiagnosticsService(AiProperties props, AiService ai, ChatClient client, JdbcTemplate jdbc, TimeService time, ObjectMapper json) {
        this.props = props;
        this.ai = ai;
        this.client = client;
        this.jdbc = jdbc;
        this.time = time;
        this.json = json;
    }

    public Diagnostics diagnostics(Long userId) {
        List<String> chain = props.modelChain();
        Long all = jdbc.queryForObject("select coalesce(sum(count),0) from ai_usage where usage_date = ?", Long.class, Date.valueOf(time.today(userId)));
        Long cache = jdbc.queryForObject("select count(*) from ai_cache", Long.class);
        List<Map<String, Object>> lastErr = jdbc.queryForList("select error, created_at from ai_call_log where not success order by id desc limit 1");
        List<JsonNode> recent = jdbc.queryForList("select feature, model, success, http_status, latency_ms, error, created_at from ai_call_log order by id desc limit 20")
                .stream().map(r -> (JsonNode) json.valueToTree(r)).toList();
        return new Diagnostics(props.enabled(), props.usable(), chain.isEmpty() ? null : chain.get(0), chain.size() > 1 ? chain.subList(1, chain.size()) : List.of(),
                props.dailyLimitPerUser(), ai.usageToday(userId), all == null ? 0 : all, cache == null ? 0 : cache,
                lastErr.isEmpty() ? null : (String) lastErr.get(0).get("error"),
                lastErr.isEmpty() ? null : ((java.sql.Timestamp) lastErr.get(0).get("created_at")).toInstant(), recent);
    }

    /** "Test AI connection": one tiny call straight to each model in the chain until one answers. */
    public TestResult test() {
        if (!props.usable()) return new TestResult(false, null, 0, null, "AI is not configured: set AI_ENABLED=true, OPENROUTER_API_KEY and OPENROUTER_MODEL.");
        String lastError = null;
        for (String model : props.modelChain()) {
            long t0 = System.nanoTime();
            try {
                String raw = client.complete(model, List.of(new ChatClient.Message("user", "Reply with exactly: Mentor online")), 300, props.timeoutSeconds());
                long ms = (System.nanoTime() - t0) / 1_000_000;
                return new TestResult(true, model, ms, AiText.stripReasoning(raw), null);
            } catch (ChatClient.AiCallException e) {
                lastError = model + ": " + e.getMessage();
            }
        }
        return new TestResult(false, null, 0, null, lastError);
    }

    public Instant now() { return time.now(); }
}
