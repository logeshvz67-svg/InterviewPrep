package com.codequest.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "streaks")
public class Streak extends BaseEntity {
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "current_streak", nullable = false)
    private int currentStreak;

    @Column(name = "longest_streak", nullable = false)
    private int longestStreak;

    @Column(name = "last_active_date")
    private LocalDate lastActiveDate;

    @Column(nullable = false)
    private int freezes;

    @Column(name = "comeback_pending", nullable = false)
    private boolean comebackPending;

    @Column(name = "comeback_count", nullable = false)
    private int comebackCount;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }
    public int getLongestStreak() { return longestStreak; }
    public void setLongestStreak(int longestStreak) { this.longestStreak = longestStreak; }
    public LocalDate getLastActiveDate() { return lastActiveDate; }
    public void setLastActiveDate(LocalDate lastActiveDate) { this.lastActiveDate = lastActiveDate; }
    public int getFreezes() { return freezes; }
    public void setFreezes(int freezes) { this.freezes = freezes; }
    public boolean isComebackPending() { return comebackPending; }
    public void setComebackPending(boolean comebackPending) { this.comebackPending = comebackPending; }
    public int getComebackCount() { return comebackCount; }
    public void setComebackCount(int comebackCount) { this.comebackCount = comebackCount; }
}
