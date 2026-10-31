package com.codequest.ai;

import com.codequest.ai.ChatClient.AiCallException;
import com.codequest.ai.ChatClient.Message;
import com.codequest.service.TimeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** AI failure handling with a scripted fake model: retries, fallback models, limits, JSON repair. */
class AiServiceFallbackTest {
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    TimeService time = mock(TimeService.class);
    List<String> calls = new ArrayList<>();
    List<Object> script = new ArrayList<>(); // each entry: String reply or AiCallException
    ChatClient fake = (model, msgs, max, timeout) -> {
        calls.add(model);
        Object next = script.isEmpty() ? "ok" : script.remove(0);
        if (next instanceof AiCallException e) throw e;
        return (String) next;
    };

    AiService service(boolean enabled) {
        AiProperties p = new AiProperties(enabled, "key", "primary:free", "backup:free, third:free", 30, 500, 10, 6, 8,
                "https://example.invalid", null);
        return new AiService(p, fake, jdbc, time, new ObjectMapper(), Optional.of(ms -> { }));
    }

    @BeforeEach
    void setUp() {
        when(time.today(any())).thenReturn(LocalDate.of(2026, 11, 1));
        when(time.now()).thenReturn(Instant.parse("2026-11-01T10:00:00Z"));
        when(jdbc.queryForList(anyString(), eq(Integer.class), any(), any())).thenReturn(List.of(0)); // usage today
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(Object[].class))).thenReturn(0);  // global throttle
    }

    List<Message> q() { return List.of(new Message("user", "explain HashMap")); }

    @Test
    void disabledReturnsFriendlyOffMessageWithoutCalling() {
        var r = service(false).ask(1L, "t", "sys", q(), null);
        assertThat(r.ok()).isFalse();
        assertThat(r.userMessage()).isEqualTo(AiService.OFF);
        assertThat(calls).isEmpty();
    }

    @Test
    void retriesTwiceOn429ThenFallsBackToNextModel() {
        script.add(new AiCallException(429, true, "rate"));
        script.add(new AiCallException(429, true, "rate"));
        script.add(new AiCallException(503, true, "busy"));
        script.add("<think>hmm</think>Answer from backup");
        var r = service(true).ask(1L, "t", "sys", q(), null);
        assertThat(r.ok()).isTrue();
        assertThat(r.text()).isEqualTo("Answer from backup");
        assertThat(r.model()).isEqualTo("backup:free");
        assertThat(calls).containsExactly("primary:free", "primary:free", "primary:free", "backup:free");
    }

    @Test
    void nonRetryableErrorSkipsStraightToNextModel() {
        script.add(new AiCallException(404, false, "no such model"));
        script.add("fine");
        var r = service(true).ask(1L, "t", "sys", q(), null);
        assertThat(r.ok()).isTrue();
        assertThat(calls).containsExactly("primary:free", "backup:free");
    }

    @Test
    void allModelsFailingGivesRestingMessageNotACrash() {
        for (int i = 0; i < 9; i++) script.add(new AiCallException(500, true, "down"));
        var r = service(true).ask(1L, "t", "sys", q(), null);
        assertThat(r.ok()).isFalse();
        assertThat(r.userMessage()).isEqualTo(AiService.RESTING);
        assertThat(calls).hasSize(9); // 3 models x (1 try + 2 retries)
    }

    @Test
    void reasoningOnlyReplyCountsAsFailure() {
        script.add("<think>only thinking, no answer");
        script.add("real answer");
        var r = service(true).ask(1L, "t", "sys", q(), null);
        assertThat(r.text()).isEqualTo("real answer");
    }

    @Test
    void dailyLimitStopsBeforeCalling() {
        when(jdbc.queryForList(anyString(), eq(Integer.class), any(), any())).thenReturn(List.of(30));
        var r = service(true).ask(1L, "t", "sys", q(), null);
        assertThat(r.ok()).isFalse();
        assertThat(r.userMessage()).startsWith(AiService.RESTING);
        assertThat(calls).isEmpty();
    }

    @Test
    void globalThrottleStopsBeforeCalling() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(Object[].class))).thenReturn(8);
        assertThat(service(true).ask(1L, "t", "sys", q(), null).ok()).isFalse();
        assertThat(calls).isEmpty();
    }

    @Test
    void badJsonIsRetriedOnceThenGivesUpGracefully() {
        script.add("this is not json");
        script.add("```json\n{\"accuracy\": 8, \"clarity\": 7}\n```");
        var out = service(true).askJson(1L, "t", "sys", q(), n -> n.has("accuracy"), null);
        assertThat(out).isPresent();
        assertThat(out.get().path("clarity").asInt()).isEqualTo(7);

        calls.clear();
        script.add("nope");
        script.add("still nope");
        assertThat(service(true).askJson(1L, "t", "sys", q(), n -> n.has("accuracy"), null)).isEmpty();
        assertThat(calls).hasSize(2);
    }

    @Test
    void secretsAreScrubbedBeforeSending() {
        List<List<Message>> sent = new ArrayList<>();
        ChatClient capture = (model, msgs, max, t) -> { sent.add(msgs); return "ok"; };
        AiProperties p = new AiProperties(true, "key", "m", null, 30, 500, 10, 6, 8, "x", null);
        new AiService(p, capture, jdbc, time, new ObjectMapper(), Optional.of(ms -> { }))
                .ask(1L, "t", "sys", List.of(new Message("user", "my token=abc123secret please help")), null);
        assertThat(sent.get(0).get(1).content()).doesNotContain("abc123secret");
    }
}
