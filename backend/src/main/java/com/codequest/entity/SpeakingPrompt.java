package com.codequest.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "speaking_prompts")
public class SpeakingPrompt extends BaseEntity {
    @Column(name = "ext_id", nullable = false, unique = true) private String extId;
    @Column(nullable = false) private String category;
    @Column(nullable = false) private String prompt;
    @Column(nullable = false) private String framework;
    @Column(name = "time_limit_seconds", nullable = false) private int timeLimitSeconds;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false) private List<String> tips = new ArrayList<>();
    @Column(name = "sample_answer", nullable = false) private String sampleAnswer;

    public String getExtId() { return extId; }
    public void setExtId(String v) { extId = v; }
    public String getCategory() { return category; }
    public void setCategory(String v) { category = v; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String v) { prompt = v; }
    public String getFramework() { return framework; }
    public void setFramework(String v) { framework = v; }
    public int getTimeLimitSeconds() { return timeLimitSeconds; }
    public void setTimeLimitSeconds(int v) { timeLimitSeconds = v; }
    public List<String> getTips() { return tips; }
    public void setTips(List<String> v) { tips = v; }
    public String getSampleAnswer() { return sampleAnswer; }
    public void setSampleAnswer(String v) { sampleAnswer = v; }
}
