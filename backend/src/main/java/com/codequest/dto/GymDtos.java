package com.codequest.dto;

import com.codequest.entity.GymEnums.Difficulty;
import com.codequest.entity.GymEnums.HintLevel;
import com.codequest.entity.GymEnums.Platform;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.URL;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class GymDtos {
    private GymDtos() {}

    public record ProblemRequest(
            @NotBlank @Size(max = 200) String title,
            @URL(message = "Must be a valid link") @Size(max = 500) String link,
            @NotNull Platform platform,
            @NotNull Difficulty difficulty,
            @NotEmpty(message = "Pick at least one pattern") @Size(max = 5) List<@NotBlank String> patternCodes,
            @Min(1) @Max(600) int timeTakenMinutes,
            @Min(1) @Max(50) int attempts,
            @NotNull HintLevel hintLevel,
            boolean attemptedBeforeHint,
            @Min(1) @Max(5) int confidence,
            @NotBlank @Size(min = 20, max = 5000, message = "Write your approach in your own words (at least 20 characters)") String approach,
            @Size(max = 20000) String code,
            @PastOrPresent LocalDate solvedOn,
            @Size(max = 120) String bankSlug) {}

    public record PatternView(String code, String name, String description, boolean spCore) {}

    public record ProblemView(Long id, String title, String link, Platform platform, Difficulty difficulty,
                              List<PatternView> patterns, int timeTakenMinutes, int attempts, HintLevel hintLevel,
                              boolean attemptedBeforeHint, int confidence, String approach, String code,
                              LocalDate solvedOn, Instant createdAt) {}

    public record ProblemSaved(ProblemView problem, GameDtos.XpAward approachXp, GameDtos.XpAward attemptXp,
                               List<LocalDate> revisitDates) {}

    public record PatternStat(String code, String name, boolean spCore, int solved, double avgConfidence,
                              double unaidedRate, String strength) {}

    public record PatternMap(List<PatternStat> patterns, PatternView recommendedNext, String recommendationReason) {}

    public record RevisitView(Long id, Long problemId, String problemTitle, String link, int round, LocalDate dueDate, boolean overdue) {}

    public record RevisitCompleteRequest(boolean solvedUnaided, @Size(max = 2000) String notes) {}

    public record MistakeRequest(
            @NotBlank String categoryCode,
            Long problemId,
            @Size(max = 120) String topic,
            @NotBlank @Size(min = 10, max = 2000) String whatHappened,
            @NotBlank @Size(min = 5, max = 2000) String lessonLearned) {}

    public record MistakeView(Long id, String categoryCode, String categoryName, Long problemId, String topic,
                              String whatHappened, String lessonLearned, boolean resolved, Instant createdAt) {}

    public record MistakeCategoryView(String code, String name, String tip) {}

    public record MistakeTrend(String categoryCode, String categoryName, long count, String tip) {}
}
