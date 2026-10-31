package com.codequest.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Lessons -> quiz gate -> flashcards -> recall, plus AI-off fallbacks, end to end on real Postgres. */
@AutoConfigureMockMvc
class LearningFlowIntegrationTest extends IntegrationTestBase {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired MutableClock clock;

    String register() throws Exception {
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"l" + UUID.randomUUID() + "@t.com\",\"password\":\"password123\",\"displayName\":\"L\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("accessToken").asText();
    }

    ResultActions call(String method, String url, String token, String body) throws Exception {
        var req = request(HttpMethod.valueOf(method), url).header("Authorization", "Bearer " + token);
        if (body != null) req.contentType(MediaType.APPLICATION_JSON).content(body);
        return mvc.perform(req);
    }

    JsonNode read(ResultActions r) throws Exception { return json.readTree(r.andReturn().getResponse().getContentAsString()); }

    /** Build the correct answers straight from the database (the API never exposes them). */
    String correctAnswers(String slug) {
        return jdbc.queryForObject("""
                select json_object_agg(ext_id, case type when 'FILL_THE_CODE' then (answer->'accepted'->0)::json
                  when 'MATCH' then (select json_agg(p->'right') from jsonb_array_elements(answer->'pairs') p)
                  else (answer->'index')::json end)
                from quiz_questions q join lessons l on l.id = q.lesson_id where l.slug = ?""", String.class, slug);
    }

    @Test
    void contentIsSeeded() {
        assertThat(jdbc.queryForObject("select count(*) from lessons", Integer.class)).isGreaterThanOrEqualTo(153);
        assertThat(jdbc.queryForObject("select count(*) from problem_bank", Integer.class)).isGreaterThan(100);
        assertThat(jdbc.queryForObject("select count(*) from question_bank", Integer.class)).isGreaterThanOrEqualTo(250);
    }

    @Test
    void lessonNeverLeaksAnswersAndSecondLessonIsGated() throws Exception {
        String t = register();
        JsonNode lesson = read(call("GET", "/api/lessons/how-java-works", t, null).andExpect(status().isOk()));
        assertThat(lesson.get("quiz").toString()).doesNotContain("\"answer\"").doesNotContain("accepted").doesNotContain("pairs");
        assertThat(lesson.at("/content/quiz").isMissingNode()).isTrue();
        call("GET", "/api/lessons/setup-first-program", t, null).andExpect(jsonPath("$.locked").value(true)).andExpect(jsonPath("$.content").isEmpty());
        call("POST", "/api/lessons/setup-first-program/quiz", t, "{\"answers\":{}}").andExpect(status().isForbidden());
    }

    @Test
    void passingQuizAfterRequiredStepsCompletesLessonUnlocksNextAndCreatesFlashcards() throws Exception {
        clock.set(Instant.parse("2026-11-10T05:00:00Z"));
        String t = register();
        // Wrong answers: no pass, no penalty.
        call("POST", "/api/lessons/how-java-works/quiz", t, "{\"answers\":{\"q1\":3}}")
                .andExpect(jsonPath("$.passed").value(false));
        // Pass the quiz before doing the steps: passed, but not complete yet.
        call("POST", "/api/lessons/how-java-works/quiz", t, "{\"answers\":" + correctAnswers("how-java-works") + "}")
                .andExpect(jsonPath("$.passed").value(true)).andExpect(jsonPath("$.lessonCompleted").value(false));
        call("POST", "/api/lessons/how-java-works/steps", t, "{\"step\":\"tryIt\"}").andExpect(status().isOk());
        call("POST", "/api/lessons/how-java-works/steps", t, "{\"step\":\"predict\"}")
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        call("GET", "/api/lessons/setup-first-program", t, null).andExpect(jsonPath("$.locked").value(false));
        // Topic status moved by real learning, not clicking.
        JsonNode cur = read(call("GET", "/api/curriculum/java", t, null));
        assertThat(cur.toString()).contains("\"slug\":\"how-java-works\"");
        // Flashcards appear for TOMORROW, then show up in recall.
        assertThat(read(call("GET", "/api/flashcards", t, null)).get("totalItems").asInt()).isEqualTo(5);
        assertThat(read(call("GET", "/api/flashcards/due", t, null)).size()).isZero();
        clock.set(Instant.parse("2026-11-11T05:00:00Z"));
        JsonNode due = read(call("GET", "/api/flashcards/due", t, null));
        assertThat(due.size()).isEqualTo(5);
        for (JsonNode c : due) {
            call("POST", "/api/flashcards/" + c.get("id").asLong() + "/review", t, "{\"grade\":\"GOOD\"}").andExpect(status().isOk());
        }
        call("POST", "/api/recall/complete", t, null).andExpect(jsonPath("$.xp.awarded").value(true));
        call("POST", "/api/recall/complete", t, null).andExpect(jsonPath("$.xp.awarded").value(false)); // once per day
        JsonNode warm = read(call("GET", "/api/recall/warmup", t, null));
        assertThat(warm.get("questions").size()).isEqualTo(3);
    }

    @Test
    void proveItAt90PercentMarksTopicMastered() throws Exception {
        String t = register();
        call("POST", "/api/lessons/string-pool/prove-it", t, "{\"answers\":" + correctAnswers("string-pool") + "}")
                .andExpect(jsonPath("$.passed").value(true)).andExpect(jsonPath("$.lessonCompleted").value(true));
        JsonNode cur = read(call("GET", "/api/curriculum/java", t, null));
        boolean mastered = false;
        for (JsonNode s : cur.get("sections")) for (JsonNode tp : s.get("topics"))
            if ("string-pool".equals(tp.get("slug").asText())) mastered = "MASTERED".equals(tp.get("status").asText());
        assertThat(mastered).isTrue();
    }

    @Test
    void aiOffEverythingStillAnswers() throws Exception {
        String t = register();
        call("POST", "/api/mentor/chat", t, "{\"mode\":\"DOUBT\",\"message\":\"What is the String pool?\"}")
                .andExpect(jsonPath("$.aiUsed").value(false)).andExpect(jsonPath("$.reply.fallback").value(true));
        call("POST", "/api/mentor/hint", t, "{\"problemTitle\":\"Two Sum\",\"patternCode\":\"HASHING\",\"level\":2}")
                .andExpect(jsonPath("$.aiUsed").value(false));
        call("POST", "/api/mentor/quiz", t, "{\"topic\":\"HashMap\",\"count\":3}")
                .andExpect(jsonPath("$.aiUsed").value(false)).andExpect(jsonPath("$.questions.length()").value(3));
        call("GET", "/api/problem-bank/suggestions", t, null).andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void mockTestUsesOnlyTheUsersOwnPattern() throws Exception {
        String a = register();
        String b = register();
        long id = read(call("POST", "/api/exam-patterns", a,
                "{\"companyCode\":\"INFOSYS_DSE\",\"name\":\"Mine\",\"sections\":[{\"name\":\"Apt\",\"kind\":\"APTITUDE\",\"questions\":3,\"minutes\":5}]}")
                .andExpect(status().isCreated())).get("id").asLong();
        call("POST", "/api/mock-tests?patternId=" + id, b, null).andExpect(status().isNotFound());
        JsonNode mt = read(call("POST", "/api/mock-tests?patternId=" + id, a, null).andExpect(status().isCreated()));
        assertThat(mt.toString()).doesNotContain("explanation");
        call("POST", "/api/mock-tests/" + mt.get("id").asLong() + "/submit", a, "{\"answers\":{}}").andExpect(jsonPath("$.status").value("SUBMITTED"));
    }
}
