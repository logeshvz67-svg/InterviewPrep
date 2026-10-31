package com.codequest.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end through HTTP -> security filter -> controller -> service -> real Postgres. */
@AutoConfigureMockMvc
class ApiFlowIntegrationTest extends IntegrationTestBase {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired MutableClock clock;

    private static final String ONBOARD = """
            {"characterName":"Hero","avatar":"knight","primaryCompany":"INFOSYS_SP","laterCompanies":["TCS"],
             "timezone":"Asia/Kolkata","dailyMinutes":60,"whyStatement":"Better job",
             "selfAssessment":{"JAVA":2,"DSA":2,"SQL":2,"SPRING":1,"COMMUNICATION":1}}""";

    private static final String PROBLEM = """
            {"title":"Two Sum","platform":"LEETCODE","difficulty":"EASY","patternCodes":["HASHING"],
             "timeTakenMinutes":20,"attempts":1,"hintLevel":"NONE","attemptedBeforeHint":true,"confidence":4,
             "approach":"Use a HashMap of value to index and look up target minus current."}""";

    String register() throws Exception {
        String email = "u" + UUID.randomUUID() + "@test.com";
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\",\"displayName\":\"Hero\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("accessToken").asText();
    }

    ResultActions call(String method, String url, String token, String body) throws Exception {
        var req = request(org.springframework.http.HttpMethod.valueOf(method), url).header("Authorization", "Bearer " + token);
        if (body != null) req.contentType(MediaType.APPLICATION_JSON).content(body);
        return mvc.perform(req);
    }

    JsonNode read(ResultActions r) throws Exception {
        return json.readTree(r.andReturn().getResponse().getContentAsString());
    }

    @Test
    void healthIsPublicAndApiNeedsLogin() throws Exception {
        mvc.perform(get("/health")).andExpect(status().isOk());
        mvc.perform(get("/api/command-center")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/command-center").header("Authorization", "Bearer not-a-real-token")).andExpect(status().isUnauthorized());
    }

    @Test
    void validationErrorsAre400WithFieldMessages() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"bad\",\"password\":\"123\",\"displayName\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void onboardingThenLogProblemAwardsServerSideXpAndRevisits() throws Exception {
        clock.set(Instant.parse("2026-11-05T06:00:00Z"));
        String t = register();
        call("PUT", "/api/onboarding", t, ONBOARD).andExpect(status().isOk()).andExpect(jsonPath("$.onboardingComplete").value(true));
        call("GET", "/api/roadmap", t, null).andExpect(status().isOk()).andExpect(jsonPath("$.totalWeeks").value(16));

        JsonNode saved = read(call("POST", "/api/problems", t, PROBLEM).andExpect(status().isCreated()));
        assertThat(saved.at("/approachXp/xp").asInt()).isEqualTo(15);
        assertThat(saved.at("/attemptXp/xp").asInt()).isEqualTo(10);
        assertThat(saved.get("revisitDates")).hasSize(3);

        JsonNode cc = read(call("GET", "/api/command-center", t, null).andExpect(status().isOk()));
        assertThat(cc.at("/level/totalXp").asInt()).isEqualTo(25);
        assertThat(cc.at("/streak/current").asInt()).isEqualTo(1);
        assertThat(cc.get("todayQuests").size()).isLessThanOrEqualTo(3);
    }

    @Test
    void usersCannotSeeEachOthersData() throws Exception {
        String alice = register();
        String bob = register();
        long id = read(call("POST", "/api/problems", alice, PROBLEM)).at("/problem/id").asLong();
        call("GET", "/api/problems/" + id, alice, null).andExpect(status().isOk());
        call("GET", "/api/problems/" + id, bob, null).andExpect(status().isNotFound());
        call("DELETE", "/api/problems/" + id, bob, null).andExpect(status().isNotFound());
        assertThat(read(call("GET", "/api/problems", bob, null)).get("totalItems").asInt()).isZero();
    }

    @Test
    void masteredCannotBeSetByClicking() throws Exception {
        String t = register();
        call("PUT", "/api/curriculum/java/1/status", t, "{\"status\":\"MASTERED\"}").andExpect(status().isBadRequest());
        call("PUT", "/api/curriculum/java/1/status", t, "{\"status\":\"UNDERSTOOD\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDERSTOOD"));
    }

    @Test
    void streakFollowsIndianMidnightNotUtcMidnight() throws Exception {
        String t = register();
        call("PUT", "/api/onboarding", t, ONBOARD).andExpect(status().isOk());
        String mistake = "{\"categoryCode\":\"EDGE_CASE\",\"whatHappened\":\"Forgot empty array\",\"lessonLearned\":\"Test empty input\"}";

        clock.set(Instant.parse("2026-11-05T18:00:00Z")); // 23:30 IST, Nov 5
        call("POST", "/api/mistakes", t, mistake).andExpect(status().isCreated());
        clock.set(Instant.parse("2026-11-05T18:31:00Z")); // 00:01 IST, Nov 6 (still Nov 5 in UTC!)
        call("POST", "/api/mistakes", t, mistake).andExpect(status().isCreated());

        call("GET", "/api/streak", t, null).andExpect(jsonPath("$.current").value(2))
                .andExpect(jsonPath("$.lastActiveDate").value("2026-11-06"));
    }

    @Test
    void focusSessionCannotBeCompletedEarlyButCanAfterTime() throws Exception {
        clock.set(Instant.parse("2026-11-07T04:00:00Z"));
        String t = register();
        long id = read(call("POST", "/api/focus/start", t, "{\"mode\":\"POMODORO\",\"topic\":\"OOP\",\"plannedMinutes\":25}")
                .andExpect(status().isCreated())).get("id").asLong();
        call("POST", "/api/focus/start", t, "{\"mode\":\"POMODORO\",\"topic\":\"OOP\",\"plannedMinutes\":25}").andExpect(status().isConflict());
        call("POST", "/api/focus/" + id + "/complete", t, null).andExpect(status().isBadRequest());

        clock.set(Instant.parse("2026-11-07T04:25:10Z"));
        call("POST", "/api/focus/" + id + "/complete", t, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.xp.xp").value(10))
                .andExpect(jsonPath("$.session.status").value("COMPLETED"));
    }

    @Test
    void refreshTokenRotates() throws Exception {
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"rot" + UUID.randomUUID() + "@test.com\",\"password\":\"password123\",\"displayName\":\"R\"}"))
                .andReturn().getResponse().getContentAsString();
        String refresh = json.readTree(body).get("refreshToken").asText();
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isOk());
        // Old token is now revoked: reusing it fails.
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isUnauthorized());
    }
}
