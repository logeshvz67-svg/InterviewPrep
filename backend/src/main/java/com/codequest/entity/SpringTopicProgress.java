package com.codequest.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "spring_topic_progress")
public class SpringTopicProgress extends BaseEntity {
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "topic_id", nullable = false) private Long topicId;
    /** NOT_STARTED | LEARNING | UNDERSTOOD | CAN_EXPLAIN */
    @Column(nullable = false) private String status = "NOT_STARTED";
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "checklist_done", nullable = false) private List<Integer> checklistDone = new ArrayList<>();
    @Column(name = "best_quiz_percent", nullable = false, precision = 5, scale = 2) private BigDecimal bestQuizPercent = BigDecimal.ZERO;
    @Column(name = "explain_back") private String explainBack;

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { userId = v; }
    public Long getTopicId() { return topicId; }
    public void setTopicId(Long v) { topicId = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { status = v; }
    public List<Integer> getChecklistDone() { return checklistDone; }
    public void setChecklistDone(List<Integer> v) { checklistDone = v; }
    public BigDecimal getBestQuizPercent() { return bestQuizPercent; }
    public void setBestQuizPercent(BigDecimal v) { bestQuizPercent = v; }
    public String getExplainBack() { return explainBack; }
    public void setExplainBack(String v) { explainBack = v; }
}
