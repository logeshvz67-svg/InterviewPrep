package com.codequest.service;

import com.codequest.domain.QuizGrader;
import com.codequest.domain.XpAction;
import com.codequest.dto.InterviewDtos.*;
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
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Exam pattern editor + timed mock tests.
 * The app NEVER hardcodes a company's test pattern. You enter it (sections, question counts, minutes,
 * cutoffs) after checking the official careers page, and mock tests are generated from what you entered.
 *
 * Sections: APTITUDE (aptitude bank), TECH_MCQ (lesson quiz questions), CODING (problem bank; you solve on
 * the platform and self-report: we don't run code, so coding scores are honest self-reports).
 */
@Service
public class ExamService {
    static final Duration GRACE = Duration.ofMinutes(2);

    private final ExamPatternRepository patterns;
    private final MockTestRepository tests;
    private final MockTestAttemptRepository attempts;
    private final CompanyTagRepository companies;
    private final AptitudeQuestionRepository aptitude;
    private final QuizQuestionRepository quizQuestions;
    private final LessonRepository lessons;
    private final ProblemBankRepository problemBank;
    private final PracticeService practice;
    private final XpService xp;
    private final TimeService time;
    private final ObjectMapper json;

    public ExamService(ExamPatternRepository patterns, MockTestRepository tests, MockTestAttemptRepository attempts, CompanyTagRepository companies,
                       AptitudeQuestionRepository aptitude, QuizQuestionRepository quizQuestions, LessonRepository lessons,
                       ProblemBankRepository problemBank, PracticeService practice, XpService xp, TimeService time, ObjectMapper json) {
        this.patterns = patterns;
        this.tests = tests;
        this.attempts = attempts;
        this.companies = companies;
        this.aptitude = aptitude;
        this.quizQuestions = quizQuestions;
        this.lessons = lessons;
        this.problemBank = problemBank;
        this.practice = practice;
        this.xp = xp;
        this.time = time;
        this.json = json;
    }

    // ------------------------------------------------------------------ patterns

    @Transactional(readOnly = true)
    public List<ExamPatternView> listPatterns(Long userId) {
        return patterns.findByUserIdOrderByCompanyCodeAscNameAsc(userId).stream().map(ExamService::view).toList();
    }

    @Transactional
    public ExamPatternView savePattern(Long userId, Long id, ExamPatternRequest req) {
        if (!companies.existsByCode(req.companyCode())) throw ApiException.badRequest("Unknown company");
        ExamPatternConfig p = id == null ? new ExamPatternConfig() : patterns.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Exam pattern"));
        p.setUserId(userId);
        p.setCompanyCode(req.companyCode());
        p.setName(req.name().trim());
        p.setSections(json.valueToTree(req.sections()));
        p.setSourceNote(req.sourceNote());
        p.setVerifiedOn(req.verifiedOn());
        return view(patterns.save(p));
    }

    @Transactional
    public void deletePattern(Long userId, Long id) {
        patterns.delete(patterns.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Exam pattern")));
    }

    // ------------------------------------------------------------------ mock tests

    @Transactional
    public MockTestView start(Long userId, Long patternId) {
        ExamPatternConfig p = patterns.findByIdAndUserId(patternId, userId).orElseThrow(() -> ApiException.notFound("Exam pattern"));
        String company = p.getCompanyCode();
        Random rnd = new Random();
        ArrayNode paper = json.createArrayNode();
        int total = 0;
        for (JsonNode s : p.getSections()) {
            String kind = s.path("kind").asText();
            int n = s.path("questions").asInt();
            total += s.path("minutes").asInt();
            List<Long> ids = switch (kind) {
                case "APTITUDE" -> pick(aptitude.findAll().stream().filter(a -> a.relevantTo(company)).map(BaseEntity::getId).toList(),
                        aptitude.findAll().stream().map(BaseEntity::getId).toList(), n, rnd);
                case "TECH_MCQ" -> {
                    Set<Long> lessonIds = lessons.findAll().stream().filter(l -> l.relevantTo(company)).map(BaseEntity::getId).collect(Collectors.toSet());
                    List<Long> pool = quizQuestions.findByLessonIdIn(lessonIds).stream().filter(q -> QuizGrader.isOptionType(q.getType())).map(BaseEntity::getId).toList();
                    yield pick(pool, pool, n, rnd);
                }
                default -> {
                    boolean sp = "INFOSYS_SP".equals(company) || "PRODUCT".equals(company);
                    List<ProblemBankItem> all = problemBank.findAll();
                    List<Long> pool = all.stream().filter(b -> b.relevantTo(company) && (sp ? !b.getDifficulty().equals("EASY") : !b.getDifficulty().equals("HARD")))
                            .map(BaseEntity::getId).toList();
                    yield pick(pool, all.stream().map(BaseEntity::getId).toList(), n, rnd);
                }
            };
            ObjectNode sec = paper.addObject();
            sec.put("name", s.path("name").asText());
            sec.put("kind", kind);
            sec.put("minutes", s.path("minutes").asInt());
            if (s.hasNonNull("cutoffPercent")) sec.put("cutoffPercent", s.path("cutoffPercent").asInt());
            ArrayNode arr = sec.putArray("itemIds");
            ids.forEach(arr::add);
        }
        MockTest t = new MockTest();
        t.setUserId(userId);
        t.setCompanyCode(company);
        t.setTitle(p.getName() + " mock");
        t.setPattern(p.getSections());
        t.setPaper(paper);
        t.setTotalMinutes(total);
        t.setStatus("RUNNING");
        t.setStartedAt(time.now());
        t.setEndsAt(time.now().plus(Duration.ofMinutes(total)));
        tests.save(t);
        return view(t, null);
    }

    private static List<Long> pick(List<Long> preferred, List<Long> fallback, int n, Random rnd) {
        List<Long> pool = new ArrayList<>(preferred.size() >= n ? preferred : fallback);
        Collections.shuffle(pool, rnd);
        return pool.subList(0, Math.min(n, pool.size()));
    }

    @Transactional
    public MockTestView get(Long userId, Long id) {
        MockTest t = load(userId, id);
        if ("RUNNING".equals(t.getStatus()) && time.now().isAfter(t.getEndsAt().plus(Duration.ofHours(6)))) t.setStatus("EXPIRED");
        return view(t, attempts.findByMockTestId(id).orElse(null));
    }

    @Transactional
    public MockTestView submit(Long userId, Long id, MockSubmit req) {
        MockTest t = load(userId, id);
        if (!"RUNNING".equals(t.getStatus())) throw ApiException.conflict("This mock test is already " + t.getStatus().toLowerCase());
        Instant now = time.now();
        boolean late = now.isAfter(t.getEndsAt().plus(GRACE));

        ArrayNode sectionScores = json.createArrayNode();
        double sumPct = 0;
        int sections = 0;
        List<AptitudeAnswer> aptAnswers = new ArrayList<>();
        for (JsonNode sec : t.getPaper()) {
            String kind = sec.path("kind").asText();
            List<Long> ids = new ArrayList<>();
            sec.path("itemIds").forEach(x -> ids.add(x.asLong()));
            double got = 0;
            switch (kind) {
                case "APTITUDE" -> {
                    Map<Long, AptitudeQuestion> qs = aptitude.findAllById(ids).stream().collect(Collectors.toMap(BaseEntity::getId, a -> a));
                    for (Long qid : ids) {
                        JsonNode a = req.answers().get("APTITUDE:" + qid);
                        Integer chosen = a == null || !a.canConvertToInt() ? null : a.asInt();
                        aptAnswers.add(new AptitudeAnswer(qid, chosen, null));
                        if (chosen != null && qs.containsKey(qid) && qs.get(qid).getAnswer() == chosen) got++;
                    }
                }
                case "TECH_MCQ" -> {
                    Map<Long, QuizQuestion> qs = quizQuestions.findByIdIn(ids).stream().collect(Collectors.toMap(BaseEntity::getId, q -> q));
                    for (Long qid : ids) {
                        QuizQuestion q = qs.get(qid);
                        if (q != null && QuizGrader.isCorrect(q.getType(), q.getAnswer(), req.answers().get("TECH:" + qid))) got++;
                    }
                }
                default -> {
                    for (Long pid : ids) {
                        String st = req.answers().getOrDefault("CODING:" + pid, json.createObjectNode()).path("status").asText("NOT_ATTEMPTED");
                        got += switch (st) { case "SOLVED" -> 1.0; case "PARTIAL" -> 0.5; default -> 0; };
                    }
                }
            }
            double pct = ids.isEmpty() ? 0 : got * 100 / ids.size();
            ObjectNode o = sectionScores.addObject();
            o.put("name", sec.path("name").asText());
            o.put("kind", kind);
            o.put("score", Math.round(got * 10) / 10.0);
            o.put("total", ids.size());
            o.put("percent", Math.round(pct * 10) / 10.0);
            if (sec.hasNonNull("cutoffPercent")) o.put("passedCutoff", pct >= sec.path("cutoffPercent").asInt());
            if ("CODING".equals(kind)) o.put("note", "Self-reported (code isn't executed here)");
            sumPct += pct;
            sections++;
        }
        if (!aptAnswers.isEmpty()) practice.gradeAptitude(userId, new AptitudeSubmit(aptAnswers), "MOCK");

        MockTestAttempt a = new MockTestAttempt();
        a.setMockTestId(t.getId());
        a.setUserId(userId);
        a.setAnswers(json.valueToTree(req.answers()));
        a.setSectionScores(sectionScores);
        a.setScorePercent(BigDecimal.valueOf(sections == 0 ? 0 : sumPct / sections).setScale(2, RoundingMode.HALF_UP));
        a.setSubmittedAt(now);
        a.setLate(late);
        attempts.save(a);
        t.setStatus("SUBMITTED");
        xp.award(userId, XpAction.MOCK_TEST_COMPLETED, "mocktest:" + t.getId(), t.getTitle());
        return view(t, a);
    }

    @Transactional(readOnly = true)
    public List<MockSummary> history(Long userId) {
        return tests.findTop30ByUserIdOrderByStartedAtDesc(userId).stream().map(t -> new MockSummary(t.getId(), t.getTitle(), t.getCompanyCode(),
                t.getStatus(), t.getStartedAt(), attempts.findByMockTestId(t.getId()).map(MockTestAttempt::getScorePercent).orElse(null))).toList();
    }

    /** 0..1: average of your last 3 mock tests (null if none). Used by readiness. */
    public BigDecimal recentMockScore(Long userId) {
        List<MockTestAttempt> last = attempts.findTop3ByUserIdOrderBySubmittedAtDesc(userId);
        if (last.isEmpty()) return null;
        double avg = last.stream().mapToDouble(x -> x.getScorePercent().doubleValue()).average().orElse(0);
        return BigDecimal.valueOf(avg / 100).setScale(4, RoundingMode.HALF_UP);
    }

    private MockTest load(Long userId, Long id) {
        return tests.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Mock test"));
    }

    /** The paper with question content but WITHOUT answers (until submitted). */
    private MockTestView view(MockTest t, MockTestAttempt attempt) {
        boolean done = attempt != null;
        List<MockSection> secs = new ArrayList<>();
        for (JsonNode sec : t.getPaper()) {
            String kind = sec.path("kind").asText();
            List<Long> ids = new ArrayList<>();
            sec.path("itemIds").forEach(x -> ids.add(x.asLong()));
            List<JsonNode> items = new ArrayList<>();
            switch (kind) {
                case "APTITUDE" -> {
                    Map<Long, AptitudeQuestion> m = aptitude.findAllById(ids).stream().collect(Collectors.toMap(BaseEntity::getId, a -> a));
                    for (Long qid : ids) {
                        AptitudeQuestion q = m.get(qid);
                        if (q == null) continue;
                        ObjectNode o = json.valueToTree(PracticeService.item(q));
                        o.put("key", "APTITUDE:" + qid);
                        if (done) { o.put("answer", q.getAnswer()); o.put("explanation", q.getExplanation()); }
                        items.add(o);
                    }
                }
                case "TECH_MCQ" -> {
                    Map<Long, QuizQuestion> m = quizQuestions.findByIdIn(ids).stream().collect(Collectors.toMap(BaseEntity::getId, q -> q));
                    for (Long qid : ids) {
                        QuizQuestion q = m.get(qid);
                        if (q == null) continue;
                        ObjectNode o = json.createObjectNode();
                        o.put("key", "TECH:" + qid);
                        o.put("type", q.getType());
                        o.put("prompt", q.getPrompt());
                        o.put("code", q.getCode());
                        o.set("options", q.getOptions());
                        if (done) { o.put("answer", q.getAnswer().path("index").asInt()); o.put("explanation", q.getExplanation()); }
                        items.add(o);
                    }
                }
                default -> {
                    Map<Long, ProblemBankItem> m = problemBank.findAllById(ids).stream().collect(Collectors.toMap(BaseEntity::getId, b -> b));
                    for (Long pid : ids) {
                        ProblemBankItem b = m.get(pid);
                        if (b == null) continue;
                        ObjectNode o = json.createObjectNode();
                        o.put("key", "CODING:" + pid);
                        o.put("title", b.getTitle());
                        o.put("url", b.getUrl());
                        o.put("difficulty", b.getDifficulty());
                        o.put("pattern", b.getPatternCode());
                        o.put("slug", b.getSlug());
                        items.add(o);
                    }
                }
            }
            secs.add(new MockSection(sec.path("name").asText(), kind, sec.path("minutes").asInt(),
                    sec.hasNonNull("cutoffPercent") ? sec.path("cutoffPercent").asInt() : null, items));
        }
        ObjectNode result = null;
        if (done) {
            result = json.createObjectNode();
            result.put("scorePercent", attempt.getScorePercent());
            result.put("late", attempt.isLate());
            result.set("sections", attempt.getSectionScores());
            result.set("answers", attempt.getAnswers());
        }
        return new MockTestView(t.getId(), t.getTitle(), t.getCompanyCode(), t.getStatus(), t.getStartedAt(), t.getEndsAt(), time.now(),
                t.getTotalMinutes(), secs, result);
    }

    static ExamPatternView view(ExamPatternConfig p) {
        int total = 0;
        for (JsonNode s : p.getSections()) total += s.path("minutes").asInt();
        return new ExamPatternView(p.getId(), p.getCompanyCode(), p.getName(), p.getSections(), p.getSourceNote(), p.getVerifiedOn(), total);
    }
}
