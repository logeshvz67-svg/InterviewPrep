package com.codequest.dto;

import com.codequest.entity.TopicStatus;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class CurriculumDtos {
    private CurriculumDtos() {}

    public record TopicView(Long id, String section, int sortOrder, String slug, String title, String summary,
                            boolean rebuild, String infosysRelevance, List<String> companies,
                            TopicStatus status, BigDecimal masteryScore, Instant lastReviewed) {}

    public record SectionView(String section, String name, int total, int understoodOrBetter, List<TopicView> topics) {}

    public record CurriculumView(String companyFilter, int totalTopics, int understoodOrBetter, List<SectionView> sections) {}

    public record StatusUpdateRequest(@NotNull TopicStatus status) {}

    public record WorldView(String code, String name, String description, int sortOrder, int unlockQuizPercent) {}

    public record CompanyView(String code, String name) {}
}
