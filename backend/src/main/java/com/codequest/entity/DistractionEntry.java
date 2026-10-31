package com.codequest.entity;

import com.codequest.entity.GymEnums.DistractionCategory;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "distraction_log")
public class DistractionEntry extends BaseEntity {
    @Column(name = "user_id", nullable = false) private Long userId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private DistractionCategory category;
    private String note;
    @Column(name = "local_date", nullable = false) private LocalDate localDate;
    @Column(name = "local_hour", nullable = false) private int localHour;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public DistractionCategory getCategory() { return category; }
    public void setCategory(DistractionCategory category) { this.category = category; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDate getLocalDate() { return localDate; }
    public void setLocalDate(LocalDate localDate) { this.localDate = localDate; }
    public int getLocalHour() { return localHour; }
    public void setLocalHour(int localHour) { this.localHour = localHour; }
}
