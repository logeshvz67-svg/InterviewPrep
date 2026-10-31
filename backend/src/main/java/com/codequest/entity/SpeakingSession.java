package com.codequest.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/** One speaking attempt. Only the transcript and scores are stored, never the audio. */
@Entity
@Table(name = "speaking_sessions")
public class SpeakingSession extends BaseEntity {
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "prompt_id") private Long promptId;
    @Column(nullable = false) private String mode;
    @Column(name = "prompt_text", nullable = false) private String promptText;
    @Column(nullable = false) private String transcript;
    @Column(name = "input_method", nullable = false) private String inputMethod;
    @Column(name = "duration_seconds", nullable = false) private int durationSeconds;
    @Column(name = "word_count", nullable = false) private int wordCount;
    @Column(nullable = false) private int wpm;
    @Column(name = "filler_count", nullable = false) private int fillerCount;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false) private Map<String, Integer> fillers;
    @Column(name = "long_pauses", nullable = false) private int longPauses;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false) private Map<String, Integer> scores;
    @Column(name = "overall_score", nullable = false, precision = 5, scale = 2) private BigDecimal overallScore;
    private String feedback;
    @Column(name = "improved_answer") private String improvedAnswer;
    @Column(name = "ai_used", nullable = false) private boolean aiUsed;
    @Column(name = "local_date", nullable = false) private LocalDate localDate;

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { userId = v; }
    public Long getPromptId() { return promptId; }
    public void setPromptId(Long v) { promptId = v; }
    public String getMode() { return mode; }
    public void setMode(String v) { mode = v; }
    public String getPromptText() { return promptText; }
    public void setPromptText(String v) { promptText = v; }
    public String getTranscript() { return transcript; }
    public void setTranscript(String v) { transcript = v; }
    public String getInputMethod() { return inputMethod; }
    public void setInputMethod(String v) { inputMethod = v; }
    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int v) { durationSeconds = v; }
    public int getWordCount() { return wordCount; }
    public void setWordCount(int v) { wordCount = v; }
    public int getWpm() { return wpm; }
    public void setWpm(int v) { wpm = v; }
    public int getFillerCount() { return fillerCount; }
    public void setFillerCount(int v) { fillerCount = v; }
    public Map<String, Integer> getFillers() { return fillers; }
    public void setFillers(Map<String, Integer> v) { fillers = v; }
    public int getLongPauses() { return longPauses; }
    public void setLongPauses(int v) { longPauses = v; }
    public Map<String, Integer> getScores() { return scores; }
    public void setScores(Map<String, Integer> v) { scores = v; }
    public BigDecimal getOverallScore() { return overallScore; }
    public void setOverallScore(BigDecimal v) { overallScore = v; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String v) { feedback = v; }
    public String getImprovedAnswer() { return improvedAnswer; }
    public void setImprovedAnswer(String v) { improvedAnswer = v; }
    public boolean isAiUsed() { return aiUsed; }
    public void setAiUsed(boolean v) { aiUsed = v; }
    public LocalDate getLocalDate() { return localDate; }
    public void setLocalDate(LocalDate v) { localDate = v; }
}
