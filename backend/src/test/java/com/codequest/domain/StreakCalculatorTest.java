package com.codequest.domain;

import com.codequest.domain.StreakCalculator.DisplayStatus;
import com.codequest.domain.StreakCalculator.State;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class StreakCalculatorTest {
    private static final LocalDate D = LocalDate.of(2026, 11, 10);

    @Test
    void firstActivityStartsStreakAtOne() {
        var r = StreakCalculator.onActivity(new State(0, 0, null, 0), D);
        assertThat(r.state().current()).isEqualTo(1);
        assertThat(r.state().lastActive()).isEqualTo(D);
    }

    @Test
    void sameDayTwiceChangesNothing() {
        var s = new State(3, 5, D, 1);
        var r = StreakCalculator.onActivity(s, D);
        assertThat(r.changed()).isFalse();
        assertThat(r.state()).isEqualTo(s);
    }

    @Test
    void nextDayAddsOne() {
        var r = StreakCalculator.onActivity(new State(3, 3, D, 0), D.plusDays(1));
        assertThat(r.state().current()).isEqualTo(4);
        assertThat(r.state().longest()).isEqualTo(4);
    }

    @Test
    void oneMissedDayUsesOneFreeze() {
        var r = StreakCalculator.onActivity(new State(5, 5, D, 2), D.plusDays(2));
        assertThat(r.state().current()).isEqualTo(6);
        assertThat(r.state().freezes()).isEqualTo(1);
        assertThat(r.frozenDays()).containsExactly(D.plusDays(1));
        assertThat(r.streakRestarted()).isFalse();
    }

    @Test
    void notEnoughFreezesRestartsWithoutComebackForShortBreak() {
        var r = StreakCalculator.onActivity(new State(5, 9, D, 1), D.plusDays(3)); // 2 missed, 1 freeze
        assertThat(r.state().current()).isEqualTo(1);
        assertThat(r.state().freezes()).isEqualTo(1); // freezes are kept, not burned for nothing
        assertThat(r.state().longest()).isEqualTo(9);
        assertThat(r.comebackTriggered()).isFalse();
        assertThat(r.streakRestarted()).isTrue();
    }

    @Test
    void threeOrMoreDaysAwayTriggersComebackMode() {
        var r = StreakCalculator.onActivity(new State(5, 5, D, 0), D.plusDays(4)); // 3 missed
        assertThat(r.comebackTriggered()).isTrue();
        assertThat(r.state().current()).isEqualTo(1);
    }

    @Test
    void everySeventhDayEarnsAFreezeUpToTwo() {
        var r = StreakCalculator.onActivity(new State(6, 6, D, 0), D.plusDays(1));
        assertThat(r.state().current()).isEqualTo(7);
        assertThat(r.freezeEarned()).isTrue();
        assertThat(r.state().freezes()).isEqualTo(1);

        var capped = StreakCalculator.onActivity(new State(13, 13, D, 2), D.plusDays(1));
        assertThat(capped.state().freezes()).isEqualTo(StreakCalculator.MAX_FREEZES);
        assertThat(capped.freezeEarned()).isFalse();
    }

    @Test
    void clockGoingBackwardsIsIgnored() {
        var s = new State(3, 3, D, 0);
        assertThat(StreakCalculator.onActivity(s, D.minusDays(1)).changed()).isFalse();
    }

    @Test
    void displayStatusNeverMutatesAndIsKind() {
        var s = new State(4, 4, D, 1);
        assertThat(StreakCalculator.status(s, D)).isEqualTo(DisplayStatus.DONE_TODAY);
        assertThat(StreakCalculator.status(s, D.plusDays(1))).isEqualTo(DisplayStatus.KEEP_IT_GOING);
        assertThat(StreakCalculator.status(s, D.plusDays(2))).isEqualTo(DisplayStatus.FREEZE_WILL_COVER);
        assertThat(StreakCalculator.status(s, D.plusDays(3))).isEqualTo(DisplayStatus.PAUSED);
        assertThat(StreakCalculator.status(new State(0, 0, null, 0), D)).isEqualTo(DisplayStatus.NOT_STARTED);
    }
}
