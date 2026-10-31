package com.codequest.service;

import com.codequest.entity.User;
import com.codequest.exception.ApiException;
import com.codequest.repository.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** "Your data is yours": full JSON export and permanent deletion. */
@Service
public class AccountDataService {
    /** Every user-owned table. Deleting the user cascades to all of them (ON DELETE CASCADE). */
    static final List<String> USER_TABLES = List.of(
            "profiles", "roadmaps", "xp_events", "streaks", "activity_days", "java_topic_progress", "user_quests",
            "problems_solved", "problem_revisits", "mistakes", "focus_sessions", "distraction_log", "daily_commitments",
            "lesson_progress", "quiz_attempts", "flashcards", "flashcard_reviews", "user_achievements",
            "ai_conversations", "ai_messages", "ai_usage", "speaking_sessions", "mock_interviews",
            "interview_practice", "aptitude_attempts", "exam_pattern_config", "mock_tests", "mock_test_attempts",
            "interview_diary", "spring_topic_progress", "spring_projects", "project_milestones", "build_logs", "reports");

    private final JdbcTemplate jdbc;
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AccountDataService(JdbcTemplate jdbc, UserRepository users, PasswordEncoder encoder) {
        this.jdbc = jdbc;
        this.users = users;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> exportAll(Long userId) {
        Map<String, Object> out = new LinkedHashMap<>();
        User u = users.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
        out.put("exportedFor", Map.of("email", u.getEmail(), "displayName", u.getDisplayName()));
        for (String t : USER_TABLES) {
            // Table names come from the fixed list above (never from user input), so this is safe.
            out.put(t, jdbc.queryForList("select * from " + t + " where user_id = ? order by id", userId));
        }
        return out;
    }

    /** Permanently deletes the account and everything in it. Requires the password again. */
    @Transactional
    public void deleteAccount(Long userId, String password) {
        User u = users.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
        if (!encoder.matches(password, u.getPasswordHash())) throw ApiException.forbidden("Password is wrong");
        users.delete(u); // ON DELETE CASCADE removes every user-owned row
    }
}
