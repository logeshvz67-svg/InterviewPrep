package com.codequest.service;

import com.codequest.domain.QuestTrigger;
import com.codequest.domain.XpAction;
import com.codequest.dto.GameDtos.XpAward;
import com.codequest.dto.GymDtos.*;
import com.codequest.dto.PageResponse;
import com.codequest.entity.Mistake;
import com.codequest.entity.MistakeCategory;
import com.codequest.exception.ApiException;
import com.codequest.mapper.Mappers;
import com.codequest.repository.MistakeCategoryRepository;
import com.codequest.repository.MistakeRepository;
import com.codequest.repository.ProblemSolvedRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Mistake Notebook: recording a mistake honestly is rewarded (+10), never punished. */
@Service
public class MistakeService {
    private final MistakeRepository mistakes;
    private final MistakeCategoryRepository categories;
    private final ProblemSolvedRepository problems;
    private final XpService xp;
    private final QuestService quests;
    private final TimeService time;

    public MistakeService(MistakeRepository mistakes, MistakeCategoryRepository categories, ProblemSolvedRepository problems,
                          XpService xp, QuestService quests, TimeService time) {
        this.mistakes = mistakes;
        this.categories = categories;
        this.problems = problems;
        this.xp = xp;
        this.quests = quests;
        this.time = time;
    }

    public record Saved(MistakeView mistake, XpAward xp) {}

    @Transactional
    public Saved create(Long userId, MistakeRequest req) {
        if (!categories.existsByCode(req.categoryCode())) throw ApiException.badRequest("Unknown category " + req.categoryCode());
        if (req.problemId() != null && problems.findByIdAndUserId(req.problemId(), userId).isEmpty()) {
            throw ApiException.notFound("Problem");
        }
        Mistake m = new Mistake();
        m.setUserId(userId);
        m.setCategoryCode(req.categoryCode());
        m.setProblemId(req.problemId());
        m.setTopic(req.topic());
        m.setWhatHappened(req.whatHappened().trim());
        m.setLessonLearned(req.lessonLearned().trim());
        mistakes.save(m);
        XpAward award = xp.award(userId, XpAction.MISTAKE_RECORDED, "mistake:" + m.getId(), req.categoryCode());
        quests.advance(userId, QuestTrigger.MISTAKE_RECORDED);
        return new Saved(Mappers.toView(m, categoryMap()), award);
    }

    @Transactional(readOnly = true)
    public PageResponse<MistakeView> list(Long userId, Boolean resolved, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var result = resolved == null ? mistakes.findByUserId(userId, pageable) : mistakes.findByUserIdAndResolved(userId, resolved, pageable);
        Map<String, MistakeCategory> cats = categoryMap();
        return PageResponse.of(result, m -> Mappers.toView(m, cats));
    }

    @Transactional
    public MistakeView setResolved(Long userId, Long id, boolean resolved) {
        Mistake m = mistakes.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Mistake"));
        m.setResolved(resolved);
        m.setResolvedAt(resolved ? time.now() : null);
        return Mappers.toView(m, categoryMap());
    }

    @Transactional
    public void delete(Long userId, Long id) {
        mistakes.delete(mistakes.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Mistake")));
    }

    /** Top recurring mistake types in the last 7 days, with a tip for each. */
    @Transactional(readOnly = true)
    public List<MistakeTrend> weeklyTop(Long userId) {
        Map<String, MistakeCategory> cats = categoryMap();
        return mistakes.countByCategorySince(userId, time.now().minus(Duration.ofDays(7))).stream().limit(3)
                .map(c -> {
                    MistakeCategory cat = cats.get(c.getCategoryCode());
                    return new MistakeTrend(c.getCategoryCode(), cat == null ? c.getCategoryCode() : cat.getName(), c.getTotal(),
                            cat == null ? "" : cat.getTip());
                }).toList();
    }

    @Transactional(readOnly = true)
    public List<MistakeCategoryView> categories() {
        return categories.findAll().stream().map(Mappers::toView).toList();
    }

    private Map<String, MistakeCategory> categoryMap() {
        return categories.findAll().stream().collect(Collectors.toMap(MistakeCategory::getCode, Function.identity()));
    }
}
