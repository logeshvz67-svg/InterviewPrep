package com.codequest.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "mock_test_attempts")
public class MockTestAttempt extends BaseEntity {
    @Column(name = "mock_test_id", nullable = false, unique = true) private Long mockTestId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false) private JsonNode answers;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "section_scores", nullable = false) private JsonNode sectionScores;
    @Column(name = "score_percent", nullable = false, precision = 5, scale = 2) private BigDecimal scorePercent;
    @Column(name = "submitted_at", nullable = false) private Instant submittedAt;
    @Column(nullable = false) private boolean late;

    public Long getMockTestId() { return mockTestId; }
    public void setMockTestId(Long v) { mockTestId = v; }
    public Long getUserId() { return userId; }
    public void setUserId(Long v) { userId = v; }
    public JsonNode getAnswers() { return answers; }
    public void setAnswers(JsonNode v) { answers = v; }
    public JsonNode getSectionScores() { return sectionScores; }
    public void setSectionScores(JsonNode v) { sectionScores = v; }
    public BigDecimal getScorePercent() { return scorePercent; }
    public void setScorePercent(BigDecimal v) { scorePercent = v; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant v) { submittedAt = v; }
    public boolean isLate() { return late; }
    public void setLate(boolean v) { late = v; }
}
