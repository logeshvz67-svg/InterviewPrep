package com.codequest.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "project_milestones")
public class ProjectMilestone extends BaseEntity {
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "project_id", nullable = false) private Long projectId;
    @Column(nullable = false) private String title;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    @Column(name = "done_at") private Instant doneAt;

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { userId = v; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long v) { projectId = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { title = v; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int v) { sortOrder = v; }
    public Instant getDoneAt() { return doneAt; }
    public void setDoneAt(Instant v) { doneAt = v; }
}
