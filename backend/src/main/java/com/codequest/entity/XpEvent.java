package com.codequest.entity;

import com.codequest.domain.XpAction;
import jakarta.persistence.*;

import java.time.LocalDate;

/** One row per XP award: the audit trail. Profile.totalXp is just a running total of these. */
@Entity
@Table(name = "xp_events")
public class XpEvent extends BaseEntity {
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private XpAction action;

    @Column(nullable = false)
    private int amount;

    @Column(nullable = false)
    private int gold;

    /** What was rewarded, e.g. "problem:42". Unique per action, so double-submits earn nothing. */
    @Column(name = "source_key", nullable = false)
    private String sourceKey;

    @Column(name = "local_date", nullable = false)
    private LocalDate localDate;

    private String note;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public XpAction getAction() { return action; }
    public void setAction(XpAction action) { this.action = action; }
    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }
    public int getGold() { return gold; }
    public void setGold(int gold) { this.gold = gold; }
    public String getSourceKey() { return sourceKey; }
    public void setSourceKey(String sourceKey) { this.sourceKey = sourceKey; }
    public LocalDate getLocalDate() { return localDate; }
    public void setLocalDate(LocalDate localDate) { this.localDate = localDate; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
