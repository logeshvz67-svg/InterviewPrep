package com.codequest.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "aptitude_questions")
public class AptitudeQuestion extends BaseEntity {
    @Column(name = "ext_id", nullable = false, unique = true) private String extId;
    @Column(nullable = false) private String section;
    @Column(nullable = false) private String topic;
    @Column(nullable = false) private String difficulty;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false) private List<String> companies = new ArrayList<>();
    @Column(nullable = false) private String question;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false) private List<String> options = new ArrayList<>();
    @Column(nullable = false) private int answer;
    @Column(nullable = false) private String explanation;

    public boolean relevantTo(String company) {
        return company == null || company.isBlank() || companies.contains("ALL") || companies.contains(company);
    }

    public String getExtId() { return extId; }
    public void setExtId(String v) { extId = v; }
    public String getSection() { return section; }
    public void setSection(String v) { section = v; }
    public String getTopic() { return topic; }
    public void setTopic(String v) { topic = v; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String v) { difficulty = v; }
    public List<String> getCompanies() { return companies; }
    public void setCompanies(List<String> v) { companies = v; }
    public String getQuestion() { return question; }
    public void setQuestion(String v) { question = v; }
    public List<String> getOptions() { return options; }
    public void setOptions(List<String> v) { options = v; }
    public int getAnswer() { return answer; }
    public void setAnswer(int v) { answer = v; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String v) { explanation = v; }
}
