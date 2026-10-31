package com.codequest.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A node of the Spring Boot concept map. content = {summary, checklist, quiz, explainBack}. */
@Entity
@Table(name = "spring_topics")
public class SpringTopic extends BaseEntity {
    @Column(nullable = false, unique = true) private String code;
    @Column(nullable = false) private String title;
    @Column(name = "topic_group", nullable = false) private String topicGroup;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false) private JsonNode content;

    public String getCode() { return code; }
    public void setCode(String v) { code = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { title = v; }
    public String getTopicGroup() { return topicGroup; }
    public void setTopicGroup(String v) { topicGroup = v; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int v) { sortOrder = v; }
    public JsonNode getContent() { return content; }
    public void setContent(JsonNode v) { content = v; }
}
