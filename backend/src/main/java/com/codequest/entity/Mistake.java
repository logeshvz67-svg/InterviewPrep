package com.codequest.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "mistakes")
public class Mistake extends BaseEntity {
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "category_code", nullable = false) private String categoryCode;
    @Column(name = "problem_id") private Long problemId;
    private String topic;
    @Column(name = "what_happened", nullable = false) private String whatHappened;
    @Column(name = "lesson_learned", nullable = false) private String lessonLearned;
    @Column(nullable = false) private boolean resolved;
    @Column(name = "resolved_at") private Instant resolvedAt;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getCategoryCode() { return categoryCode; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }
    public Long getProblemId() { return problemId; }
    public void setProblemId(Long problemId) { this.problemId = problemId; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getWhatHappened() { return whatHappened; }
    public void setWhatHappened(String whatHappened) { this.whatHappened = whatHappened; }
    public String getLessonLearned() { return lessonLearned; }
    public void setLessonLearned(String lessonLearned) { this.lessonLearned = lessonLearned; }
    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
}
