package com.codequest.service;

import com.codequest.domain.ReadinessCalculator;
import com.codequest.domain.ReadinessCalculator.Component;
import com.codequest.dto.DashboardDtos.*;
import com.codequest.dto.FocusDtos.CommitmentView;
import com.codequest.dto.FocusDtos.FocusView;
import com.codequest.dto.FocusDtos.NextAction;
import com.codequest.dto.GameDtos.QuestView;
import com.codequest.dto.GameDtos.StreakView;
import com.codequest.dto.GymDtos.RevisitView;
import com.codequest.entity.Profile;
import com.codequest.entity.Roadmap;
import com.codequest.mapper.Mappers;
import com.codequest.repository.RoadmapRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Builds the Command Center in ONE request (fewer round-trips = faster on a slow free server). */
@Service
public class DashboardService {
    private final ProfileService profiles;
    private final StreakService streaks;
    private final QuestService quests;
    private final XpService xp;
    private final GymService gym;
    private final CurriculumService curriculum;
    private final FocusService focus;
    private final RoadmapRepository roadmaps;
    private final TimeService time;
    private final FlashcardService flashcards;
    private final PracticeService practice;
    private final ExamService exams;
    private final AchievementService achievements;
    private final ReportService reports;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final com.fasterxml.jackson.databind.ObjectMapper json;

    public DashboardService(ProfileService profiles, StreakService streaks, QuestService quests, XpService xp, GymService gym,
                            CurriculumService curriculum, FocusService focus, RoadmapRepository roadmaps, TimeService time,
                            FlashcardService flashcards, PracticeService practice, ExamService exams, AchievementService achievements,
                            ReportService reports, org.springframework.jdbc.core.JdbcTemplate jdbc, com.fasterxml.jackson.databind.ObjectMapper json) {
        this.flashcards = flashcards;
        this.practice = practice;
        this.exams = exams;
        this.achievements = achievements;
        this.reports = reports;
        this.jdbc = jdbc;
        this.json = json;
        this.profiles = profiles;
        this.streaks = streaks;
        this.quests = quests;
        this.xp = xp;
        this.gym = gym;
        this.curriculum = curriculum;
        this.focus = focus;
        this.roadmaps = roadmaps;
        this.time = time;
    }

    @Transactional
    public CommandCenter commandCenter(Long userId) {
        Profile p = profiles.load(userId);
        LocalDate today = time.today(userId);
        StreakView streak = streaks.view(userId);
        List<QuestView> todayQuests = quests.today(userId);
        FocusView active = focus.active(userId);
        int due = gym.dueRevisitCount(userId) + flashcards.dueCount(userId);

        Long daysToTest = p.getTestDate() != null && !p.getTestDate().isBefore(today)
                ? ChronoUnit.DAYS.between(today, p.getTestDate()) : null;

        Roadmap r = roadmaps.findFirstByUserIdAndActiveTrueOrderByIdDesc(userId).orElse(null);
        int week = r == null ? 0 : ProfileService.currentWeek(r, today);
        String weekTitle = r == null || r.getWeeks().isEmpty() ? null : r.getWeeks().get(week - 1).title();

        ReadinessView readiness = readiness(userId, p.getPrimaryCompany());
        // Daily snapshot for the "readiness over time" chart + the Infosys Ready badge.
        reports.snapshotReadiness(userId, p.getPrimaryCompany(), json.valueToTree(java.util.Map.of("score", readiness.score())));
        Object th = p.getSettings().get("readyThreshold");
        double threshold = th instanceof Number n ? n.doubleValue() : 75;
        if (p.getPrimaryCompany().startsWith("INFOSYS")) achievements.evaluate(userId, readiness.score().doubleValue(), threshold);
        BigDecimal javaV = component(readiness, Component.JAVA);
        BigDecimal dsaV = component(readiness, Component.DSA);
        Map<String, Integer> self = p.getSelfAssessment();
        List<RadarPoint> radar = List.of(
                new RadarPoint("Java", self.get("JAVA"), toFive(javaV)),
                new RadarPoint("DSA", self.get("DSA"), toFive(dsaV)),
                new RadarPoint("SQL", self.get("SQL"), toFiveOrNull(component(readiness, Component.SQL), readiness, Component.SQL)),
                new RadarPoint("Spring", self.get("SPRING"), springMeasured(userId)),
                new RadarPoint("Communication", self.get("COMMUNICATION"), toFiveOrNull(component(readiness, Component.COMMUNICATION), readiness, Component.COMMUNICATION)));

        NextAction next = nextAction(userId, active, streak, todayQuests, weekTitle);
        return new CommandCenter(p.getCharacterName(), p.getAvatar(), p.getWhyStatement(), Mappers.toLevelView(p), streak,
                todayQuests, due, daysToTest, radar, readiness, active, next, xp.todayXp(userId), week, weekTitle,
                welcome(streak));
    }

    /** "Do this now": exactly ONE next step, so there is nothing to decide. */
    @Transactional
    public NextAction nextAction(Long userId) {
        Profile p = profiles.load(userId);
        Roadmap r = roadmaps.findFirstByUserIdAndActiveTrueOrderByIdDesc(userId).orElse(null);
        String weekTitle = r == null || r.getWeeks().isEmpty() ? null : r.getWeeks().get(ProfileService.currentWeek(r, time.today(userId)) - 1).title();
        return nextAction(userId, focus.active(userId), streaks.view(userId), quests.today(userId), p.isOnboardingComplete() ? weekTitle : null);
    }

