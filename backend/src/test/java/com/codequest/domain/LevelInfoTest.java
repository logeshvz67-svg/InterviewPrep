package com.codequest.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LevelInfoTest {

    @ParameterizedTest
    @CsvSource({"0,1", "99,1", "100,2", "299,2", "300,3", "1000,5", "4500,10"})
    void levelForXp(int xp, int expectedLevel) {
        assertThat(LevelInfo.of(xp).level()).isEqualTo(expectedLevel);
    }

    @ParameterizedTest
    @CsvSource({"0,INTERN", "499,INTERN", "500,TRAINEE", "1500,DEVELOPER", "3500,SENIOR_DEV", "7000,TECH_LEAD", "12000,ARCHITECT", "99999,ARCHITECT"})
    void rankForXp(int xp, Rank expected) {
        assertThat(Rank.forXp(xp)).isEqualTo(expected);
    }

    @Test
    void progressBarAndNextRank() {
        LevelInfo l = LevelInfo.of(150); // level 2: 100..300
        assertThat(l.levelProgressPercent()).isEqualTo(25);
        assertThat(l.nextRank()).isEqualTo(Rank.TRAINEE);
        assertThat(l.nextRankXp()).isEqualTo(500);
        assertThat(LevelInfo.of(20000).nextRank()).isNull();
    }

    @Test
    void negativeXpIsRejected() {
        assertThatThrownBy(() -> LevelInfo.of(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void xpTableMatchesTheBrief() {
        assertThat(XpAction.LESSON_COMPLETE.xp()).isEqualTo(20);
        assertThat(XpAction.QUIZ_PASS.xp()).isEqualTo(25);
        assertThat(XpAction.PROBLEM_WITH_APPROACH.xp()).isEqualTo(15);
        assertThat(XpAction.REVISIT_RESOLVED.xp()).isEqualTo(25);
        assertThat(XpAction.MOCK_INTERVIEW.xp()).isEqualTo(60);
        assertThat(XpAction.FOCUS_SESSION_25.gold()).isEqualTo(2);
    }
}
