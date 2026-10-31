package com.codequest.service;

import com.codequest.content.ContentCatalog;
import com.codequest.domain.QuestTrigger;
import com.codequest.domain.QuizGrader;
import com.codequest.domain.XpAction;
import com.codequest.dto.GameDtos.XpAward;
import com.codequest.dto.LearningDtos.*;
import com.codequest.entity.*;
import com.codequest.exception.ApiException;
import com.codequest.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Worlds, lessons and quizzes.
 * Teaching flow: Why -> Analogy -> Visual -> Explanation -> Tiny code -> Try it (type it) -> Predict -> Quiz -> Explain it back.
 * Gate: a lesson unlocks when the previous lesson in its section is completed (quiz >= 80%) — or you can
 * always take a "Prove it" test (90%+) to show you already know a topic. Content is never auto-skipped
 * from a self-assessment.
 */
@Service
public class LessonService {
    public static final int PASS_PERCENT = 80;
    public static final int PROVE_IT_PERCENT = 90;
    /** Steps you must actually do (not just read) before a lesson counts as complete. */
    public static final List<String> REQUIRED_STEPS = List.of("tryIt", "predict");

    private final LessonRepository lessons;
    private final QuizQuestionRepository questions;
    private final LessonProgressRepository progress;
    private final QuizAttemptRepository attempts;
    private final WorldRepository worlds;
    private final JavaTopicRepository topics;
    private final JavaTopicProgressRepository topicProgress;
    private final FlashcardService flashcards;
    private final XpService xp;
    private final QuestService quests;
    private final StreakService streaks;
    private final TimeService time;
    private final ContentCatalog catalog;
    private final ObjectMapper json;

    public LessonService(LessonRepository lessons, QuizQuestionRepository questions, LessonProgressRepository progress,
                         QuizAttemptRepository attempts, WorldRepository worlds, JavaTopicRepository topics,
                         JavaTopicProgressRepository topicProgress, FlashcardService flashcards, XpService xp,
                         QuestService quests, StreakService streaks, TimeService time, ContentCatalog catalog, ObjectMapper json) {
        this.lessons = lessons;
        this.questions = questions;
        this.progress = progress;
        this.attempts = attempts;
        this.worlds = worlds;
        this.topics = topics;
        this.topicProgress = topicProgress;
        this.flashcards = flashcards;
        this.xp = xp;
        this.quests = quests;
        this.streaks = streaks;
        this.time = time;
        this.catalog = catalog;
        this.json = json;
    }

    // ------------------------------------------------------------------ worlds

    @Transactional(readOnly = true)
    public List<WorldSummary> worlds(Long userId) {
        Map<Long, LessonProgress> mine = myProgress(userId);
        Map<String, List<Lesson>> byWorld = lessons.findAllByOrderByWorldCodeAscSectionAscSortOrderAsc().stream()
                .collect(Collectors.groupingBy(Lesson::getWorldCode));
        return worlds.findAllByOrderBySortOrder().stream().map(w -> {
            List<Lesson> ls = byWorld.getOrDefault(w.getCode(), List.of());
            int done = (int) ls.stream().filter(l -> isDone(mine.get(l.getId()))).count();
            return new WorldSummary(w.getCode(), w.getName(), w.getDescription(), w.getSortOrder(), ls.size(), done,
                    catalog.outlineFor(w.getCode()).size());
        }).toList();
    }

