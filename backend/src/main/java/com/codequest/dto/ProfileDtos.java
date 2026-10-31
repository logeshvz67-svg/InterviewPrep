package com.codequest.dto;

import com.codequest.domain.RoadmapWeek;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class ProfileDtos {
    private ProfileDtos() {}

    /** Onboarding: character creation + target + time budget + self-assessment (informational only). */
    public record OnboardingRequest(
            @NotBlank @Size(max = 60) String characterName,
            @NotBlank @Size(max = 40) String avatar,
            @NotBlank @Pattern(regexp = "INFOSYS_SP|INFOSYS_DSE|TCS|WIPRO|COGNIZANT|ACCENTURE|CAPGEMINI|PRODUCT",
                    message = "Unknown company") String primaryCompany,
            @NotNull @Size(max = 8) List<@NotBlank String> laterCompanies,
            @Future(message = "Test date must be in the future") LocalDate testDate,
            @NotBlank String timezone,
            @Min(10) @Max(600) int dailyMinutes,
            @Size(max = 1000) String whyStatement,
            @NotNull Map<@Pattern(regexp = "JAVA|DSA|SQL|SPRING|COMMUNICATION") String, @Min(1) @Max(5) Integer> selfAssessment) {}

    public record ProfileUpdateRequest(
            @NotBlank @Size(max = 60) String characterName,
            @NotBlank @Size(max = 40) String avatar,
            @NotBlank String timezone,
            @Min(10) @Max(600) int dailyMinutes,
            @Size(max = 1000) String whyStatement,
            boolean soundEnabled) {}

    public record SwitchCompanyRequest(@NotBlank String companyCode) {}

    public record TestDateRequest(@Future(message = "Test date must be in the future") LocalDate testDate) {}

    public record ProfileView(
            String characterName, String avatar, String primaryCompany, List<String> laterCompanies,
            LocalDate testDate, String timezone, int dailyMinutes, String whyStatement,
            Map<String, Integer> selfAssessment, boolean onboardingComplete, int totalXp, int gold, boolean soundEnabled,
            Map<String, Object> settings) {}

    public record RoadmapView(LocalDate startDate, LocalDate endDate, int totalWeeks, int currentWeek, List<RoadmapWeek> weeks) {}
}
