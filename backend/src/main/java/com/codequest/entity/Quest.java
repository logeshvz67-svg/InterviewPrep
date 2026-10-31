package com.codequest.entity;

import jakarta.persistence.*;

/** Shared catalog row (same for every user). Read-only for normal users. */
@Entity
@Table(name = "quests")
public class Quest extends BaseEntity {
    @Column(nullable = false, unique = true) private String code;
    @Column(nullable = false) private String kind;
    @Column(nullable = false) private String title;
    @Column(nullable = false) private String description;
    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false) private com.codequest.domain.QuestTrigger triggerType;
    @Column(nullable = false) private int target;
    @Column(name = "gold_reward", nullable = false) private int goldReward;
    /** For COMPUTED weekly/story quests: which statistic measures progress. */
    private String metric;

    public String getCode() { return code; }
    public String getKind() { return kind; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public com.codequest.domain.QuestTrigger getTriggerType() { return triggerType; }
    public int getTarget() { return target; }
    public int getGoldReward() { return goldReward; }
    public String getMetric() { return metric; }
}
