package com.codequest.domain;

import com.codequest.domain.Sm2Scheduler.Grade;
import com.codequest.domain.Sm2Scheduler.State;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class Sm2SchedulerTest {
    @Test
    void goodAnswersClimbTheLadder1_3_7_14_30() {
        State s = State.fresh();
        int[] expected = {1, 3, 7, 14, 30};
        for (int e : expected) {
            s = Sm2Scheduler.review(s, Grade.GOOD);
            assertThat(s.intervalDays()).isEqualTo(e);
        }
        // After the ladder: interval grows by the ease factor.
        State after = Sm2Scheduler.review(s, Grade.GOOD);
        assertThat(after.intervalDays()).isGreaterThan(30);
    }

    @Test
    void againResetsToOneDayAndCountsALapse() {
        State s = new State(new BigDecimal("2.50"), 14, 4, 0);
        State r = Sm2Scheduler.review(s, Grade.AGAIN);
        assertThat(r.intervalDays()).isEqualTo(1);
        assertThat(r.repetitions()).isZero();
        assertThat(r.lapses()).isEqualTo(1);
        assertThat(r.ease()).isLessThan(s.ease());
    }

    @Test
    void easeNeverDropsBelowMinimum() {
        State s = State.fresh();
        for (int i = 0; i < 20; i++) s = Sm2Scheduler.review(s, Grade.AGAIN);
        assertThat(s.ease()).isEqualByComparingTo(Sm2Scheduler.MIN_EASE);
    }

    @Test
    void hardIsShorterAndEasyIsLongerThanGood() {
        State s = Sm2Scheduler.review(State.fresh(), Grade.GOOD); // 1 day
        int good = Sm2Scheduler.review(s, Grade.GOOD).intervalDays();
        int hard = Sm2Scheduler.review(s, Grade.HARD).intervalDays();
        int easy = Sm2Scheduler.review(s, Grade.EASY).intervalDays();
        assertThat(hard).isLessThan(good);
        assertThat(easy).isGreaterThan(good);
    }

    @Test
    void intervalIsCapped() {
        State s = new State(new BigDecimal("3.00"), 300, 10, 0);
        assertThat(Sm2Scheduler.review(s, Grade.EASY).intervalDays()).isEqualTo(Sm2Scheduler.MAX_INTERVAL);
    }
}
