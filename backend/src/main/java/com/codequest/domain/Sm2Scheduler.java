package com.codequest.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Spaced repetition, SM-2 style, with the learning ladder 1 -> 3 -> 7 -> 14 -> 30 days.
 *
 * Grades (4 buttons): AGAIN = 0, HARD = 3, GOOD = 4, EASY = 5.
 *  - AGAIN: you forgot. Back to 1 day, ease goes down, counts as a lapse.
 *  - HARD / GOOD / EASY: move up the ladder; after 30 days, multiply by the ease factor.
 * Ease = how easy this card is for YOU (starts 2.5, never below 1.3).
 */
public final class Sm2Scheduler {
    public static final int[] LADDER = {1, 3, 7, 14, 30};
    public static final BigDecimal MIN_EASE = new BigDecimal("1.30");
    public static final int MAX_INTERVAL = 365;

    private Sm2Scheduler() {}

    public enum Grade {
        AGAIN(0), HARD(3), GOOD(4), EASY(5);
        public final int value;
        Grade(int v) { value = v; }
    }

    public record State(BigDecimal ease, int intervalDays, int repetitions, int lapses) {
        public static State fresh() { return new State(new BigDecimal("2.50"), 0, 0, 0); }
    }

    public static State review(State s, Grade g) {
        int q = g.value;
        // Standard SM-2 ease update.
        double delta = 0.1 - (5 - q) * (0.08 + (5 - q) * 0.02);
        BigDecimal ease = s.ease().add(BigDecimal.valueOf(delta)).setScale(2, RoundingMode.HALF_UP).max(MIN_EASE);

        if (g == Grade.AGAIN) {
            return new State(ease, 1, 0, s.lapses() + 1);
        }
        int reps = s.repetitions() + 1;
        int interval;
        if (reps <= LADDER.length) {
            int step = LADDER[reps - 1];
            interval = switch (g) {
                case HARD -> Math.max(1, Math.round(step * 0.6f));
                case EASY -> reps < LADDER.length ? LADDER[reps] : Math.round(step * 1.3f); // skip a rung
                default -> step;
            };
            if (g == Grade.EASY && reps < LADDER.length) reps++; // easy really skips a rung
        } else {
            double factor = switch (g) {
                case HARD -> 1.2;
                case EASY -> ease.doubleValue() * 1.3;
                default -> ease.doubleValue();
            };
            interval = (int) Math.round(Math.max(1, s.intervalDays()) * factor);
        }
        return new State(ease, Math.min(MAX_INTERVAL, Math.max(1, interval)), reps, s.lapses());
    }
}
