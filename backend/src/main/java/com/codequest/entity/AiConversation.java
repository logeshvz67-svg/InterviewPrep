package com.codequest.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ai_conversations")
public class AiConversation extends BaseEntity {
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(nullable = false) private String mode;
    @Column(nullable = false) private String title;
    private String topic;
    @Column(nullable = false) private boolean resolved;
    @Column(name = "hint_level", nullable = false) private int hintLevel;

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { userId = v; }
    public String getMode() { return mode; }
    public void setMode(String v) { mode = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { title = v; }
    public String getTopic() { return topic; }
    public void setTopic(String v) { topic = v; }
    public boolean isResolved() { return resolved; }
    public void setResolved(boolean v) { resolved = v; }
    public int getHintLevel() { return hintLevel; }
    public void setHintLevel(int v) { hintLevel = v; }
    public void touch() { onUpdate(); }
}
