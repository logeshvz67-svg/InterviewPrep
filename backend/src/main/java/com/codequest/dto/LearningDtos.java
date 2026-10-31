package com.codequest.dto;

import com.codequest.dto.GameDtos.XpAward;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class LearningDtos {
    private LearningDtos() {}

    public record WorldSummary(String code, String name, String description, int sortOrder, int lessonsWritten,
                               int lessonsCompleted, int outlineCount) {}

    /** status: LOCKED | AVAILABLE | IN_PROGRESS | COMPLETED | OUTLINE (planned, not written yet). */
    public record LessonCard(String slug, String title, String section, int order, int estimatedMinutes, String status,
                             BigDecimal bestQuizPercent, boolean mastered, List<String> companies, String summary,
                             boolean relevant) {}

    public record SectionGroup(String section, String name, int total, int completed, List<LessonCard> lessons) {}

    public record WorldDetail(String code, String name, String description, int unlockPercent, List<SectionGroup> sections) {}

    /** Quiz question WITHOUT the answer. MATCH options = {left:[...], right:[...shuffled]}. */
    public record QuestionView(String id, String type, String prompt, String code, JsonNode options) {}

    public record LessonView(String slug, String title, String world, String section, int order, int estimatedMinutes,
                             boolean locked, String lockReason, JsonNode content, List<QuestionView> quiz,
                             List<String> stepsDone, String status, BigDecimal bestQuizPercent, boolean quizPassed,
                             String topicStatus, String explainBack, String prevSlug, String nextSlug, List<String> companies) {}

    public record StepRequest(@NotBlank @Pattern(regexp = "why|analogy|visual|explanation|tinyCode|tryIt|predict|quiz|explainBack") String step) {}

    /** answers: question id -> chosen index (number), typed text, or array of right-hand texts (MATCH). */
    public record QuizSubmission(@NotNull Map<@NotBlank String, JsonNode> answers) {}

    public record QuestionResult(String id, boolean correct, JsonNode given, String correctAnswer, String explanation) {}

    public record QuizResult(String kind, int correct, int total, BigDecimal percent, boolean passed, int passPercent,
                             List<QuestionResult> results, List<XpAward> xp, boolean lessonCompleted, String unlockedNextSlug,
                             String message) {}

    public record ExplainBackRequest(@NotBlank @Size(min = 20, max = 4000) String text,
                                     @NotNull @Size(max = 10) List<@Min(0) @Max(9) Integer> checkedRubric,
                                     boolean useAi) {}

    public record ExplainBackResult(BigDecimal score, String feedback, String modelAnswer, boolean aiUsed, XpAward xp, String topicStatus) {}

    public record FlashcardView(Long id, String front, String back, String topic, String sourceType, int intervalDays,
                                int repetitions, LocalDate nextReviewOn, boolean suspended) {}

    public record ReviewRequest(@NotBlank @Pattern(regexp = "AGAIN|HARD|GOOD|EASY") String grade) {}

    public record ReviewResult(FlashcardView card, int reviewedToday, int dueLeft) {}

    public record ManualCardRequest(@NotBlank @Size(max = 500) String front, @NotBlank @Size(max = 2000) String back,
                                    @Size(max = 160) String topic) {}

    public record WarmupView(List<FlashcardView> cards, List<QuestionView> questions, Map<String, String> questionLesson,
                             int dueTotal, int reviewedToday, boolean rewardedToday) {}

    public record WarmupAnswerRequest(@NotNull Map<@NotBlank String, JsonNode> answers) {}

    public record RecallComplete(XpAward xp, int reviewedToday, String message) {}
}
