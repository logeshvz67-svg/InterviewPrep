package com.codequest.dto;

import com.codequest.entity.GymEnums.DistractionCategory;
import com.codequest.entity.GymEnums.FocusMode;
import com.codequest.entity.GymEnums.FocusStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class FocusDtos {
    private FocusDtos() {}

    public record StartFocusRequest(@NotNull FocusMode mode, @NotBlank @Size(max = 120) String topic,
                                    @Min(5) @Max(90) int plannedMinutes) {}

    /** serverNow lets the browser fix its countdown if the device clock is off. */
    public record FocusView(Long id, FocusMode mode, String topic, int plannedMinutes, Instant startedAt,
                            Instant endsAt, FocusStatus status, Integer actualMinutes, Instant serverNow) {}

    public record FocusCompleted(FocusView session, GameDtos.XpAward xp, boolean streakCounted) {}

    public record FocusStats(int days, int totalMinutes, Map<String, Integer> minutesByTopic, Map<LocalDate, Integer> minutesByDay) {}

    public record DistractionRequest(@NotNull DistractionCategory category, @Size(max = 300) String note) {}

    public record DistractionView(Long id, DistractionCategory category, String note, LocalDate localDate, int localHour, Instant createdAt) {}

    public record DistractionInsights(int days, int total, Map<DistractionCategory, Long> byCategory,
                                      Map<Integer, Long> byHour, String topCategory, Integer worstHour, String tip) {}

    public record CommitmentTask(@NotBlank @Size(max = 160) String text, boolean done) {}

    public record CommitmentRequest(@NotNull @Size(min = 1, max = 3, message = "Pick 1 to 3 tasks") List<@Valid CommitmentTask> tasks) {}

    public record CommitmentView(LocalDate forDate, List<CommitmentTask> tasks) {}

    public record CommitmentSaved(CommitmentView commitment, GameDtos.XpAward xp) {}

    public record NextAction(String type, String title, String detail, String route) {}
}
