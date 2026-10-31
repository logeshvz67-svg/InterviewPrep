package com.codequest.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "problem_revisits")
public class ProblemRevisit extends BaseEntity {
    @Column(name = "user_id", nullable = false) private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id")
    private ProblemSolved problem;

    /** 1 = after 3 days, 2 = after 7 days, 3 = after 14 days. */
    @Column(nullable = false) private int round;
    @Column(name = "due_date", nullable = false) private LocalDate dueDate;
    @Column(name = "completed_at") private Instant completedAt;
    @Column(name = "solved_unaided") private Boolean solvedUnaided;
    private String notes;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public ProblemSolved getProblem() { return problem; }
    public void setProblem(ProblemSolved problem) { this.problem = problem; }
    public int getRound() { return round; }
    public void setRound(int round) { this.round = round; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Boolean getSolvedUnaided() { return solvedUnaided; }
    public void setSolvedUnaided(Boolean solvedUnaided) { this.solvedUnaided = solvedUnaided; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
