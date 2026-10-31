package com.codequest.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "daily_commitments")
public class DailyCommitment extends BaseEntity {
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "for_date", nullable = false) private LocalDate forDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<Task> tasks = new ArrayList<>();

    /** One committed task. A record is a perfect fit: small, immutable data. */
    public record Task(String text, boolean done) {}

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDate getForDate() { return forDate; }
    public void setForDate(LocalDate forDate) { this.forDate = forDate; }
    public List<Task> getTasks() { return tasks; }
    public void setTasks(List<Task> tasks) { this.tasks = tasks; }
}
