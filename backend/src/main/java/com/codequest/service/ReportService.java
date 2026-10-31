package com.codequest.service;

import com.codequest.ai.AiService;
import com.codequest.ai.ChatClient.Message;
import com.codequest.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;

/** Weekly / monthly reports (numbers always; an AI summary only if available). */
@Service
public class ReportService {
    private final JdbcTemplate jdbc;
    private final AiService ai;
    private final TimeService time;
    private final ObjectMapper json;

    public ReportService(JdbcTemplate jdbc, AiService ai, TimeService time, ObjectMapper json) {
        this.jdbc = jdbc;
        this.ai = ai;
        this.time = time;
        this.json = json;
    }

    @Transactional
    public Map<String, Object> generate(Long u, String kind, boolean useAi) {
        LocalDate today = time.today(u);
        LocalDate start, end;
        if ("MONTHLY".equals(kind)) {
            start = today.withDayOfMonth(1).minusMonths(today.getDayOfMonth() < 7 ? 1 : 0);
            end = start.plusMonths(1).minusDays(1);
        } else if ("WEEKLY".equals(kind)) {
            // The current week so far (Monday..today).
            start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            end = start.plusDays(6);
        } else throw ApiException.badRequest("kind must be WEEKLY or MONTHLY");
        Date s = Date.valueOf(start), e = Date.valueOf(end);
        ObjectNode data = json.createObjectNode();
        data.setAll((ObjectNode) json.valueToTree(jdbc.queryForMap("""
                select (select count(*) from activity_days where user_id=? and local_date between ? and ? and not via_freeze) as days_studied,
                       (select coalesce(sum(amount),0) from xp_events where user_id=? and local_date between ? and ?) as xp,
                       (select coalesce(sum(actual_minutes),0) from focus_sessions where user_id=? and status='COMPLETED' and local_date between ? and ?) as focus_minutes,
                       (select count(*) from problems_solved where user_id=? and solved_on between ? and ?) as problems,
                       (select count(*) from problems_solved where user_id=? and solved_on between ? and ? and hint_level='NONE') as problems_no_hint,
                       (select count(*) from mistakes where user_id=? and created_at::date between ? and ?) as mistakes,
                       (select count(*) from lesson_progress where user_id=? and completed_at::date between ? and ?) as lessons,
                       (select count(*) from flashcard_reviews where user_id=? and local_date between ? and ?) as reviews,
                       (select count(*) from speaking_sessions where user_id=? and local_date between ? and ?) as speaking,
                       (select round(avg(score_percent),1) from mock_test_attempts where user_id=? and submitted_at::date between ? and ?) as mock_avg
                """, u, s, e, u, s, e, u, s, e, u, s, e, u, s, e, u, s, e, u, s, e, u, s, e, u, s, e, u, s, e)));
        data.set("topMistakes", json.valueToTree(jdbc.queryForList("""
                select c.name, count(*) n from mistakes m join mistake_categories c on c.code=m.category_code
                where m.user_id=? and m.created_at::date between ? and ? group by c.name order by n desc limit 3""", u, s, e)));
        data.set("distractions", json.valueToTree(jdbc.queryForList(
                "select category, count(*) n from distraction_log where user_id=? and local_date between ? and ? group by category order by n desc limit 3", u, s, e)));
        String summary = useAi ? aiSummary(u, kind, data) : null;
        if (summary == null) summary = templateSummary(data);
        jdbc.update("""
                insert into reports (user_id, kind, company_code, period_start, period_end, data, ai_summary) values (?,?, 'ALL', ?,?, ?::jsonb, ?)
                on conflict (user_id, kind, company_code, period_start) do update set data = excluded.data, ai_summary = excluded.ai_summary, updated_at = now()""",
                u, kind, s, e, data.toString(), summary);
        return Map.of("kind", kind, "periodStart", start, "periodEnd", end, "data", data, "summary", summary);
    }

    private String aiSummary(Long u, String kind, JsonNode data) {
        var r = ai.ask(u, "report", MentorService.SYSTEM + "\nWrite a short, kind " + kind.toLowerCase()
                        + " progress report (max 150 words): 2 wins, 1-2 things to improve, 1 concrete goal. Compare only with the learner's own past. No check question.",
                List.of(new Message("user", data.toString())), null);
        return r.ok() ? r.text() : null;
    }

    static String templateSummary(JsonNode d) {
        int days = d.path("days_studied").asInt();
        StringBuilder sb = new StringBuilder();
        sb.append("You studied on ").append(days).append(days == 1 ? " day" : " days").append(" and earned ").append(d.path("xp").asInt()).append(" XP. ");
        if (d.path("problems").asInt() > 0) sb.append("You logged ").append(d.path("problems").asInt()).append(" problems (")
                .append(d.path("problems_no_hint").asInt()).append(" without hints). ");
        if (d.path("lessons").asInt() > 0) sb.append("Lessons completed: ").append(d.path("lessons").asInt()).append(". ");
        if (d.path("reviews").asInt() > 0) sb.append("Flashcard reviews: ").append(d.path("reviews").asInt()).append(". ");
        JsonNode top = d.path("topMistakes");
        if (top.isArray() && !top.isEmpty()) sb.append("Most common mistake: ").append(top.get(0).path("name").asText()).append(" — check its tip in the Mistake Notebook. ");
        sb.append(days >= 4 ? "Great consistency. Keep it going!" : "Goal: show up on 4 days next time, even with the 20-minute Minimum Viable Day.");
        return sb.toString();
    }

    public List<Map<String, Object>> list(Long u) {
        return jdbc.queryForList("select kind, period_start, period_end, data, ai_summary, updated_at from reports where user_id=? and kind in ('WEEKLY','MONTHLY') order by period_start desc limit 30", u);
    }

    /** One readiness snapshot per day per company (for the readiness-over-time chart). */
    public void snapshotReadiness(Long u, String company, JsonNode data) {
        LocalDate today = time.today(u);
        jdbc.update("""
                insert into reports (user_id, kind, company_code, period_start, period_end, data) values (?, 'READINESS', ?, ?, ?, ?::jsonb)
                on conflict (user_id, kind, company_code, period_start) do update set data = excluded.data, updated_at = now()""",
                u, company, Date.valueOf(today), Date.valueOf(today), data.toString());
    }
}
