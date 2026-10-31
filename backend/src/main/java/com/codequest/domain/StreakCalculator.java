package com.codequest.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure streak math (no database, no clock) so it is easy to unit-test.
 *
 * Rules (kind by design, no heavy penalties):
 *  - Activity on the next day: streak + 1.
 *  - Missed days are covered by freezes if you have enough (each missed day uses one freeze).
 *  - Not enough freezes: the streak restarts at 1. If you were away 3+ days, Comeback Mode turns on.
 *  - Every 7 days of streak earns a freeze, max 2 saved.
 *  - "today" is always the user's LOCAL date (their timezone), so midnight in India
 *    is midnight for the streak, not midnight UTC.
 */
public final class StreakCalculator {
    public static final int MAX_FREEZES = 2;
    public static final int DAYS_PER_FREEZE = 7;
    public static final int COMEBACK_AFTER_DAYS_AWAY = 3;

    private StreakCalculator() {}

    public record State(int current, int longest, LocalDate lastActive, int freezes) {}

    public record Result(State state, boolean changed, List<LocalDate> frozenDays,
                         boolean freezeEarned, boolean comebackTriggered, boolean streakRestarted) {}

    public static Result onActivity(State s, LocalDate today) {
        // Same day (or a clock that went backwards): nothing changes.
        if (s.lastActive() != null && !today.isAfter(s.lastActive())) {
            return new Result(s, false, List.of(), false, false, false);
        }

        int current = s.current();
        int freezes = s.freezes();
        List<LocalDate> frozen = new ArrayList<>();
        boolean comeback = false;
        boolean restarted = false;

        if (s.lastActive() == null) {
            current = 1;
        } else {
            long missed = ChronoUnit.DAYS.between(s.lastActive(), today) - 1;
            if (missed == 0) {
                current += 1;
            } else if (missed <= freezes) {
                for (int i = 1; i <= missed; i++) frozen.add(s.lastActive().plusDays(i));
                freezes -= (int) missed;
                current += 1; // frozen days protect the streak but don't add to it
            } else {
                restarted = current > 0;
                comeback = missed >= COMEBACK_AFTER_DAYS_AWAY;
                current = 1;
            }
        }

        boolean earned = false;
        if (current > 0 && current % DAYS_PER_FREEZE == 0 && freezes < MAX_FREEZES) {
            freezes++;
            earned = true;
        }

        State next = new State(current, Math.max(s.longest(), current), today, freezes);
        return new Result(next, true, frozen, earned, comeback, restarted);
    }

    public enum DisplayStatus { NOT_STARTED, DONE_TODAY, KEEP_IT_GOING, FREEZE_WILL_COVER, PAUSED }

    /** What to show on the dashboard WITHOUT changing anything (reading must never break a streak). */
    public static DisplayStatus status(State s, LocalDate today) {
        if (s.lastActive() == null) return DisplayStatus.NOT_STARTED;
        long missed = ChronoUnit.DAYS.between(s.lastActive(), today) - 1;
        if (missed < 0) return DisplayStatus.DONE_TODAY;
        if (missed == 0) return DisplayStatus.KEEP_IT_GOING;
        if (missed <= s.freezes()) return DisplayStatus.FREEZE_WILL_COVER;
        return DisplayStatus.PAUSED;
    }

    public static long daysAway(State s, LocalDate today) {
        if (s.lastActive() == null) return 0;
        return Math.max(0, ChronoUnit.DAYS.between(s.lastActive(), today));
    }
}