    @Transactional(readOnly = true)
    public WorldDetail world(Long userId, String code, String company) {
        World w = worlds.findAllByOrderBySortOrder().stream().filter(x -> x.getCode().equals(code)).findFirst()
                .orElseThrow(() -> ApiException.notFound("World"));
        Map<Long, LessonProgress> mine = myProgress(userId);
        Map<Long, TopicStatus> tStatus = topicStatuses(userId);
        List<Lesson> all = lessons.findByWorldCodeOrderBySectionAscSortOrderAsc(code);
        Map<String, List<Lesson>> bySection = all.stream().collect(Collectors.groupingBy(Lesson::getSection, LinkedHashMap::new, Collectors.toList()));
        Map<String, String> sectionNames = sectionNames();

        List<SectionGroup> groups = new ArrayList<>();
        for (var e : bySection.entrySet()) {
            List<LessonCard> cards = new ArrayList<>();
            // Lessons for other companies stay visible (dimmed): hiding them would break the unlock order.
            for (Lesson l : e.getValue()) {
                cards.add(card(l, mine.get(l.getId()), isLocked(l, all, mine), tStatus.get(l.getTopicId()), l.relevantTo(company)));
            }
            int done = (int) cards.stream().filter(c -> "COMPLETED".equals(c.status())).count();
            groups.add(new SectionGroup(e.getKey(), sectionNames.getOrDefault(e.getKey(), e.getKey()), cards.size(), done, cards));
        }
        // Planned lessons that aren't written yet are shown as an outline, so the whole map is visible.
        List<LessonCard> outline = new ArrayList<>();
        int i = 1;
        for (JsonNode o : catalog.outlineFor(code)) {
            outline.add(new LessonCard(null, o.path("title").asText(), "OUTLINE", i++, 0, "OUTLINE", BigDecimal.ZERO, false,
                    List.of(), o.path("summary").asText(), true));
        }
        if (!outline.isEmpty()) groups.add(new SectionGroup("OUTLINE", "Coming next (outline)", outline.size(), 0, outline));
        return new WorldDetail(w.getCode(), w.getName(), w.getDescription(), w.getUnlockQuizPercent(), groups);
    }

    private LessonCard card(Lesson l, LessonProgress p, boolean locked, TopicStatus ts, boolean relevant) {
        String status = p == null ? (locked ? "LOCKED" : "AVAILABLE") : p.isCompleted() ? "COMPLETED" : "IN_PROGRESS";
        return new LessonCard(l.getSlug(), l.getTitle(), l.getSection(), l.getSortOrder(), l.getEstimatedMinutes(), status,
                p == null ? BigDecimal.ZERO : p.getBestQuizPercent(), ts == TopicStatus.MASTERED,
                l.getCompanyCodes().stream().sorted().toList(), firstSentence(l.getContent().path("why").asText("")), relevant);
    }

    static String firstSentence(String md) {
        String plain = md.replaceAll("[*_`#>]", "").replaceAll("\\s+", " ").trim();
        int dot = plain.indexOf(". ");
        return dot > 0 && dot < 220 ? plain.substring(0, dot + 1) : plain.length() > 220 ? plain.substring(0, 217) + "…" : plain;
    }

    // ------------------------------------------------------------------ one lesson

    @Transactional(readOnly = true)
    public LessonView lesson(Long userId, String slug) {
        Lesson l = load(slug);
        List<Lesson> siblings = lessons.findByWorldCodeOrderBySectionAscSortOrderAsc(l.getWorldCode()).stream()
                .filter(x -> x.getSection().equals(l.getSection())).toList();
        Map<Long, LessonProgress> mine = myProgress(userId);
        LessonProgress p = mine.get(l.getId());
        boolean locked = isLocked(l, siblings, mine);
        int idx = indexOf(siblings, l);
        String prev = idx > 0 ? siblings.get(idx - 1).getSlug() : null;
        String next = idx >= 0 && idx < siblings.size() - 1 ? siblings.get(idx + 1).getSlug() : null;
        TopicStatus ts = l.getTopicId() == null ? null : topicStatuses(userId).get(l.getTopicId());
        String reason = locked ? "Pass the quiz of \"" + siblings.get(idx - 1).getTitle() + "\" (" + PASS_PERCENT
                + "%+) to unlock this lesson. Already know it? Take the Prove-it test." : null;
        return new LessonView(l.getSlug(), l.getTitle(), l.getWorldCode(), l.getSection(), l.getSortOrder(), l.getEstimatedMinutes(),
                locked, reason, locked ? null : l.getContent(), locked ? List.of() : publicQuestions(l.getId()),
                p == null ? List.of() : p.getStepsDone(), p == null ? "NOT_STARTED" : p.getStatus().name(),
                p == null ? BigDecimal.ZERO : p.getBestQuizPercent(), p != null && p.getQuizPassedAt() != null,
                ts == null ? null : ts.name(), p == null ? null : p.getExplainBack(), prev, next,
                l.getCompanyCodes().stream().sorted().toList());
    }

