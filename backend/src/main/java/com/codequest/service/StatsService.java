package com.codequest.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Plain SQL counts used by achievements, weekly/story quests and the Hall of Records.
 * WHY JdbcTemplate here: these are read-only aggregate questions ("how many X since Y?").
 * Writing them as SQL is shorter and clearer than loading entities just to count them.
 * Every query filters by user_id.
 */
@Service
public class StatsService {
    private final JdbcTemplate jdbc;

    public StatsService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long count(String sql, Object... args) {
        Long n = jdbc.queryForObject(sql, Long.class, args);
        return n == null ? 0 : n;
    }

    public long lessonsCompleted(Long u) { return count("select count(*) from lesson_progress where user_id=? and status='COMPLETED'", u); }
    public long lessonsCompletedSince(Long u, Instant since) { return count("select count(*) from lesson_progress where user_id=? and completed_at>=?", u, Timestamp.from(since)); }
    public long sectionLessonsCompleted(Long u, String section) {
        return count("select count(*) from lesson_progress p join lessons l on l.id=p.lesson_id where p.user_id=? and p.status='COMPLETED' and l.world_code='JAVA_CORE' and l.section=?", u, section);
    }
    public long sectionLessonsTotal(String section) { return count("select count(*) from java_topics where section=?", section); }
    public long proveItPassed(Long u) { return count("select count(*) from quiz_attempts where user_id=? and kind='PROVE_IT' and passed", u); }
    public long problems(Long u) { return count("select count(*) from problems_solved where user_id=?", u); }
    public long problemsSince(Long u, Instant since) { return count("select count(*) from problems_solved where user_id=? and created_at>=?", u, Timestamp.from(since)); }
    public long distinctPatterns(Long u) { return count("select count(distinct pp.pattern_id) from problem_patterns pp join problems_solved p on p.id=pp.problem_id where p.user_id=?", u); }
    public long resolvedMistakes(Long u) { return count("select count(*) from mistakes where user_id=? and resolved", u); }
    public long recallDaysSince(Long u, LocalDate from) { return count("select count(distinct local_date) from flashcard_reviews where user_id=? and local_date>=?", u, Date.valueOf(from)); }
    public long focusSessions(Long u) { return count("select count(*) from focus_sessions where user_id=? and status='COMPLETED'", u); }
    public long focusSessionsSince(Long u, LocalDate from) { return count("select count(*) from focus_sessions where user_id=? and status='COMPLETED' and local_date>=?", u, Date.valueOf(from)); }
    public long speakingSessions(Long u) { return count("select count(*) from speaking_sessions where user_id=?", u); }
    public long speakingSince(Long u, LocalDate from) { return count("select count(*) from speaking_sessions where user_id=? and local_date>=?", u, Date.valueOf(from)); }
    public long mockTests(Long u) { return count("select count(*) from mock_tests where user_id=? and status='SUBMITTED'", u); }
    public long mockInterviews(Long u) { return count("select count(*) from mock_interviews where user_id=? and status='COMPLETED'", u); }
    public long springWins(Long u) {
        return count("select (select count(*) from project_milestones where user_id=? and done_at is not null) + (select count(*) from spring_topic_progress where user_id=? and status in ('UNDERSTOOD','CAN_EXPLAIN'))", u, u);
    }
    public long longestStreak(Long u) { return count("select coalesce(max(longest_streak),0) from streaks where user_id=?", u); }
    public long comebacks(Long u) { return count("select coalesce(max(comeback_count),0) from streaks where user_id=?", u); }

    /** Longest run of consecutive days with at least one flashcard review. */
    public long longestRecallRun(Long u) {
        List<LocalDate> days = jdbc.queryForList("select distinct local_date from flashcard_reviews where user_id=? order by 1", LocalDate.class, u);
        long best = 0, run = 0;
        LocalDate prev = null;
        for (LocalDate d : days) {
            run = prev != null && prev.plusDays(1).equals(d) ? run + 1 : 1;
            best = Math.max(best, run);
            prev = d;
        }
        return best;
    }

    /** True if every Java section that has lessons is fully completed in at least one section. */
    public boolean anyJavaSectionComplete(Long u) {
        Long n = jdbc.queryForObject("""
                select count(*) from (
                  select l.section from lessons l
                  left join lesson_progress p on p.lesson_id=l.id and p.user_id=? and p.status='COMPLETED'
                  where l.world_code='JAVA_CORE'
                  group by l.section
                  having count(*) = (select count(*) from java_topics t where t.section=l.section)
                     and count(p.id) = count(*)) s""", Long.class, u);
        return n != null && n > 0;
    }

    public JdbcTemplate jdbc() { return jdbc; }
}
