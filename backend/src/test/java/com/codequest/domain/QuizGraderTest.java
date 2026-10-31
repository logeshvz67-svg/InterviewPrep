package com.codequest.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuizGraderTest {
    final ObjectMapper m = new ObjectMapper();

    @Test
    void optionTypesCompareIndex() throws Exception {
        var answer = m.readTree("{\"index\":2}");
        assertThat(QuizGrader.isCorrect("MCQ", answer, m.readTree("2"))).isTrue();
        assertThat(QuizGrader.isCorrect("PREDICT_OUTPUT", answer, m.readTree("1"))).isFalse();
        assertThat(QuizGrader.isCorrect("MCQ", answer, null)).isFalse();
        assertThat(QuizGrader.isCorrect("MCQ", answer, m.readTree("\"x\""))).isFalse();
    }

    @Test
    void fillTheCodeIgnoresSpacesAndTrailingSemicolonButNotCase() throws Exception {
        var answer = m.readTree("{\"accepted\":[\"equals()\", \"s.equals(t)\"]}");
        assertThat(QuizGrader.isCorrect("FILL_THE_CODE", answer, m.readTree("\" equals ( ) \""))).isTrue();
        assertThat(QuizGrader.isCorrect("FILL_THE_CODE", answer, m.readTree("\"s.equals(t);\""))).isTrue();
        assertThat(QuizGrader.isCorrect("FILL_THE_CODE", answer, m.readTree("\"EQUALS()\""))).isFalse();
        assertThat(QuizGrader.isCorrect("FILL_THE_CODE", answer, m.readTree("\"\""))).isFalse();
    }

    @Test
    void matchNeedsEveryPairRight() throws Exception {
        var answer = m.readTree("{\"pairs\":[{\"left\":\"a\",\"right\":\"1\"},{\"left\":\"b\",\"right\":\"2\"}]}");
        assertThat(QuizGrader.isCorrect("MATCH", answer, m.readTree("[\"1\",\"2\"]"))).isTrue();
        assertThat(QuizGrader.isCorrect("MATCH", answer, m.readTree("[\"2\",\"1\"]"))).isFalse();
        assertThat(QuizGrader.isCorrect("MATCH", answer, m.readTree("[\"1\"]"))).isFalse();
    }

    @Test
    void percentRounds() {
        assertThat(QuizGrader.percent(6, 7)).isEqualByComparingTo("85.71");
        assertThat(QuizGrader.percent(0, 0)).isEqualByComparingTo("0");
    }
}
