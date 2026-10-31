package com.codequest.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Hall of Records: your history as numbers and charts. Compares you only with your past self.
 * All queries are plain SQL aggregates filtered by user_id.
 */
@Service
public class RecordsService {
    private final JdbcTemplate jdbc;
    private final TimeService time;

    public RecordsService(StatsService stats, TimeService time) {
        this.jdbc = stats.jdbc();
        this.time = time;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> records(Long u) {
        LocalDate today = time.today(u);
        Date yearAgo = Date.valueOf(today.minusDays(364));
        Date ninetyAgo = Date.valueOf(today.minusDays(89));
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("today", today.toString());
        r.put("heatmap", jdbc.queryForList("""
                select d.local_date as day,
                       coalesce((select sum(amount) from xp_events x where x.user_id=? and x.local_date=d.local_date),0) as xp,
                       coalesce((select sum(actual_minutes) from focus_sessions f where f.user_id=? and f.status='COMPLETED' and f.local_date=d.local_date),0) as minutes,
                       d.via_freeze as frozen
                from activity_days d where d.user_id=? and d.local_date>=? order by d.local_date""", u, u, u, yearAgo));
        r.put("xpByDay", jdbc.queryForList("select local_date as day, sum(amount) as xp from xp_events where user_id=? and local_date>=? group by local_date order by local_date", u, ninetyAgo));
        r.put("totals", jdbc.queryForMap("""
                select (select coalesce(sum(amount),0) from xp_events where user_id=?) as xp,
                       (select count(*) from activity_days where user_id=? and not via_freeze) as days_studied,
                       (select coalesce(sum(actual_minutes),0) from focus_sessions where user_id=? and status='COMPLETED') as focus_minutes,
                       (select count(*) from lesson_progress where user_id=? and status='COMPLETED') as lessons,
                       (select count(*) from problems_solved where user_id=?) as problems,
                       (select count(*) from flashcard_reviews where user_id=?) as reviews,
                       (select count(*) from mistakes where user_id=?) as mistakes""", u, u, u, u, u, u, u));
        r.put("hoursByTopic", jdbc.queryForList("select topic, sum(actual_minutes) as minutes from focus_sessions where user_id=? and status='COMPLETED' group by topic order by 2 desc limit 15", u));
        r.put("problemsByPattern", jdbc.queryForList("""
                select pt.name as pattern, count(*) as count from problem_patterns pp
                join problems_solved p on p.id=pp.problem_id join patterns pt on pt.id=pp.pattern_id
                where p.user_id=? group by pt.name, pt.sort_order order by pt.sort_order""", u));
        r.put("problemsByDifficulty", jdbc.queryForList("select difficulty, count(*) as count from problems_solved where user_id=? group by difficulty", u));
        r.put("firstAttemptRate", jdbc.queryForMap("""
                select count(*) as total,
                       count(*) filter (where attempts=1 and hint_level='NONE') as first_try_no_hint
                from problems_solved where user_id=?""", u));
        r.put("hintTrend", jdbc.queryForList("""
                select date_trunc('week', solved_on)::date as week, count(*) as total,
                       count(*) filter (where hint_level <> 'NONE') as with_hint
                from problems_solved where user_id=? and solved_on >= ? group by 1 order by 1""", u, Date.valueOf(today.minusWeeks(12))));
        r.put("recallByTopic", jdbc.queryForList("""
                select coalesce(f.topic,'Other') as topic, count(*) as total, count(*) filter (where r.grade >= 3) as remembered
                from flashcard_reviews r join flashcards f on f.id=r.flashcard_id where r.user_id=?
                group by 1 order by 2 desc limit 15""", u));
        r.put("quizBySection", jdbc.queryForList("""
                select l.world_code as world, l.section, round(avg(a.score_percent),1) as avg_percent, count(*) as attempts
                from quiz_attempts a join lessons l on l.id=a.lesson_id where a.user_id=? and a.kind='LESSON'
                group by l.world_code, l.section order by 1, 2""", u));
        r.put("speakingTrend", jdbc.queryForList("""
                select date_trunc('week', local_date)::date as week, round(avg(wpm)) as wpm, round(avg(filler_count),1) as fillers,
                       round(avg(overall_score),1) as score, count(*) as sessions
                from speaking_sessions where user_id=? group by 1 order by 1""", u));
        r.put("mockScores", jdbc.queryForList("""
                select a.submitted_at as at, t.title, a.score_percent as score from mock_test_attempts a join mock_tests t on t.id=a.mock_test_id
                where a.user_id=? order by a.submitted_at""", u));
        r.put("mockInterviewScores", jdbc.queryForList("select ended_at as at, round_type as round, overall_score as score from mock_interviews where user_id=? and status='COMPLETED' order by ended_at", u));
        r.put("readinessHistory", jdbc.queryForList("""
                select period_start as day, company_code as company, (data->>'score')::numeric as score
                from reports where user_id=? and kind='READINESS' order by period_start""", u));
        r.put("mistakesByCategory", jdbc.queryForList("""
                select c.name as category, count(*) as count from mistakes m join mistake_categories c on c.code=m.category_code
                where m.user_id=? group by c.name order by 2 desc""", u));
        return r;
    }

    public List<Map<String, Object>> problemsForCsv(Long u) {
        return jdbc.queryForList("""
                select p.solved_on, p.title, p.platform, p.difficulty,
                       (select string_agg(pt.name, '; ') from problem_patterns pp join patterns pt on pt.id=pp.pattern_id where pp.problem_id=p.id) as patterns,
                       p.time_taken_minutes, p.attempts, p.hint_level, p.confidence, p.link, p.approach
                from problems_solved p where p.user_id=? order by p.solved_on""", u);
    }
}
