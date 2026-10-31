package com.codequest.service;

import com.codequest.domain.QuestTrigger;
import com.codequest.domain.RevisitSchedule;
import com.codequest.domain.XpAction;
import com.codequest.dto.GameDtos.XpAward;
import com.codequest.dto.GymDtos.*;
import com.codequest.dto.PageResponse;
import com.codequest.entity.GymEnums.HintLevel;
import com.codequest.entity.Pattern;
import com.codequest.entity.ProblemRevisit;
import com.codequest.entity.ProblemSolved;
import com.codequest.exception.ApiException;
import com.codequest.mapper.Mappers;
import com.codequest.repository.PatternRepository;
import com.codequest.repository.ProblemRevisitRepository;
import com.codequest.repository.ProblemSolvedRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/** Problem-Solving Gym: log problems solved elsewhere, pattern map, revisit queue. */
@Service
public class GymService {
    private final ProblemSolvedRepository problems;
    private final PatternRepository patterns;
    private final ProblemRevisitRepository revisits;
    private final com.codequest.repository.ProblemBankRepository bank;
    private final XpService xp;
    private final QuestService quests;
    private final StreakService streaks;
    private final TimeService time;

    public GymService(ProblemSolvedRepository problems, PatternRepository patterns, ProblemRevisitRepository revisits,
                      com.codequest.repository.ProblemBankRepository bank,
                      XpService xp, QuestService quests, StreakService streaks, TimeService time) {
        this.problems = problems;
        this.patterns = patterns;
        this.revisits = revisits;
        this.bank = bank;
        this.xp = xp;
        this.quests = quests;
        this.streaks = streaks;
        this.time = time;
    }

    @Transactional(readOnly = true)
    public List<PatternView> allPatterns() {
        return patterns.findAllByOrderBySortOrder().stream().map(Mappers::toView).toList();
    }

    @Transactional
    public ProblemSaved log(Long userId, ProblemRequest req) {
        LocalDate today = time.today(userId);
        LocalDate solvedOn = req.solvedOn() == null ? today : req.solvedOn();
        if (solvedOn.isAfter(today)) throw ApiException.badRequest("Solved date can't be in the future");

        ProblemSolved p = new ProblemSolved();
        p.setUserId(userId);
        apply(p, req, solvedOn);
        problems.save(p);

        // Revisit queue: re-solve from scratch after 3/7/14 days.
        List<LocalDate> dates = new ArrayList<>();
        for (int round = 1; round <= RevisitSchedule.OFFSETS_DAYS.size(); round++) {
            ProblemRevisit r = new ProblemRevisit();
            r.setUserId(userId);
            r.setProblem(p);
            r.setRound(round);
            r.setDueDate(RevisitSchedule.dueDate(solvedOn, round));
            revisits.save(r);
            dates.add(r.getDueDate());
        }

        // Reward the PROCESS: writing your own approach, and trying before peeking.
        XpAward approachXp = xp.award(userId, XpAction.PROBLEM_WITH_APPROACH, "problem:" + p.getId(), p.getTitle());
        XpAward attemptXp = req.attemptedBeforeHint()
                ? xp.award(userId, XpAction.ATTEMPT_BEFORE_HINT, "problem:" + p.getId(), p.getTitle())
                : XpAward.none("Try it yourself before opening hints next time (+10 XP)");
        quests.advance(userId, QuestTrigger.PROBLEM_LOGGED);
        return new ProblemSaved(Mappers.toView(p), approachXp, attemptXp, dates);
    }

    @Transactional
    public ProblemView update(Long userId, Long id, ProblemRequest req) {
        ProblemSolved p = problems.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Problem"));
        LocalDate solvedOn = req.solvedOn() == null ? p.getSolvedOn() : req.solvedOn();
        if (solvedOn.isAfter(time.today(userId))) throw ApiException.badRequest("Solved date can't be in the future");
        apply(p, req, solvedOn);
        return Mappers.toView(p); // dirty checking saves the changes at commit
    }

