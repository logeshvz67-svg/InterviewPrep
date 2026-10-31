package com.codequest.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ai_messages")
public class AiMessage extends BaseEntity {
    @Column(name = "conversation_id", nullable = false) private Long conversationId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(nullable = false) private String role;
    @Column(nullable = false) private String content;
    private String model;
    /** true = this answer came from a non-AI fallback (AI off or resting). */
    @Column(nullable = false) private boolean fallback;

    public AiMessage() {}
    public AiMessage(Long conversationId, Long userId, String role, String content, String model, boolean fallback) {
        this.conversationId = conversationId;
        this.userId = userId;
        this.role = role;
        this.content = content;
        this.model = model;
        this.fallback = fallback;
    }

    public Long getConversationId() { return conversationId; }
    public Long getUserId() { return userId; }
    public String getRole() { return role; }
    public String getContent() { return content; }
    public String getModel() { return model; }
    public boolean isFallback() { return fallback; }
}