    @Transactional
    public LessonView markStep(Long userId, String slug, StepRequest req) {
        Lesson l = load(slug);
        assertUnlocked(userId, l);
        LessonProgress p = progressFor(userId, l);
        if (!p.getStepsDone().contains(req.step())) {
            List<String> steps = new ArrayList<>(p.getStepsDone());
            steps.add(req.step());
            p.setStepsDone(steps); // new list so Hibernate sees the JSON change
        }
        moveTopicAtLeast(userId, l, TopicStatus.LEARNING);
        if (!p.isCompleted() && p.getQuizPassedAt() != null && p.getStepsDone().containsAll(REQUIRED_STEPS)) {
            completeLesson(userId, l, p, "LESSON");
        }
        streaks.recordActivity(userId);
        return lesson(userId, slug);
    }

    /** Questions for the Prove-it test: every quiz question of the lesson, in a shuffled order. */
    @Transactional(readOnly = true)
    public List<QuestionView> proveItQuestions(Long userId, String slug) {
        Lesson l = load(slug);
        List<QuestionView> qs = new ArrayList<>(publicQuestions(l.getId()));
        Collections.shuffle(qs, new Random(userId * 31 + l.getId()));
        return qs;
    }

    @Transactional
    public QuizResult submitQuiz(Long userId, String slug, QuizSubmission sub, boolean proveIt) {
        Lesson l = load(slug);
        if (!proveIt) assertUnlocked(userId, l);
        List<QuizQuestion> qs = questions.findByLessonIdOrderBySortOrder(l.getId());
        if (qs.isEmpty()) throw ApiException.badRequest("This lesson has no quiz yet");
        boolean firstAttempt = attempts.findByUserIdAndLessonIdOrderByCreatedAtDesc(userId, l.getId()).isEmpty();

        List<QuestionResult> results = new ArrayList<>();
        ArrayNode details = json.createArrayNode();
        List<XpAward> awards = new ArrayList<>();
        int correct = 0;
        for (QuizQuestion q : qs) {
            JsonNode given = sub.answers().get(q.getExtId());
            boolean ok = QuizGrader.isCorrect(q.getType(), q.getAnswer(), given);
            if (ok) correct++;
            results.add(new QuestionResult(q.getExtId(), ok, given, displayAnswer(q), q.getExplanation()));
            ObjectNode d = details.addObject();
            d.put("id", q.getExtId());
            d.put("questionId", q.getId());
            d.put("correct", ok);
            d.set("given", given);
            if (ok && firstAttempt && !proveIt) {
                XpAward a = xp.award(userId, XpAction.FIRST_ATTEMPT_CORRECT, "q:" + q.getId(), l.getTitle());
                if (a.awarded()) awards.add(a);
            }
        }
        BigDecimal pct = QuizGrader.percent(correct, qs.size());
        int need = proveIt ? PROVE_IT_PERCENT : PASS_PERCENT;
        boolean passed = pct.compareTo(BigDecimal.valueOf(need)) >= 0;

        QuizAttempt att = new QuizAttempt();
        att.setUserId(userId);
        att.setLessonId(l.getId());
        att.setKind(proveIt ? "PROVE_IT" : "LESSON");
        att.setTotal(qs.size());
        att.setCorrect(correct);
        att.setScorePercent(pct);
        att.setPassed(passed);
        att.setDetails(details);
        att.setLocalDate(time.today(userId));
        attempts.save(att);

        LessonProgress p = progressFor(userId, l);
        if (pct.compareTo(p.getBestQuizPercent()) > 0) p.setBestQuizPercent(pct);
        boolean completedNow = false;
        String message;
        if (proveIt) {
            if (passed) {
                awards.add(xp.award(userId, XpAction.PROVE_IT_PASSED, "prove:" + l.getId(), l.getTitle()));
                if (p.getQuizPassedAt() == null) p.setQuizPassedAt(time.now());
                if (!p.isCompleted()) { completeLesson(userId, l, p, "PROVE_IT"); completedNow = true; }
                setTopicMastered(userId, l, pct);
                message = "Proven! This topic is now Mastered and moves to spaced review only.";
            } else {
                message = "Not yet 90%, and that's useful to know. The lesson will fill the gaps.";
            }
        } else if (passed) {
            if (p.getQuizPassedAt() == null) p.setQuizPassedAt(time.now());
            awards.add(xp.award(userId, XpAction.QUIZ_PASS, "quiz:" + l.getId(), l.getTitle()));
            if (!p.isCompleted() && p.getStepsDone().containsAll(REQUIRED_STEPS)) {
                completeLesson(userId, l, p, "LESSON");
                completedNow = true;
            }
            message = completedNow ? "Quiz passed and lesson complete!"
                    : p.isCompleted() ? "Quiz passed again. Nice recall!"
                    : "Quiz passed! Finish 'Try it yourself' and 'Predict the output' to complete the lesson.";
        } else {
            message = "You need " + PASS_PERCENT + "% to pass. Read the explanations, then try again: no penalty.";
        }
        streaks.recordActivity(userId);
        String unlocked = completedNow ? nextSlug(l) : null;
        return new QuizResult(proveIt ? "PROVE_IT" : "LESSON", correct, qs.size(), pct, passed, need, results,
                awards.stream().filter(XpAward::awarded).toList(), completedNow, unlocked, message);
    }

