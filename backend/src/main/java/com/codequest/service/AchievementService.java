package com.codequest.service;

import com.codequest.entity.Achievement;
import com.codequest.entity.UserAchievement;
import com.codequest.repository.AchievementRepository;
import com.codequest.repository.UserAchievementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/** Badges. Checked after XP-earning actions; each unlocks once. Compares you only with your past self. */
@Service
public class AchievementService {
    private final AchievementRepository achievements;
    private final UserAchievementRepository mine;
    private final StatsService stats;
    private final TimeService time;

    public AchievementService(AchievementRepository achievements, UserAchievementRepository mine, StatsService stats, TimeService time) {
        this.achievements = achievements;
        this.mine = mine;
        this.stats = stats;
        this.time = time;
    }

    public record BadgeView(String code, String name, String description, String icon, Instant unlockedAt) {}

    @Transactional(readOnly = true)
    public List<BadgeView> list(Long userId) {
        Map<Long, Instant> unlocked = mine.findByUserId(userId).stream()
                .collect(Collectors.toMap(UserAchievement::getAchievementId, UserAchievement::getUnlockedAt));
        return achievements.findAllByOrderBySortOrder().stream()
                .map(a -> new BadgeView(a.getCode(), a.getName(), a.getDescription(), a.getIcon(), unlocked.get(a.getId()))).toList();
    }

    /** Check every badge not yet unlocked. readinessPercent may be null (only the dashboard knows it). */
    @Transactional
    public List<String> evaluate(Long userId, Double readinessPercent, double readyThreshold) {
        Set<Long> have = mine.findByUserId(userId).stream().map(UserAchievement::getAchievementId).collect(Collectors.toSet());
        List<String> unlockedNow = new ArrayList<>();
        for (Achievement a : achievements.findAllByOrderBySortOrder()) {
            if (have.contains(a.getId())) continue;
            boolean earned = switch (a.getCode()) {
                case "FIRST_BLOOD" -> stats.problems(userId) >= 1;
                case "PATTERN_HUNTER" -> stats.distinctPatterns(userId) >= 8;
                case "RECALL_MASTER" -> stats.longestRecallRun(userId) >= 30;
                case "VOICE_OF_CONFIDENCE" -> stats.speakingSessions(userId) >= 20;
                case "BUG_SLAYER" -> stats.resolvedMistakes(userId) >= 10;
                case "SPRING_INITIATOR" -> stats.springWins(userId) >= 1;
                case "BOSS_SLAYER" -> stats.mockInterviews(userId) >= 1;
                case "COMEBACK_KID" -> stats.comebacks(userId) >= 1;
                case "JAVA_SCHOLAR" -> stats.anyJavaSectionComplete(userId);
                case "INFOSYS_READY" -> readinessPercent != null && readinessPercent >= readyThreshold;
                case "SCHOLAR_10" -> stats.lessonsCompleted(userId) >= 10;
                case "PROVEN" -> stats.proveItPassed(userId) >= 1;
                case "STREAK_7" -> stats.longestStreak(userId) >= 7;
                case "FOCUS_10" -> stats.focusSessions(userId) >= 10;
                default -> false;
            };
            if (earned) {
                mine.save(new UserAchievement(userId, a.getId(), time.now()));
                unlockedNow.add(a.getCode());
            }
        }
        return unlockedNow;
    }
}
