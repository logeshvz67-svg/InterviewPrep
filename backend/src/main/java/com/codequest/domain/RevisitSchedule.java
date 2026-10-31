package com.codequest.domain;

import java.time.LocalDate;
import java.util.List;

/** Re-solve a problem from scratch after 3, 7 and 14 days (counted from the day you solved it). */
public final class RevisitSchedule {
    public static final List<Integer> OFFSETS_DAYS = List.of(3, 7, 14);

    private RevisitSchedule() {}

    /** Due date for round 1..3. */
    public static LocalDate dueDate(LocalDate solvedOn, int round) {
        if (round < 1 || round > OFFSETS_DAYS.size()) throw new IllegalArgumentException("round must be 1.." + OFFSETS_DAYS.size());
        return solvedOn.plusDays(OFFSETS_DAYS.get(round - 1));
    }

    public static List<LocalDate> allDueDates(LocalDate solvedOn) {
        return OFFSETS_DAYS.stream().map(solvedOn::plusDays).toList();
    }
}
