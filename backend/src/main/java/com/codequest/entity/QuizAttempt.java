package com.codequest.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt extends BaseEntity {
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "lesson_id") private Long lessonId;
    @Column(nullable = false) private String kind;
    @Column(nullable = false) private int total;
    @Column(nullable = false) private int correct;
    @Column(name = "score_percent", nullable = false, precision = 5, scale = 2) private BigDecimal scorePercent;
    @Column(nullable = false) private boolean passed;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false) private JsonNode details;
    @Column(name = "local_date", nullable = false) private LocalDate localDate;

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { userId = v; }
    public Long getLessonId() { return lessonId; }
    public void setLessonId(Long v) { lessonId = v; }
    public String getKind() { return kind; }
    public void setKind(String v) { kind = v; }
    public int getTotal() { return total; }
    public void setTotal(int v) { total = v; }
    public int getCorrect() { return correct; }
    public void setCorrect(int v) { correct = v; }
    public BigDecimal getScorePercent() { return scorePercent; }
    public void setScorePercent(BigDecimal v) { scorePercent = v; }
    public boolean isPassed() { return passed; }
    public void setPassed(boolean v) { passed = v; }
    public JsonNode getDetails() { return details; }
    public void setDetails(JsonNode v) { details = v; }
    public LocalDate getLocalDate() { return localDate; }
    public void setLocalDate(LocalDate v) { localDate = v; }
}
