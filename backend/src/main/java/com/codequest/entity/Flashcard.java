package com.codequest.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "flashcards")
public class Flashcard extends BaseEntity {
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(nullable = false) private String front;
    @Column(nullable = false) private String back;
    private String topic;
    @Column(name = "source_type", nullable = false) private String sourceType;
    @Column(name = "source_ref", nullable = false) private String sourceRef;
    @Column(nullable = false, precision = 4, scale = 2) private BigDecimal ease = new BigDecimal("2.50");
    @Column(name = "interval_days", nullable = false) private int intervalDays;
    @Column(nullable = false) private int repetitions;
    @Column(nullable = false) private int lapses;
    @Column(name = "next_review_on", nullable = false) private LocalDate nextReviewOn;
    @Column(name = "last_reviewed_at") private Instant lastReviewedAt;
    @Column(nullable = false) private boolean suspended;

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { userId = v; }
    public String getFront() { return front; }
    public void setFront(String v) { front = v; }
    public String getBack() { return back; }
    public void setBack(String v) { back = v; }
    public String getTopic() { return topic; }
    public void setTopic(String v) { topic = v; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String v) { sourceType = v; }
    public String getSourceRef() { return sourceRef; }
    public void setSourceRef(String v) { sourceRef = v; }
    public BigDecimal getEase() { return ease; }
    public void setEase(BigDecimal v) { ease = v; }
    public int getIntervalDays() { return intervalDays; }
    public void setIntervalDays(int v) { intervalDays = v; }
    public int getRepetitions() { return repetitions; }
    public void setRepetitions(int v) { repetitions = v; }
    public int getLapses() { return lapses; }
    public void setLapses(int v) { lapses = v; }
    public LocalDate getNextReviewOn() { return nextReviewOn; }
    public void setNextReviewOn(LocalDate v) { nextReviewOn = v; }
    public Instant getLastReviewedAt() { return lastReviewedAt; }
    public void setLastReviewedAt(Instant v) { lastReviewedAt = v; }
    public boolean isSuspended() { return suspended; }
    public void setSuspended(boolean v) { suspended = v; }
}
