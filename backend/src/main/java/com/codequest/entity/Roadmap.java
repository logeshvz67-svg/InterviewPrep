package com.codequest.entity;

import com.codequest.domain.RoadmapWeek;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "roadmaps")
public class Roadmap extends BaseEntity {
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "total_weeks", nullable = false)
    private int totalWeeks;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<RoadmapWeek> weeks = new ArrayList<>();

    @Column(nullable = false)
    private boolean active = true;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public int getTotalWeeks() { return totalWeeks; }
    public void setTotalWeeks(int totalWeeks) { this.totalWeeks = totalWeeks; }
    public List<RoadmapWeek> getWeeks() { return weeks; }
    public void setWeeks(List<RoadmapWeek> weeks) { this.weeks = weeks; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
