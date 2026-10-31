package com.codequest.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lesson_progress")
public class LessonProgress extends BaseEntity {
    public enum Status { IN_PROGRESS, COMPLETED }

    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "lesson_id", nullable = false) private Long lessonId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Status status = Status.IN_PROGRESS;

    /** Teaching steps finished, e.g. ["why","analogy","tryIt","predict"]. */
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "steps_done", nullable = false)
    private List<String> stepsDone = new ArrayList<>();

    @Column(name = "best_quiz_percent", nullable = false, precision = 5, scale = 2) private BigDecimal bestQuizPercent = BigDecimal.ZERO;
    @Column(name = "quiz_passed_at") private Instant quizPassedAt;
    @Column(name = "completed_at") private Instant completedAt;
    /** LESSON (normal path) or PROVE_IT (tested out with 90%+). */
    @Column(name = "completed_via") private String completedVia;
    @Column(name = "explain_back") private String explainBack;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "explain_back_checks") private List<Integer> explainBackChecks;
    @Column(name = "explain_back_score", precision = 5, scale = 2) private BigDecimal explainBackScore;

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { userId = v; }
    public Long getLessonId() { return lessonId; }
    public void setLessonId(Long v) { lessonId = v; }
    public Status getStatus() { return status; }
    public void setStatus(Status v) { status = v; }
    public List<String> getStepsDone() { return stepsDone; }
    public void setStepsDone(List<String> v) { stepsDone = v; }
    public BigDecimal getBestQuizPercent() { return bestQuizPercent; }
    public void setBestQuizPercent(BigDecimal v) { bestQuizPercent = v; }
    public Instant getQuizPassedAt() { return quizPassedAt; }
    public void setQuizPassedAt(Instant v) { quizPassedAt = v; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant v) { completedAt = v; }
    public String getCompletedVia() { return completedVia; }
    public void setCompletedVia(String v) { completedVia = v; }
    public String getExplainBack() { return explainBack; }
    public void setExplainBack(String v) { explainBack = v; }
    public List<Integer> getExplainBackChecks() { return explainBackChecks; }
    public void setExplainBackChecks(List<Integer> v) { explainBackChecks = v; }
    public BigDecimal getExplainBackScore() { return explainBackScore; }
    public void setExplainBackScore(BigDecimal v) { explainBackScore = v; }
    public boolean isCompleted() { return status == Status.COMPLETED; }
}
