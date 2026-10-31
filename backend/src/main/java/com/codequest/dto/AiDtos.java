package com.codequest.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class AiDtos {
    private AiDtos() {}

    public record ChatRequest(Long conversationId,
                              @NotBlank @Pattern(regexp = "DOUBT|SOCRATIC|CODE_REVIEW|MISTAKE") String mode,
                              @NotBlank @Size(max = 6000) String message,
                              @Size(max = 120) String topic) {}

    public record MessageView(Long id, String role, String content, boolean fallback, Instant createdAt) {}

    public record ChatResponse(Long conversationId, MessageView reply, boolean aiUsed, String notice) {}

    public record ConversationView(Long id, String mode, String title, String topic, boolean resolved, int hintLevel,
                                   Instant updatedAt, List<MessageView> messages) {}

    /** Hint ladder: 1 nudge -> 2 pattern name -> 3 pseudo-code -> 4 full approach. */
    public record HintRequest(@NotBlank @Size(max = 200) String problemTitle, @Size(max = 3000) String problemText,
                              @Size(max = 40) String patternCode, @Min(1) @Max(4) int level, @Size(max = 3000) String myAttempt) {}

    public record HintResponse(int level, String levelName, String hint, boolean aiUsed, String notice) {}

    public record CodeReviewRequest(@NotBlank @Size(max = 12000) String code, @Size(max = 300) String problem) {}

    public record QuizGenRequest(@NotBlank @Size(max = 120) String topic, @Min(3) @Max(8) int count) {}

    /** Generated practice questions include the answer (practice only: no XP, so no need to hide it). */
    public record GeneratedQuestion(String prompt, String code, List<String> options, int answer, String explanation) {}

    public record QuizGenResponse(String topic, List<GeneratedQuestion> questions, boolean aiUsed, String source, String notice) {}

    public record BriefingResponse(String kind, String text, boolean aiUsed) {}

    public record Diagnostics(boolean enabled, boolean usable, String primaryModel, List<String> fallbackModels,
                              int dailyLimitPerUser, int myUsageToday, long allUsageToday, long cacheEntries,
                              String lastError, Instant lastErrorAt, List<JsonNode> recentCalls) {}

    public record TestResult(boolean ok, String model, long latencyMs, String reply, String error) {}

    public record ExplainScore(BigDecimal score, String feedback, boolean aiUsed) {}
}
