package com.codequest.dto;

import com.codequest.domain.ReadinessCalculator;

import java.math.BigDecimal;
import java.util.List;

public final class DashboardDtos {
    private DashboardDtos() {}

    public record RadarPoint(String area, Integer selfRated, Integer measured) {}

    public record ReadinessView(String companyCode, BigDecimal score, BigDecimal trackedWeightPercent,
                                List<ReadinessCalculator.ComponentScore> components, String improveNext, String improveNextTip) {}

    public record CommandCenter(
            String characterName, String avatar, String whyStatement,
            GameDtos.LevelView level, GameDtos.StreakView streak,
            List<GameDtos.QuestView> todayQuests,
            int recallQueueCount, Long daysToTest,
            List<RadarPoint> radar, ReadinessView readiness,
            FocusDtos.FocusView activeFocus, FocusDtos.NextAction nextAction,
            int todayXp, int currentWeek, String currentWeekTitle, String welcomeMessage) {}
}
