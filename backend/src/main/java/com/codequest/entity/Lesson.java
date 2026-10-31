package com.codequest.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashSet;
import java.util.Set;

/** A lesson (imported from YAML). The full teaching content is one JSON document. */
@Entity
@Table(name = "lessons")
public class Lesson extends BaseEntity {
    @Column(name = "world_code", nullable = false) private String worldCode;
    @Column(nullable = false, unique = true) private String slug;
    @Column(nullable = false) private String section;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    @Column(nullable = false) private String title;
    @Column(name = "topic_id") private Long topicId;
    @Column(name = "pattern_code") private String patternCode;
    @Column(name = "estimated_minutes", nullable = false) private int estimatedMinutes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private JsonNode content;

    @Column(name = "content_version", nullable = false) private int contentVersion;
    @Column(name = "admin_edited", nullable = false) private boolean adminEdited;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "lesson_company_relevance", joinColumns = @JoinColumn(name = "lesson_id"))
    @Column(name = "company_code")
    private Set<String> companyCodes = new HashSet<>();

    public boolean relevantTo(String company) {
        return company == null || company.isBlank() || companyCodes.contains("ALL") || companyCodes.contains(company);
    }

    public String getWorldCode() { return worldCode; }
    public void setWorldCode(String v) { worldCode = v; }
    public String getSlug() { return slug; }
    public void setSlug(String v) { slug = v; }
    public String getSection() { return section; }
    public void setSection(String v) { section = v; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int v) { sortOrder = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { title = v; }
    public Long getTopicId() { return topicId; }
    public void setTopicId(Long v) { topicId = v; }
    public String getPatternCode() { return patternCode; }
    public void setPatternCode(String v) { patternCode = v; }
    public int getEstimatedMinutes() { return estimatedMinutes; }
    public void setEstimatedMinutes(int v) { estimatedMinutes = v; }
    public JsonNode getContent() { return content; }
    public void setContent(JsonNode v) { content = v; }
    public int getContentVersion() { return contentVersion; }
    public void setContentVersion(int v) { contentVersion = v; }
    public boolean isAdminEdited() { return adminEdited; }
    public void setAdminEdited(boolean v) { adminEdited = v; }
    public Set<String> getCompanyCodes() { return companyCodes; }
    public void setCompanyCodes(Set<String> v) { companyCodes = v; }
}