    /**
     * Explain it back. Without AI: the learner ticks rubric items honestly, score = ticked / total.
     * (Phase 3 adds optional AI scoring through MentorService, which calls saveExplainBack too.)
     */
    @Transactional
    public ExplainBackResult saveExplainBack(Long userId, String slug, String text, List<Integer> checked,
                                             BigDecimal aiScore, String aiFeedback) {
        Lesson l = load(slug);
        assertUnlocked(userId, l);
        LessonProgress p = progressFor(userId, l);
        int rubricSize = Math.max(1, l.getContent().path("explainBack").path("rubric").size());
        List<Integer> valid = checked.stream().filter(i -> i >= 0 && i < rubricSize).distinct().toList();
        BigDecimal selfScore = BigDecimal.valueOf(valid.size() * 100.0 / rubricSize).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal score = aiScore != null ? aiScore : selfScore;
        p.setExplainBack(text.trim());
        p.setExplainBackChecks(valid);
        p.setExplainBackScore(score);
        XpAward award = XpAward.none("Explain-backs score 60%+ to earn XP. Try covering more rubric points.");
        String topicStatus = null;
        if (score.compareTo(BigDecimal.valueOf(60)) >= 0 && text.trim().length() >= 60) {
            award = xp.award(userId, XpAction.EXPLAIN_IT_BACK, "explain:" + l.getId(), l.getTitle());
            if (p.isCompleted()) {
                moveTopicAtLeast(userId, l, TopicStatus.CAN_EXPLAIN);
                topicStatus = TopicStatus.CAN_EXPLAIN.name();
            }
        }
        List<String> steps = new ArrayList<>(p.getStepsDone());
        if (!steps.contains("explainBack")) { steps.add("explainBack"); p.setStepsDone(steps); }
        String feedback = aiFeedback != null ? aiFeedback
                : "Self-check: you covered " + valid.size() + " of " + rubricSize + " points. Compare with the model answer below and notice what you missed.";
        return new ExplainBackResult(score, feedback, l.getContent().path("explainBack").path("modelAnswer").asText(),
                aiScore != null, award, topicStatus);
    }

    // ------------------------------------------------------------------ helpers

    private void completeLesson(Long userId, Lesson l, LessonProgress p, String via) {
        p.setStatus(LessonProgress.Status.COMPLETED);
        p.setCompletedAt(time.now());
        p.setCompletedVia(via);
        xp.award(userId, XpAction.LESSON_COMPLETE, "lesson:" + l.getId(), l.getTitle());
        flashcards.createFromLesson(userId, l);
        moveTopicAtLeast(userId, l, TopicStatus.UNDERSTOOD);
        quests.advance(userId, QuestTrigger.LESSON_DONE);
    }

    private void setTopicMastered(Long userId, Lesson l, BigDecimal pct) {
        if (l.getTopicId() == null) return;
        JavaTopicProgress tp = topicProgressFor(userId, l.getTopicId());
        tp.setStatus(TopicStatus.MASTERED);
        tp.setMasteryScore(pct);
        tp.setLastReviewed(time.now());
        topicProgress.save(tp);
    }

    private void moveTopicAtLeast(Long userId, Lesson l, TopicStatus s) {
        if (l.getTopicId() == null) return;
        JavaTopicProgress tp = topicProgressFor(userId, l.getTopicId());
        if (tp.getStatus().ordinal() < s.ordinal()) {
            tp.setStatus(s);
            tp.setLastReviewed(time.now());
            topicProgress.save(tp);
        }
    }

