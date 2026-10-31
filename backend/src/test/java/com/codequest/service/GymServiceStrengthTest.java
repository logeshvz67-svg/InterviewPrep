package com.codequest.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class GymServiceStrengthTest {
    @Test
    void zeroProblemsIsZero() {
        assertThat(GymService.strengthScore(0, 5, 1)).isZero();
    }

    @Test
    void fiveConfidentUnaidedProblemsIsFull() {
        assertThat(GymService.strengthScore(5, 5, 1)).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void hintsLowerStrength() {
        assertThat(GymService.strengthScore(5, 5, 0)).isLessThan(GymService.strengthScore(5, 5, 1));
    }
}
