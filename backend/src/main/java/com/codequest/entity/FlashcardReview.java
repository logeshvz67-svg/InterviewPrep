package com.codequest.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "flashcard_reviews")
public class FlashcardReview extends BaseEntity {
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "flashcard_id", nullable = false) private Long flashcardId;
    @Column(nullable = false) private int grade;
    @Column(name = "interval_before", nullable = false) private int intervalBefore;
    @Column(name = "interval_after", nullable = false) private int intervalAfter;
    @Column(name = "ease_after", nullable = false, precision = 4, scale = 2) private BigDecimal easeAfter;
    @Column(name = "local_date", nullable = false) private LocalDate localDate;

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { userId = v; }
    public Long getFlashcardId() { return flashcardId; }
    public void setFlashcardId(Long v) { flashcardId = v; }
    public int getGrade() { return grade; }
    public void setGrade(int v) { grade = v; }
    public int getIntervalBefore() { return intervalBefore; }
    public void setIntervalBefore(int v) { intervalBefore = v; }
    public int getIntervalAfter() { return intervalAfter; }
    public void setIntervalAfter(int v) { intervalAfter = v; }
    public BigDecimal getEaseAfter() { return easeAfter; }
    public void setEaseAfter(BigDecimal v) { easeAfter = v; }
    public LocalDate getLocalDate() { return localDate; }
    public void setLocalDate(LocalDate v) { localDate = v; }
}
