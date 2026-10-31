package com.codequest.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class InterviewDtos {
    private InterviewDtos() {}

    public record BankQuestion(Long id, String extId, String category, String topic, String difficulty, List<String> companies,
                               String question, String answer, List<String> keyPoints, BigDecimal myBestScore) {}

    public record PracticeRequest(@Size(max = 6000) String myAnswer, @NotNull @Size(max = 6) List<@Min(0) @Max(5) Integer> keyPointsCovered) {}

    public record PracticeResult(BigDecimal score, int practisedToday, GameDtos.XpAward xp) {}

    public record AptitudeItem(Long id, String section, String topic, String difficulty, String question, List<String> options) {}

    public record AptitudeAnswer(@NotNull Long questionId, Integer chosen, @Min(0) @Max(3600) Integer seconds) {}

    public record AptitudeSubmit(@NotEmpty @Size(max = 60) List<@Valid AptitudeAnswer> answers) {}

    public record AptitudeResult(Long questionId, boolean correct, Integer chosen, int answer, String explanation) {}

    public record AptitudeGraded(int correct, int total, BigDecimal percent, List<AptitudeResult> results, GameDtos.XpAward xp) {}

    public record TopicAccuracy(String section, String topic, long attempts, long correct) {}

    // ---- exam pattern editor (YOU enter the official pattern)
    public record PatternSection(@NotBlank @Size(max = 80) String name,
                                 @NotBlank @Pattern(regexp = "APTITUDE|TECH_MCQ|CODING") String kind,
                                 @Min(1) @Max(100) int questions,
                                 @Min(1) @Max(300) int minutes,
                                 @Min(0) @Max(100) Integer cutoffPercent) {}

    public record ExamPatternRequest(@NotBlank String companyCode, @NotBlank @Size(max = 120) String name,
                                     @NotEmpty @Size(max = 10) List<@Valid PatternSection> sections,
                                     @Size(max = 300) String sourceNote, @PastOrPresent LocalDate verifiedOn) {}

    public record ExamPatternView(Long id, String companyCode, String name, JsonNode sections, String sourceNote, LocalDate verifiedOn, int totalMinutes) {}

    // ---- timed mock tests
    public record MockSection(String name, String kind, int minutes, Integer cutoffPercent, List<JsonNode> items) {}

    public record MockTestView(Long id, String title, String companyCode, String status, Instant startedAt, Instant endsAt, Instant serverNow,
                               int totalMinutes, List<MockSection> sections, JsonNode result) {}

    /** answers: { "APTITUDE:<id>": 2, "TECH:<id>": 1, "CODING:<id>": {"status":"SOLVED","minutes":25} } */
    public record MockSubmit(@NotNull Map<@NotBlank String, JsonNode> answers) {}

    public record MockSummary(Long id, String title, String companyCode, String status, Instant startedAt, BigDecimal score) {}

    // ---- diary
    public record DiaryRequest(@NotBlank @Size(max = 80) String company, @NotBlank @Size(max = 40) String round,
                               @NotNull @PastOrPresent LocalDate interviewDate, @NotBlank @Size(max = 2000) String question,
                               @Size(max = 6000) String myAnswer, @Size(max = 6000) String betterAnswer, @Size(max = 200) String tags) {}

    public record DiaryView(Long id, String company, String round, LocalDate interviewDate, String question, String myAnswer,
                            String betterAnswer, String tags, Instant createdAt) {}
}
