package com.codequest.service;

import com.codequest.domain.QuestTrigger;
import com.codequest.dto.CurriculumDtos.*;
import com.codequest.entity.JavaTopic;
import com.codequest.entity.JavaTopicProgress;
import com.codequest.entity.TopicStatus;
import com.codequest.exception.ApiException;
import com.codequest.mapper.Mappers;
import com.codequest.repository.CompanyTagRepository;
import com.codequest.repository.JavaTopicProgressRepository;
import com.codequest.repository.JavaTopicRepository;
import com.codequest.repository.WorldRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CurriculumService {
    private final JavaTopicRepository topics;
    private final JavaTopicProgressRepository progress;
    private final WorldRepository worlds;
    private final CompanyTagRepository companies;
    private final QuestService quests;
    private final StreakService streaks;
    private final TimeService time;

    public CurriculumService(JavaTopicRepository topics, JavaTopicProgressRepository progress, WorldRepository worlds,
                             CompanyTagRepository companies, QuestService quests, StreakService streaks, TimeService time) {
        this.topics = topics;
        this.progress = progress;
        this.worlds = worlds;
        this.companies = companies;
        this.quests = quests;
        this.streaks = streaks;
        this.time = time;
    }

    /**
     * readOnly = true: tells Hibernate it can skip dirty checking (faster) and keeps the session open
     * so lazy fields (topic.companyCodes) can still load while we build DTOs.
     */
    @Transactional(readOnly = true)
    public CurriculumView javaCurriculum(Long userId, String companyFilter) {
        Map<Long, JavaTopicProgress> mine = progress.findByUserId(userId).stream()
                .collect(Collectors.toMap(p -> p.getTopic().getId(), Function.identity()));
        Map<String, List<TopicView>> bySection = new LinkedHashMap<>();
        Map<String, String> sectionNames = new LinkedHashMap<>();
        for (JavaTopic t : topics.findAllByOrderBySectionAscSortOrderAsc()) {
            if (companyFilter != null && !companyFilter.isBlank() && !t.relevantTo(companyFilter)) continue;
            bySection.computeIfAbsent(t.getSection(), k -> new ArrayList<>()).add(Mappers.toView(t, mine.get(t.getId())));
            sectionNames.putIfAbsent(t.getSection(), t.getSectionName());
        }
        List<SectionView> sections = bySection.entrySet().stream().map(e -> new SectionView(e.getKey(), sectionNames.get(e.getKey()),
                e.getValue().size(), (int) e.getValue().stream().filter(v -> isUnderstood(v.status())).count(), e.getValue())).toList();
        int total = sections.stream().mapToInt(SectionView::total).sum();
        int done = sections.stream().mapToInt(SectionView::understoodOrBetter).sum();
        return new CurriculumView(companyFilter, total, done, sections);
    }

    /**
     * Manual status change. MASTERED is refused: it must be earned with a "Prove it" test (90%+).
     * Moving a topic forward counts for the "Move a Java topic forward" quest and the streak.
     */
    @Transactional
    public TopicView updateStatus(Long userId, Long topicId, StatusUpdateRequest req) {
        if (req.status() == TopicStatus.MASTERED) {
            throw ApiException.badRequest("Mastered is earned by passing a Prove-it test (90%+), not by clicking. You've got this!");
        }
        JavaTopic topic = topics.findById(topicId).orElseThrow(() -> ApiException.notFound("Topic"));
        JavaTopicProgress p = progress.findByUserIdAndTopicId(userId, topicId).orElseGet(() -> {
            JavaTopicProgress n = new JavaTopicProgress();
            n.setUserId(userId);
            n.setTopic(topic);
            return n;
        });
        if (p.getStatus() == TopicStatus.MASTERED) {
            throw ApiException.badRequest("This topic is already Mastered and lives in spaced review now.");
        }
        boolean forward = req.status().ordinal() > p.getStatus().ordinal();
        p.setStatus(req.status());
        p.setLastReviewed(time.now());
        progress.save(p);
        if (forward) {
            quests.advance(userId, QuestTrigger.TOPIC_PROGRESS);
            streaks.recordActivity(userId);
        }
        return Mappers.toView(topic, p);
    }

    /** 0..1 Java score for readiness, using only topics relevant to the company. */
    @Transactional(readOnly = true)
    public BigDecimal javaScore(Long userId, String companyCode) {
        Map<Long, TopicStatus> mine = progress.findByUserId(userId).stream()
                .collect(Collectors.toMap(p -> p.getTopic().getId(), JavaTopicProgress::getStatus));
        List<JavaTopic> relevant = topics.findAllByOrderBySectionAscSortOrderAsc().stream().filter(t -> t.relevantTo(companyCode)).toList();
        if (relevant.isEmpty()) return BigDecimal.ZERO;
        double sum = relevant.stream().mapToDouble(t -> mine.getOrDefault(t.getId(), TopicStatus.NOT_STARTED).weight()).sum();
        return BigDecimal.valueOf(sum / relevant.size()).setScale(4, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public List<WorldView> worlds() {
        return worlds.findAllByOrderBySortOrder().stream()
                .map(w -> new WorldView(w.getCode(), w.getName(), w.getDescription(), w.getSortOrder(), w.getUnlockQuizPercent())).toList();
    }

    @Transactional(readOnly = true)
    public List<CompanyView> companies() {
        return companies.findAllByOrderBySortOrder().stream().map(c -> new CompanyView(c.getCode(), c.getName())).toList();
    }

    private static boolean isUnderstood(TopicStatus s) {
        return s.ordinal() >= TopicStatus.UNDERSTOOD.ordinal();
    }
}
