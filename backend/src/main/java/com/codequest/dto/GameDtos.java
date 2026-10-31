package com.codequest.dto;

import com.codequest.domain.Rank;
import com.codequest.domain.StreakCalculator.DisplayStatus;
import com.codequest.domain.XpAction;

import java.time.Instant;
import java.time.LocalDate;

public final class GameDtos {
    private GameDtos() {}

    public record LevelView(int totalXp, int level, int levelStartXp, int nextLevelXp, int levelProgressPercent,
                            Rank rank, Rank nextRank, Integer nextRankXp, int gold) {}

    public record StreakView(int current, int longest, int freezes, int maxFreezes, LocalDate lastActiveDate,
                             DisplayStatus status, boolean comebackPending, long daysAway) {}

    public record XpEventView(XpAction action, int amount, int gold, String note, LocalDate localDate, Instant createdAt) {}

    /** What an action earned. awarded=false means "already rewarded" or "daily cap reached" (honest, no error). */
    public record XpAward(boolean awarded, int xp, int gold, String reason) {
        public static XpAward none(String reason) { return new XpAward(false, 0, 0, reason); }
    }

    public record QuestView(Long id, String code, String title, String description, int progress, int target,
                            int goldReward, boolean completed) {}
}