    private JavaTopicProgress topicProgressFor(Long userId, Long topicId) {
        return topicProgress.findByUserIdAndTopicId(userId, topicId).orElseGet(() -> {
            JavaTopicProgress n = new JavaTopicProgress();
            n.setUserId(userId);
            n.setTopic(topics.getReferenceById(topicId));
            return n;
        });
    }

    private LessonProgress progressFor(Long userId, Lesson l) {
        return progress.findByUserIdAndLessonId(userId, l.getId()).orElseGet(() -> {
            LessonProgress n = new LessonProgress();
            n.setUserId(userId);
            n.setLessonId(l.getId());
            return progress.save(n);
        });
    }

    private void assertUnlocked(Long userId, Lesson l) {
        List<Lesson> siblings = lessons.findByWorldCodeOrderBySectionAscSortOrderAsc(l.getWorldCode()).stream()
                .filter(x -> x.getSection().equals(l.getSection())).toList();
        if (isLocked(l, siblings, myProgress(userId))) {
            throw ApiException.forbidden("This lesson is locked. Pass the previous lesson's quiz first, or take the Prove-it test.");
        }
    }

    /** Locked = the previous lesson in the same section isn't completed yet. The first lesson is always open. */
    static boolean isLocked(Lesson l, List<Lesson> all, Map<Long, LessonProgress> mine) {
        LessonProgress own = mine.get(l.getId());
        if (own != null && own.isCompleted()) return false;
        Lesson prev = null;
        for (Lesson x : all) {
            if (!x.getSection().equals(l.getSection())) continue;
            if (x.getSortOrder() < l.getSortOrder() && (prev == null || x.getSortOrder() > prev.getSortOrder())) prev = x;
        }
        return prev != null && !isDone(mine.get(prev.getId()));
    }

    private static boolean isDone(LessonProgress p) { return p != null && p.isCompleted(); }

    private String nextSlug(Lesson l) {
        return lessons.findByWorldCodeOrderBySectionAscSortOrderAsc(l.getWorldCode()).stream()
                .filter(x -> x.getSection().equals(l.getSection()) && x.getSortOrder() > l.getSortOrder())
                .min(Comparator.comparingInt(Lesson::getSortOrder)).map(Lesson::getSlug).orElse(null);
    }

    private static int indexOf(List<Lesson> list, Lesson l) {
        for (int i = 0; i < list.size(); i++) if (list.get(i).getId().equals(l.getId())) return i;
        return -1;
    }

    List<QuestionView> publicQuestions(Long lessonId) {
        return questions.findByLessonIdOrderBySortOrder(lessonId).stream().map(LessonService::toPublic).toList();
    }

    static QuestionView toPublic(QuizQuestion q) {
        return new QuestionView(q.getExtId(), q.getType(), q.getPrompt(), q.getCode(), q.getOptions());
    }

    /** Human-readable correct answer, shown only AFTER submitting. */
    static String displayAnswer(QuizQuestion q) {
        JsonNode a = q.getAnswer();
        return switch (q.getType()) {
            case "FILL_THE_CODE" -> a.path("accepted").path(0).asText();
            case "MATCH" -> {
                List<String> parts = new ArrayList<>();
                a.path("pairs").forEach(p -> parts.add(p.path("left").asText() + " → " + p.path("right").asText()));
                yield String.join("\n", parts);
            }
            default -> q.getOptions().path(a.path("index").asInt()).asText();
        };
    }

    private Lesson load(String slug) {
        return lessons.findBySlug(slug).orElseThrow(() -> ApiException.notFound("Lesson"));
    }

    private Map<Long, LessonProgress> myProgress(Long userId) {
        return progress.findByUserId(userId).stream().collect(Collectors.toMap(LessonProgress::getLessonId, Function.identity()));
    }

    private Map<Long, TopicStatus> topicStatuses(Long userId) {
        Map<Long, TopicStatus> m = new HashMap<>();
        topicProgress.findByUserId(userId).forEach(tp -> m.put(tp.getTopic().getId(), tp.getStatus()));
        return m;
    }

    private Map<String, String> sectionNames() {
        Map<String, String> m = new HashMap<>();
        topics.findAllByOrderBySectionAscSortOrderAsc().forEach(t -> m.putIfAbsent(t.getSection(), t.getSectionName()));
        m.put("DSA", "Patterns");
        return m;
    }
}
