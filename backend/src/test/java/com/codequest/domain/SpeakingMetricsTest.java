package com.codequest.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpeakingMetricsTest {
    @Test
    void countsWordsWpmAndFillers() {
        var r = SpeakingMetrics.compute("Um so basically I mean you know Java is like a language um", 30, 0, 60, "NONE");
        assertThat(r.words()).isEqualTo(13);
        assertThat(r.wpm()).isEqualTo(26);
        assertThat(r.fillers()).containsEntry("um", 2).containsEntry("basically", 1).containsEntry("i mean", 1)
                .containsEntry("you know", 1).containsEntry("like", 1);
        assertThat(r.fillerCount()).isEqualTo(6);
    }

    @Test
    void fillerWordsInsideOtherWordsDontCount() {
        var r = SpeakingMetrics.compute("I liked the umbrella and the summary", 10, 0, 60, "NONE");
        assertThat(r.fillerCount()).isZero();
    }

    @Test
    void starStructureIsRecognised() {
        String star = "During my final year the situation was hard. My task was to finish the project. So I decided to split it. As a result we finished early and I learned planning.";
        var r = SpeakingMetrics.compute(star, 20, 0, 60, "STAR");
        assertThat(r.scores().get("structure")).isEqualTo(10);
    }

    @Test
    void veryShortAnswersScoreLow() {
        assertThat(SpeakingMetrics.compute("hello there", 5, 0, 60, "NONE").overall()).isEqualTo(20);
    }
}
