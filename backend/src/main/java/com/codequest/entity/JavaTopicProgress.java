package com.codequest.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "java_topic_progress")
public class JavaTopicProgress extends BaseEntity {
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * @ManyToOne(fetch = LAZY): many progress rows point at one topic.
     * LAZY means Hibernate gives us a lightweight proxy and only runs the SELECT for the
     * topic when we actually use it. (The JPA default for @ManyToOne is EAGER, which can cause
     * the "N+1 queries" problem, so we switch it to LAZY on purpose.)
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id")
    private JavaTopic topic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TopicStatus status = TopicStatus.NOT_STARTED;

    /** BigDecimal for scores: exact decimals, no 0.1 + 0.2 = 0.30000000000000004 surprises. */
    @Column(name = "mastery_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal masteryScore = BigDecimal.ZERO;

    @Column(name = "last_reviewed")
    private Instant lastReviewed;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public JavaTopic getTopic() { return topic; }
    public void setTopic(JavaTopic topic) { this.topic = topic; }
    public TopicStatus getStatus() { return status; }
    public void setStatus(TopicStatus status) { this.status = status; }
    public BigDecimal getMasteryScore() { return masteryScore; }
    public void setMasteryScore(BigDecimal masteryScore) { this.masteryScore = masteryScore; }
    public Instant getLastReviewed() { return lastReviewed; }
    public void setLastReviewed(Instant lastReviewed) { this.lastReviewed = lastReviewed; }
}
