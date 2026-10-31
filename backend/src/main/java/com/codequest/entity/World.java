package com.codequest.entity;

import jakarta.persistence.*;

/** Shared catalog row (same for every user). Read-only for normal users. */
@Entity
@Table(name = "worlds")
public class World extends BaseEntity {
    @Column(nullable = false, unique = true) private String code;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String description;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    @Column(name = "unlock_quiz_percent", nullable = false) private int unlockQuizPercent;

    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getSortOrder() { return sortOrder; }
    public int getUnlockQuizPercent() { return unlockQuizPercent; }
}
