package com.codequest.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class SpeakDtos {
    private SpeakDtos() {}

    public record PromptView(Long id, String category, String prompt, String framework, int timeLimitSeconds, List<String> tips, String sampleAnswer) {}

    public record SessionRequest(Long promptId,
                                 @NotBlank @Pattern(regexp = "PRACTICE|EXPLAIN_ALOUD") String mode,
                                 @NotBlank @Size(max = 1000) String promptText,
                                 @NotBlank @Size(min = 5, max = 8000) String transcript,
                                 @Min(3) @Max(900) int durationSeconds,
                                 @NotBlank @Pattern(regexp = "VOICE|TYPED") String inputMethod,
                                 @Min(0) @Max(200) int longPauses,
                                 boolean useAi) {}

    public record SessionView(Long id, String mode, String promptText, String transcript, String inputMethod, int durationSeconds,
                              int wordCount, int wpm, int fillerCount, Map<String, Integer> fillers, int longPauses,
                              Map<String, Integer> scores, BigDecimal overallScore, String feedback, String improvedAnswer,
                              String followUpQuestion, boolean aiUsed, LocalDate localDate, Instant createdAt) {}

    public record SessionSaved(SessionView session, GameDtos.XpAward xp, String notice) {}

    public record Trends(int speakingStreak, int sessionsTotal, List<Map<String, Object>> weekly) {}

    public record StartInterviewRequest(@NotBlank @Pattern(regexp = "HR|TECHNICAL|SPRING") String roundType) {}

    public record InterviewAnswerRequest(@NotBlank @Size(max = 6000) String answer, @Min(0) @Max(900) int durationSeconds) {}

    /** The next thing the interviewer says (spoken with text-to-speech in the browser). */
    public record InterviewTurn(Long interviewId, String interviewerText, int questionNumber, int totalQuestions, boolean finished,
                                boolean aiUsed, String notice) {}

    public record InterviewView(Long id, String roundType, String status, JsonNode transcript, JsonNode scorecard,
                                BigDecimal overallScore, boolean aiUsed, Instant startedAt, Instant endedAt) {}
}
