package com.codequest.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RevisitScheduleTest {
    @Test
    void dueAfter3_7_14Days() {
        LocalDate d = LocalDate.of(2026, 12, 30);
        assertThat(RevisitSchedule.allDueDates(d)).containsExactly(
                LocalDate.of(2027, 1, 2), LocalDate.of(2027, 1, 6), LocalDate.of(2027, 1, 13));
    }

    @Test
    void invalidRound() {
        assertThatThrownBy(() -> RevisitSchedule.dueDate(LocalDate.now(), 4)).isInstanceOf(IllegalArgumentException.class);
    }
}
