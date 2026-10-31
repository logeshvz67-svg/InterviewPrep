package com.codequest.mapper;

import com.codequest.domain.LevelInfo;
import com.codequest.dto.AuthDtos.UserView;
import com.codequest.dto.CurriculumDtos.TopicView;
import com.codequest.dto.FocusDtos.DistractionView;
import com.codequest.dto.FocusDtos.FocusView;
import com.codequest.dto.GameDtos.LevelView;
import com.codequest.dto.GameDtos.QuestView;
import com.codequest.dto.GameDtos.XpEventView;
import com.codequest.dto.GymDtos.*;
import com.codequest.dto.ProfileDtos.ProfileView;
import com.codequest.entity.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Entity -> DTO conversions in one place. Written by hand (no MapStruct) so every field copy is visible.
 * Note: methods that touch lazy fields (patterns, topic, quest) must be called inside a transaction.
 */
public final class Mappers {
    private Mappers() {}

    public static UserView toView(User u) {
        return new UserView(u.getId(), u.getEmail(), u.getDisplayName(), u.getRole());
    }

    public static ProfileView toView(Profile p) {
        return new ProfileView(p.getCharacterName(), p.getAvatar(), p.getPrimaryCompany(), List.copyOf(p.getLaterCompanies()),
                p.getTestDate(), p.getTimezone(), p.getDailyMinutes(), p.getWhyStatement(), Map.copyOf(p.getSelfAssessment()),
                p.isOnboardingComplete(), p.getTotalXp(), p.getGold(), p.isSoundEnabled(), Map.copyOf(p.getSettings()));
    }

    public static LevelView toLevelView(Profile p) {
        LevelInfo l = LevelInfo.of(p.getTotalXp());
        return new LevelView(l.totalXp(), l.level(), l.levelStartXp(), l.nextLevelXp(), l.levelProgressPercent(),
                l.rank(), l.nextRank(), l.nextRankXp(), p.getGold());
    }

    public static XpEventView toView(XpEvent e) {
        return new XpEventView(e.getAction(), e.getAmount(), e.getGold(), e.getNote(), e.getLocalDate(), e.getCreatedAt());
    }

    public static QuestView toView(UserQuest uq) {
        Quest q = uq.getQuest();
        return new QuestView(uq.getId(), q.getCode(), q.getTitle(), q.getDescription(),
                Math.min(uq.getProgress(), q.getTarget()), q.getTarget(), q.getGoldReward(), uq.isCompleted());
    }

    public static PatternView toView(Pattern p) {
        return new PatternView(p.getCode(), p.getName(), p.getDescription(), p.isSpCore());
    }

    public static ProblemView toView(ProblemSolved p) {
        List<PatternView> patterns = p.getPatterns().stream()
                .sorted(Comparator.comparingInt(Pattern::getSortOrder)).map(Mappers::toView).toList();
        return new ProblemView(p.getId(), p.getTitle(), p.getLink(), p.getPlatform(), p.getDifficulty(), patterns,
                p.getTimeTakenMinutes(), p.getAttempts(), p.getHintLevel(), p.isAttemptedBeforeHint(), p.getConfidence(),
                p.getApproach(), p.getCode(), p.getSolvedOn(), p.getCreatedAt());
    }

    public static RevisitView toView(ProblemRevisit r, LocalDate today) {
        return new RevisitView(r.getId(), r.getProblem().getId(), r.getProblem().getTitle(), r.getProblem().getLink(),
                r.getRound(), r.getDueDate(), r.getDueDate().isBefore(today));
    }

    public static MistakeView toView(Mistake m, Map<String, MistakeCategory> categories) {
        MistakeCategory c = categories.get(m.getCategoryCode());
        return new MistakeView(m.getId(), m.getCategoryCode(), c == null ? m.getCategoryCode() : c.getName(), m.getProblemId(),
                m.getTopic(), m.getWhatHappened(), m.getLessonLearned(), m.isResolved(), m.getCreatedAt());
    }

    public static MistakeCategoryView toView(MistakeCategory c) {
        return new MistakeCategoryView(c.getCode(), c.getName(), c.getTip());
    }

    public static FocusView toView(FocusSession f, Instant serverNow) {
        return new FocusView(f.getId(), f.getMode(), f.getTopic(), f.getPlannedMinutes(), f.getStartedAt(),
                f.getStartedAt().plusSeconds(f.getPlannedMinutes() * 60L), f.getStatus(), f.getActualMinutes(), serverNow);
    }

    public static DistractionView toView(DistractionEntry d) {
        return new DistractionView(d.getId(), d.getCategory(), d.getNote(), d.getLocalDate(), d.getLocalHour(), d.getCreatedAt());
    }

    public static TopicView toView(JavaTopic t, JavaTopicProgress p) {
        return new TopicView(t.getId(), t.getSection(), t.getSortOrder(), t.getSlug(), t.getTitle(), t.getSummary(),
                t.isRebuild(), t.getInfosysRelevance(), t.getCompanyCodes().stream().sorted().toList(),
                p == null ? TopicStatus.NOT_STARTED : p.getStatus(),
                p == null ? BigDecimal.ZERO : p.getMasteryScore(),
                p == null ? null : p.getLastReviewed());
    }
}
