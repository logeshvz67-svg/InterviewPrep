package com.codequest.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/** A timed mock test. The timer is just two timestamps (started_at, ends_at): no server timers. */
@Entity
@Table(name = "mock_tests")
public class MockTest extends BaseEntity {
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "company_code", nullable = false) private String companyCode;
    @Column(nullable = false) private String title;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false) private JsonNode pattern;
    /** [{name, kind, minutes, cutoffPercent, itemIds:[...]}] */
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false) private JsonNode paper;
    @Column(name = "total_minutes", nullable = false) private int totalMinutes;
    @Column(nullable = false) private String status;
    @Column(name = "started_at", nullable = false) private Instant startedAt;
    @Column(name = "ends_at", nullable = false) private Instant endsAt;

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { userId = v; }
    public String getCompanyCode() { return companyCode; }
    public void setCompanyCode(String v) { companyCode = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { title = v; }
    public JsonNode getPattern() { return pattern; }
    public void setPattern(JsonNode v) { pattern = v; }
    public JsonNode getPaper() { return paper; }
    public void setPaper(JsonNode v) { paper = v; }
    public int getTotalMinutes() { return totalMinutes; }
    public void setTotalMinutes(int v) { totalMinutes = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { status = v; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant v) { startedAt = v; }
    public Instant getEndsAt() { return endsAt; }
    public void setEndsAt(Instant v) { endsAt = v; }
}
