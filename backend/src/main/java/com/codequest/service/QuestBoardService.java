package com.codequest.service;

import com.codequest.dto.GameDtos.QuestView;
import com.codequest.entity.Quest;
import com.codequest.entity.UserQuest;
import com.codequest.repository.ProfileRepository;
import com.codequest.repository.QuestRepository;
import com.codequest.repository.UserQuestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * Quest Board: daily (max 3, from QuestService), weekly and story quests.
 * Weekly/story progress is MEASURED from your data each time you look (no timers needed);
 * when a quest reaches its target, the gold is paid exactly once.
 */
@Service
public class QuestBoardService {
    static final LocalDate STORY_DATE = LocalDate.of(2000, 1, 1);

    private final QuestRepository quests;
    private final UserQuestRepository userQuests;
    private final QuestService daily;
    private final ProfileRepository profiles;
    private final StatsService stats;
    private final TimeService time;

    public QuestBoardService(QuestRepository quests, UserQuestRepository userQuests, QuestService daily,
                             ProfileRepository profiles, StatsService stats, TimeService time) {
        this.quests = quests;
        this.userQuests = userQuests;
        this.daily = daily;
        this.profiles = profiles;
        this.stats = stats;
        this.time = time;
    }

    public record Board(List<QuestView> daily, List<QuestView> weekly, List<QuestView> story, LocalDate weekStart) {}

    @Transactional
    public Board board(Long userId) {
        LocalDate today = time.today(userId);
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Instant weekStartInstant = monday.atStartOfDay(time.zoneOf(userId)).toInstant();
        List<QuestView> weekly = quests.findByKindOrderById("WEEKLY").stream()
                .map(q -> computed(userId, q, monday, metric(userId, q.getMetric(), monday, weekStartInstant))).toList();
        List<QuestView> story = quests.findByKindOrderById("STORY").stream()
                .map(q -> computed(userId, q, STORY_DATE, metric(userId, q.getMetric(), monday, weekStartInstant))).toList();
        return new Board(daily.today(userId), weekly, story, monday);
    }

    private QuestView computed(Long userId, Quest q, LocalDate forDate, long value) {
        UserQuest uq = userQuests.findByUserIdAndQuestIdAndForDate(userId, q.getId(), forDate).orElse(null);
        boolean done = uq != null && uq.isCompleted();
        if (!done && value >= q.getTarget()) {
            if (uq == null) {
                uq = new UserQuest();
                uq.setUserId(userId);
                uq.setQuest(q);
                uq.setForDate(forDate);
            }
            uq.setProgress((int) value);
            uq.setCompletedAt(time.now());
            userQuests.save(uq);
            profiles.findByUserId(userId).ifPresent(p -> p.setGold(p.getGold() + q.getGoldReward()));
            done = true;
        }
        return new QuestView(uq == null ? null : uq.getId(), q.getCode(), q.getTitle(), q.getDescription(),
                (int) Math.min(value, q.getTarget()), q.getTarget(), q.getGoldReward(), done);
    }

    private long metric(Long u, String metric, LocalDate monday, Instant weekStart) {
        if (metric == null) return 0;
        return switch (metric) {
            case "LESSONS_COMPLETED" -> stats.lessonsCompletedSince(u, weekStart);
            case "RECALL_DAYS" -> stats.recallDaysSince(u, monday);
            case "PROBLEMS_LOGGED" -> stats.problemsSince(u, weekStart);
            case "FOCUS_SESSIONS" -> stats.focusSessionsSince(u, monday);
            case "SPEAKING_SESSIONS" -> stats.speakingSince(u, monday);
            case "LESSONS_COMPLETED_TOTAL" -> stats.lessonsCompleted(u);
            case "SECTION_A_DONE" -> stats.sectionLessonsCompleted(u, "A");
            case "PROVE_IT_PASSED" -> stats.proveItPassed(u);
            case "PROBLEMS_LOGGED_TOTAL" -> stats.problems(u);
            case "MOCK_TESTS_TOTAL" -> stats.mockTests(u);
            case "MOCK_INTERVIEWS_TOTAL" -> stats.mockInterviews(u);
            default -> 0;
        };
    }
}
