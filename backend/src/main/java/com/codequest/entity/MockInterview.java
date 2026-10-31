package com.codequest.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "mock_interviews")
public class MockInterview extends BaseEntity {
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "round_type", nullable = false) private String roundType;
    @Column(nullable = false) private String status;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "question_ids", nullable = false) private List<Long> questionIds;
    /** [{role: interviewer|candidate, text, questionId?, followUp?}] */
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false) private JsonNode transcript;
    @JdbcTypeCode(SqlTypes.JSON) private JsonNode scorecard;
    @Column(name = "overall_score", precision = 5, scale = 2) private BigDecimal overallScore;
    @Column(name = "ai_used", nullable = false) private boolean aiUsed;
    @Column(name = "started_at", nullable = false) private Instant startedAt;
    @Column(name = "ended_at") private Instant endedAt;

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { userId = v; }
    public String getRoundType() { return roundType; }
    public void setRoundType(String v) { roundType = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { status = v; }
    public List<Long> getQuestionIds() { return questionIds; }
    public void setQuestionIds(List<Long> v) { questionIds = v; }
    public JsonNode getTranscript() { return transcript; }
    public void setTranscript(JsonNode v) { transcript = v; }
    public JsonNode getScorecard() { return scorecard; }
    public void setScorecard(JsonNode v) { scorecard = v; }
    public BigDecimal getOverallScore() { return overallScore; }
    public void setOverallScore(BigDecimal v) { overallScore = v; }
    public boolean isAiUsed() { return aiUsed; }
    public void setAiUsed(boolean v) { aiUsed = v; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant v) { startedAt = v; }
    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant v) { endedAt = v; }
}
