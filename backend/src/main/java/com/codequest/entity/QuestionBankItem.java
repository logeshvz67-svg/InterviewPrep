package com.codequest.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

/** Interview practice question (labelled "practice", never "actually asked"). */
@Entity
@Table(name = "question_bank")
public class QuestionBankItem extends BaseEntity {
    @Column(name = "ext_id", nullable = false, unique = true) private String extId;
    @Column(nullable = false) private String category;
    @Column(nullable = false) private String topic;
    @Column(nullable = false) private String difficulty;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false) private List<String> companies = new ArrayList<>();
    @Column(nullable = false) private String question;
    @Column(nullable = false) private String answer;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "key_points", nullable = false) private List<String> keyPoints = new ArrayList<>();

    public boolean relevantTo(String company) {
        return company == null || company.isBlank() || companies.contains("ALL") || companies.contains(company);
    }

    public String getExtId() { return extId; }
    public void setExtId(String v) { extId = v; }
    public String getCategory() { return category; }
    public void setCategory(String v) { category = v; }
    public String getTopic() { return topic; }
    public void setTopic(String v) { topic = v; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String v) { difficulty = v; }
    public List<String> getCompanies() { return companies; }
    public void setCompanies(List<String> v) { companies = v; }
    public String getQuestion() { return question; }
    public void setQuestion(String v) { question = v; }
    public String getAnswer() { return answer; }
    public void setAnswer(String v) { answer = v; }
    public List<String> getKeyPoints() { return keyPoints; }
    public void setKeyPoints(List<String> v) { keyPoints = v; }
}
