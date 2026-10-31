package com.codequest.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "spring_projects")
public class SpringProject extends BaseEntity {
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(nullable = false) private String code;
    @Column(nullable = false) private String title;
    @Column(nullable = false) private String description;
    @Column(name = "github_url") private String githubUrl;
    /** NOT_STARTED | IN_PROGRESS | DONE */
    @Column(nullable = false) private String status = "NOT_STARTED";

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { userId = v; }
    public String getCode() { return code; }
    public void setCode(String v) { code = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { title = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { description = v; }
    public String getGithubUrl() { return githubUrl; }
    public void setGithubUrl(String v) { githubUrl = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { status = v; }
}
