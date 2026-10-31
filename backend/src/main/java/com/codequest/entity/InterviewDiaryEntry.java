package com.codequest.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "interview_diary")
public class InterviewDiaryEntry extends BaseEntity {
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(nullable = false) private String company;
    @Column(nullable = false) private String round;
    @Column(name = "interview_date", nullable = false) private LocalDate interviewDate;
    @Column(nullable = false) private String question;
    @Column(name = "my_answer") private String myAnswer;
    @Column(name = "better_answer") private String betterAnswer;
    private String tags;

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { userId = v; }
    public String getCompany() { return company; }
    public void setCompany(String v) { company = v; }
    public String getRound() { return round; }
    public void setRound(String v) { round = v; }
    public LocalDate getInterviewDate() { return interviewDate; }
    public void setInterviewDate(LocalDate v) { interviewDate = v; }
    public String getQuestion() { return question; }
    public void setQuestion(String v) { question = v; }
    public String getMyAnswer() { return myAnswer; }
    public void setMyAnswer(String v) { myAnswer = v; }
    public String getBetterAnswer() { return betterAnswer; }
    public void setBetterAnswer(String v) { betterAnswer = v; }
    public String getTags() { return tags; }
    public void setTags(String v) { tags = v; }
}