    private NextAction nextAction(Long userId, FocusView active, StreakView streak, List<QuestView> todayQuests, String weekTitle) {
        if (active != null) {
            return new NextAction("FOCUS", "Finish your focus session", "Topic: " + active.topic(), "/focus");
        }
        if (streak.comebackPending()) {
            return new NextAction("COMEBACK", "Welcome back! Start with just 10 minutes", "Small steps count. No catching up needed.", "/focus");
        }
        List<RevisitView> due = gym.dueRevisits(userId);
        if (!due.isEmpty()) {
            return new NextAction("REVISIT", "Re-solve: " + due.get(0).problemTitle(), "From scratch, no peeking first. Round " + due.get(0).round(), "/gym/revisits");
        }
        for (QuestView q : todayQuests) {
            if (!q.completed()) return new NextAction("QUEST", q.title(), q.description(), routeFor(q.code()));
        }
        CommitmentView c = focus.commitmentFor(userId, false);
        for (var t : c.tasks()) {
            if (!t.done()) return new NextAction("COMMITMENT", t.text(), "You picked this yesterday.", "/focus");
        }
        String detail = weekTitle == null ? "Pick any topic you enjoy." : "This week: " + weekTitle;
        return new NextAction("FOCUS", "Start a 25-minute focus session", detail, "/focus");
    }

    private static String routeFor(String questCode) {
        return switch (questCode) {
            case "TOPIC_STEP" -> "/curriculum";
            case "LOG_PROBLEM" -> "/gym/log";
            case "RECORD_MISTAKE" -> "/gym/mistakes";
            case "REVISIT" -> "/gym/revisits";
            case "COMMIT" -> "/focus";
            default -> "/focus";
        };
    }

    @Transactional(readOnly = true)
    public ReadinessView readiness(Long userId, String companyCode) {
        Map<Component, BigDecimal> values = new EnumMap<>(Component.class);
        values.put(Component.JAVA, curriculum.javaScore(userId, companyCode));
        values.put(Component.DSA, gym.dsaScore(userId));
        values.put(Component.SQL, practice.sqlScore(userId));          // SQL/DBMS interview practice self-scores
        values.put(Component.APTITUDE, practice.aptitudeScore(userId)); // last 50 aptitude answers
        values.put(Component.MOCKS, mockScore(userId));                 // mock tests + mock interviews
        values.put(Component.COMMUNICATION, communicationScore(userId)); // last 10 speaking sessions
        // Anything still null = not practised yet: counts as 0 and is labelled "not tracked yet".
        var result = ReadinessCalculator.compute(companyCode, values);
        return new ReadinessView(companyCode, result.score(), result.trackedWeight(), result.components(),
                result.improveNext().name(), tip(result.improveNext()));
    }

    private BigDecimal mockScore(Long userId) {
        BigDecimal tests = exams.recentMockScore(userId);
        BigDecimal interviews = jdbc.queryForObject(
                "select avg(overall_score)/100 from (select overall_score from mock_interviews where user_id=? and status='COMPLETED' order by id desc limit 3) t",
                BigDecimal.class, userId);
        if (tests == null) return interviews;
        if (interviews == null) return tests;
        return tests.add(interviews).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal communicationScore(Long userId) {
        return jdbc.queryForObject(
                "select avg(overall_score)/100 from (select overall_score from speaking_sessions where user_id=? order by id desc limit 10) t",
                BigDecimal.class, userId);
    }

    private Integer springMeasured(Long userId) {
        Double v = jdbc.queryForObject("""
                select case when (select count(*) from spring_topics)=0 then null else
                  (select coalesce(sum(case status when 'LEARNING' then 0.25 when 'UNDERSTOOD' then 0.6 when 'CAN_EXPLAIN' then 1.0 else 0 end),0)
                   from spring_topic_progress where user_id=?) / (select count(*) from spring_topics) end""", Double.class, userId);
        return v == null ? null : (int) Math.round(v * 5);
    }

    private static Integer toFiveOrNull(BigDecimal v, ReadinessView r, Component c) {
        boolean tracked = r.components().stream().anyMatch(x -> x.component() == c && x.tracked());
        return tracked ? toFive(v) : null;
    }

    private static String tip(Component c) {
        return switch (c) {
            case JAVA -> "Move your next Java topic from Learning to Understood: read it, type the examples, explain it out loud.";
            case DSA -> "Open the Pattern Map and solve 2 easy problems in the recommended pattern.";
            case SQL -> "Practise 5 SQL questions in the Interview Colosseum (tick the key points you covered).";
            case APTITUDE -> "Do one 10-question aptitude set in the Aptitude Arena.";
            case MOCKS -> "Enter the exam pattern from the official careers page, then take a timed mock test.";
            case COMMUNICATION -> "Record a 2-minute self-introduction in the Communication Gym.";
        };
    }

    private static BigDecimal component(ReadinessView r, Component c) {
        return r.components().stream().filter(x -> x.component() == c).findFirst().map(ReadinessCalculator.ComponentScore::value).orElse(BigDecimal.ZERO);
    }

    private static Integer toFive(BigDecimal v) {
        return v.multiply(BigDecimal.valueOf(5)).setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private static String welcome(StreakView s) {
        if (s.comebackPending()) return "Welcome back! Life happens. No guilt, no catching up. Today, one small step is a win.";
        return switch (s.status()) {
            case NOT_STARTED -> "Your adventure starts today. One small step is enough.";
            case DONE_TODAY -> "You showed up today. That's what builds skills.";
            case KEEP_IT_GOING -> "Ready for today's step? Even 20 minutes keeps the streak alive.";
            case FREEZE_WILL_COVER -> "A streak freeze will cover the missed day once you study today.";
            case PAUSED -> "Your streak is paused, not lost forever. Start fresh with one small step.";
        };
    }
}
