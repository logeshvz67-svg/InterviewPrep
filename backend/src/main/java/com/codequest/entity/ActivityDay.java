package com.codequest.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "activity_days")
public class ActivityDay extends BaseEntity {
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "local_date", nullable = false)
    private LocalDate localDate;

    /** true = this day was covered by a streak freeze, not by real activity. */
    @Column(name = "via_freeze", nullable = false)
    private boolean viaFreeze;

    public ActivityDay() {}

    public ActivityDay(Long userId, LocalDate localDate, boolean viaFreeze) {
        this.userId = userId;
        this.localDate = localDate;
        this.viaFreeze = viaFreeze;
    }

    public Long getUserId() { return userId; }
    public LocalDate getLocalDate() { return localDate; }
    public boolean isViaFreeze() { return viaFreeze; }
}