    @Transactional
    public void delete(Long userId, Long id) {
        ProblemSolved p = problems.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Problem"));
        problems.delete(p); // revisits are removed by ON DELETE CASCADE; XP history stays as an audit trail
    }

    @Transactional(readOnly = true)
    public ProblemView get(Long userId, Long id) {
        return problems.findByIdAndUserId(id, userId).map(Mappers::toView).orElseThrow(() -> ApiException.notFound("Problem"));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProblemView> list(Long userId, String patternCode, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "solvedOn", "id"));
        var result = (patternCode == null || patternCode.isBlank())
                ? problems.findByUserId(userId, pageable)
                : problems.findByUserIdAndPattern(userId, patternCode, pageable);
        return PageResponse.of(result, Mappers::toView);
    }

    // ---------------------------------------------------------------- pattern map

    @Transactional(readOnly = true)
    public PatternMap patternMap(Long userId) {
        List<PatternStat> stats = computeStats(userId);
        PatternStat weakest = stats.stream().filter(s -> "WEAK".equals(s.strength()))
                .min(Comparator.comparingDouble(GymService::score)).orElse(null);
        PatternStat next;
        String reason;
        if (weakest != null) {
            next = weakest;
            reason = "You've started this pattern but it's still shaky. 2-3 more easy problems will make it click.";
        } else {
            next = stats.stream().filter(s -> s.solved() == 0 && s.spCore()).findFirst()
                    .orElse(stats.stream().filter(s -> !"STRONG".equals(s.strength())).findFirst().orElse(null));
            reason = next == null ? "Every pattern is strong. Time for timed mixed practice!"
                    : "Next pattern on the path. Start with 2 easy problems.";
        }
        PatternView nextView = next == null ? null : patterns.findByCodeIn(List.of(next.code())).stream().findFirst().map(Mappers::toView).orElse(null);
        return new PatternMap(stats, nextView, reason);
    }

    /** 0..1 DSA score for readiness: average strength over the core patterns. */
    @Transactional(readOnly = true)
    public BigDecimal dsaScore(Long userId) {
        List<PatternStat> core = computeStats(userId).stream().filter(PatternStat::spCore).toList();
        if (core.isEmpty()) return BigDecimal.ZERO;
        return BigDecimal.valueOf(core.stream().mapToDouble(GymService::score).average().orElse(0)).setScale(4, RoundingMode.HALF_UP);
    }

    private List<PatternStat> computeStats(Long userId) {
        Map<String, List<ProblemSolved>> byPattern = new HashMap<>();
        for (ProblemSolved p : problems.findAllByUserId(userId)) {
            for (Pattern pat : p.getPatterns()) byPattern.computeIfAbsent(pat.getCode(), k -> new ArrayList<>()).add(p);
        }
        List<PatternStat> out = new ArrayList<>();
        for (Pattern pat : patterns.findAllByOrderBySortOrder()) {
            List<ProblemSolved> list = byPattern.getOrDefault(pat.getCode(), List.of());
            int n = list.size();
            double conf = list.stream().mapToInt(ProblemSolved::getConfidence).average().orElse(0);
            double unaided = n == 0 ? 0 : (double) list.stream().filter(p -> p.getHintLevel() == HintLevel.NONE).count() / n;
            double s = strengthScore(n, conf, unaided);
            String label = n == 0 ? "NOT_STARTED" : s >= 0.65 ? "STRONG" : s >= 0.4 ? "OK" : "WEAK";
            out.add(new PatternStat(pat.getCode(), pat.getName(), pat.isSpCore(), n, round1(conf), round1(unaided), label));
        }
        return out;
    }

    /** 40% volume (5 problems = full), 30% confidence, 30% solved without hints. */
    static double strengthScore(int solved, double avgConfidence, double unaidedRate) {
        if (solved == 0) return 0;
        return Math.min(1.0, solved / 5.0) * 0.4 + (avgConfidence / 5.0) * 0.3 + unaidedRate * 0.3;
    }

    private static double score(PatternStat s) {
        return strengthScore(s.solved(), s.avgConfidence(), s.unaidedRate());
    }

    private static double round1(double v) { return Math.round(v * 10) / 10.0; }

    // ---------------------------------------------------------------- revisits

    @Transactional(readOnly = true)
    public List<RevisitView> dueRevisits(Long userId) {
        LocalDate today = time.today(userId);
        return revisits.findByUserIdAndCompletedAtIsNullAndDueDateLessThanEqualOrderByDueDate(userId, today)
                .stream().map(r -> Mappers.toView(r, today)).toList();
    }

    @Transactional(readOnly = true)
    public List<RevisitView> upcomingRevisits(Long userId) {
        LocalDate today = time.today(userId);
        return revisits.findByUserIdAndCompletedAtIsNullAndDueDateGreaterThanOrderByDueDate(userId, today)
                .stream().limit(30).map(r -> Mappers.toView(r, today)).toList();
    }

    @Transactional(readOnly = true)
    public int dueRevisitCount(Long userId) {
        return (int) revisits.countByUserIdAndCompletedAtIsNullAndDueDateLessThanEqual(userId, time.today(userId));
    }

    /**
     * Re-solved from scratch? +25 XP. Needed help? No XP (we don't reward copying), but it's honest,
     * so we schedule one more round in 3 days instead.
     */
    @Transactional
    public XpAward completeRevisit(Long userId, Long revisitId, RevisitCompleteRequest req) {
        ProblemRevisit r = revisits.findByIdAndUserId(revisitId, userId).orElseThrow(() -> ApiException.notFound("Revisit"));
        LocalDate today = time.today(userId);
        if (r.getCompletedAt() != null) throw ApiException.conflict("This revisit is already done");
        if (r.getDueDate().isAfter(today)) throw ApiException.badRequest("Not due yet. Come back on " + r.getDueDate());
        r.setCompletedAt(time.now());
        r.setSolvedUnaided(req.solvedUnaided());
        r.setNotes(req.notes());
        quests.advance(userId, QuestTrigger.REVISIT_DONE);
        if (req.solvedUnaided()) {
            return xp.award(userId, XpAction.REVISIT_RESOLVED, "revisit:" + r.getId(), r.getProblem().getTitle());
        }
        int nextRound = revisits.findByProblemIdOrderByRound(r.getProblem().getId()).stream().mapToInt(ProblemRevisit::getRound).max().orElse(r.getRound()) + 1;
        ProblemRevisit again = new ProblemRevisit();
        again.setUserId(userId);
        again.setProblem(r.getProblem());
        again.setRound(nextRound);
        again.setDueDate(today.plusDays(3));
        revisits.save(again);
        streaks.recordActivity(userId);
        return XpAward.none("Honest answer! We'll bring this one back on " + again.getDueDate() + ". Solve it unaided then for +25 XP.");
    }

    private void apply(ProblemSolved p, ProblemRequest req, LocalDate solvedOn) {
        Set<String> codes = new HashSet<>(req.patternCodes());
        List<Pattern> found = patterns.findByCodeIn(codes);
        if (found.size() != codes.size()) throw ApiException.badRequest("Unknown pattern code in " + codes);
        p.setTitle(req.title().trim());
        p.setLink(req.link());
        p.setPlatform(req.platform());
        p.setDifficulty(req.difficulty());
        p.setTimeTakenMinutes(req.timeTakenMinutes());
        p.setAttempts(req.attempts());
        p.setHintLevel(req.hintLevel());
        p.setAttemptedBeforeHint(req.attemptedBeforeHint());
        p.setConfidence(req.confidence());
        p.setApproach(req.approach().trim());
        p.setCode(req.code());
        p.setSolvedOn(solvedOn);
        p.setPatterns(new HashSet<>(found));
        if (req.bankSlug() != null && !req.bankSlug().isBlank()) {
            p.setBankProblemId(bank.findBySlug(req.bankSlug()).map(b -> b.getId()).orElse(null));
        }
    }
}
