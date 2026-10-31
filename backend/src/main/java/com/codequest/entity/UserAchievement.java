package com.codequest.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "user_achievements")
public class UserAchievement extends BaseEntity {
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "achievement_id", nullable = false) private Long achievementId;
    @Column(name = "unlocked_at", nullable = false) private Instant unlockedAt;

    public UserAchievement() {}
    public UserAchievement(Long userId, Long achievementId, Instant unlockedAt) {
        this.userId = userId;
        this.achievementId = achievementId;
        this.unlockedAt = unlockedAt;
    }

    public Long getUserId() { return userId; }
    public Long getAchievementId() { return achievementId; }
    public Instant getUnlockedAt() { return unlockedAt; }
}
