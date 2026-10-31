package com.codequest.service;

import com.codequest.domain.XpAction;
import com.codequest.dto.GameDtos.XpAward;
import com.codequest.dto.InterviewDtos.*;
import com.codequest.dto.PageResponse;
import com.codequest.entity.AptitudeQuestion;
import com.codequest.entity.QuestionBankItem;
import com.codequest.exception.ApiException;
import com.codequest.repository.AptitudeQuestionRepository;
import com.codequest.repository.QuestionBankRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Interview Colosseum question bank + Aptitude Arena.
 * All items are "practice questions in the style of" a company. Practice attempts are simple
 * append-only logs, so they're written with JdbcTemplate.
 */
@Service
public class PracticeService {
    static final int SET_SIZE = 10;

    private final QuestionBankRepository bank;
    private final AptitudeQuestionRepository aptitude;
    private final JdbcTemplate jdbc;
    private final XpService xp;
    private final StreakService streaks;
    private final TimeService time;

    public PracticeService(QuestionBankRepository bank, AptitudeQuestionRepository aptitude, JdbcTemplate jdbc, XpService xp,
                           StreakService streaks, TimeService time) {
        this.bank = bank;
        this.aptitude = aptitude;
        this.jdbc = jdbc;
        this.xp = xp;
        this.streaks = streaks;
        this.time = time;
    }

    // ------------------------------------------------------------------ interview bank

    @Transactional(readOnly = true)
    public PageResponse<BankQuestion> browse(Long userId, String category, String difficulty, String company, String q, int page, int size) {
        Map<Long, BigDecimal> best = new HashMap<>();
        jdbc.query("select question_id, max(score) s from interview_practice where user_id=? group by question_id",
                rs -> { best.put(rs.getLong(1), rs.getBigDecimal(2)); }, userId);
        String query = q == null ? "" : q.trim().toLowerCase();
        List<BankQuestion> all = bank.findAllByOrderByIdAsc().stream()
                .filter(b -> category == null || category.isBlank() || b.getCategory().equals(category))
                .filter(b -> difficulty == null || difficulty.isBlank() || b.getDifficulty().equals(difficulty))
                .filter(b -> b.relevantTo(company))
                .filter(b -> query.isEmpty() || (b.getQuestion() + " " + b.getTopic()).toLowerCase().contains(query))
                .map(b -> view(b, best.get(b.getId()))).toList();
        int from = Math.min(page * size, all.size());
        return new PageResponse<>(all.subList(from, Math.min(from + size, all.size())), page, size, all.size(), (all.size() + size - 1) / size);
    }

    /** Self-scored practice: tick the key points your answer covered. 10 a day earns a practice-set reward. */
    @Transactional
    public PracticeResult practise(Long userId, Long questionId, PracticeRequest req) {
        QuestionBankItem q = bank.findById(questionId).orElseThrow(() -> ApiException.notFound("Question"));
        int n = Math.max(1, q.getKeyPoints().size());
        long covered = req.keyPointsCovered().stream().filter(i -> i < n).distinct().count();
        BigDecimal score = BigDecimal.valueOf(covered * 100.0 / n).setScale(2, RoundingMode.HALF_UP);
        jdbc.update("insert into interview_practice (user_id, question_id, my_answer, score, local_date) values (?,?,?,?,?)",
                userId, questionId, req.myAnswer(), score, Date.valueOf(time.today(userId)));
        int today = countToday("interview_practice", userId);
        XpAward award = today >= SET_SIZE && today % SET_SIZE == 0
                ? xp.award(userId, XpAction.PRACTICE_SET, "interview-set:" + time.today(userId) + ":" + today / SET_SIZE, "Interview practice")
                : XpAward.none((SET_SIZE - today % SET_SIZE) + " more practice answers today for a +15 XP practice set.");
        streaks.recordActivity(userId);
        return new PracticeResult(score, today, award);
    }

    /** 0..1 average self-score of recent SQL/DBMS practice (null = not practised yet). */
    public BigDecimal sqlScore(Long userId) {
        BigDecimal avg = jdbc.queryForObject("""
                select avg(score) from (select p.score from interview_practice p join question_bank q on q.id=p.question_id
                where p.user_id=? and q.category in ('SQL','DBMS') order by p.id desc limit 20) t""", BigDecimal.class, userId);
        return avg == null ? null : avg.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------------ aptitude

    @Transactional(readOnly = true)
    public List<AptitudeItem> aptitudeSet(Long userId, String section, String topic, int count) {
        List<AptitudeQuestion> pool = new ArrayList<>(aptitude.findAllByOrderByIdAsc().stream()
                .filter(a -> section == null || section.isBlank() || a.getSection().equals(section))
                .filter(a -> topic == null || topic.isBlank() || a.getTopic().equalsIgnoreCase(topic)).toList());
        // Prefer questions you haven't answered correctly yet.
        Set<Long> mastered = new HashSet<>(jdbc.queryForList("select distinct question_id from aptitude_attempts where user_id=? and correct", Long.class, userId));
        Collections.shuffle(pool);
        pool.sort(Comparator.comparing(a -> mastered.contains(a.getId())));
        return pool.stream().limit(Math.max(1, Math.min(30, count))).map(PracticeService::item).toList();
    }

    @Transactional
    public AptitudeGraded gradeAptitude(Long userId, AptitudeSubmit req, String source) {
        Map<Long, AptitudeQuestion> qs = aptitude.findAllById(req.answers().stream().map(AptitudeAnswer::questionId).toList())
                .stream().collect(Collectors.toMap(AptitudeQuestion::getId, a -> a));
        List<AptitudeResult> results = new ArrayList<>();
        int correct = 0;
        for (AptitudeAnswer a : req.answers()) {
            AptitudeQuestion q = qs.get(a.questionId());
            if (q == null) continue;
            boolean ok = a.chosen() != null && a.chosen() == q.getAnswer();
            if (ok) correct++;
            jdbc.update("insert into aptitude_attempts (user_id, question_id, chosen, correct, seconds, source, local_date) values (?,?,?,?,?,?,?)",
                    userId, q.getId(), a.chosen(), ok, a.seconds(), source, Date.valueOf(time.today(userId)));
            results.add(new AptitudeResult(q.getId(), ok, a.chosen(), q.getAnswer(), q.getExplanation()));
        }
        int total = results.size();
        XpAward award = XpAward.none("Answer 10+ questions in one set for +15 XP.");
        if ("PRACTICE".equals(source) && total >= SET_SIZE) {
            award = xp.award(userId, XpAction.PRACTICE_SET, "apt-set:" + time.now().toEpochMilli(), "Aptitude set");
        }
        streaks.recordActivity(userId);
        BigDecimal pct = total == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(correct * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
        return new AptitudeGraded(correct, total, pct, results, award);
    }

    public List<TopicAccuracy> aptitudeStats(Long userId) {
        return jdbc.query("""
                select q.section, q.topic, count(*) attempts, count(*) filter (where a.correct) correct
                from aptitude_attempts a join aptitude_questions q on q.id=a.question_id where a.user_id=?
                group by q.section, q.topic order by q.section, q.topic""",
                (rs, i) -> new TopicAccuracy(rs.getString(1), rs.getString(2), rs.getLong(3), rs.getLong(4)), userId);
    }

    /** 0..1 accuracy over your last 50 aptitude answers (null if fewer than 5). */
    public BigDecimal aptitudeScore(Long userId) {
        Map<String, Object> r = jdbc.queryForMap("""
                select count(*) n, count(*) filter (where correct) c from (
                  select correct from aptitude_attempts where user_id=? order by id desc limit 50) t""", userId);
        long n = ((Number) r.get("n")).longValue();
        if (n < 5) return null;
        return BigDecimal.valueOf(((Number) r.get("c")).doubleValue() / n).setScale(4, RoundingMode.HALF_UP);
    }

    private int countToday(String table, Long userId) {
        Integer n = jdbc.queryForObject("select count(*) from " + table + " where user_id=? and local_date=?", Integer.class,
                userId, Date.valueOf(time.today(userId)));
        return n == null ? 0 : n;
    }

    static BankQuestion view(QuestionBankItem b, BigDecimal best) {
        return new BankQuestion(b.getId(), b.getExtId(), b.getCategory(), b.getTopic(), b.getDifficulty(), b.getCompanies(), b.getQuestion(),
                b.getAnswer(), b.getKeyPoints(), best);
    }

    static AptitudeItem item(AptitudeQuestion a) {
        return new AptitudeItem(a.getId(), a.getSection(), a.getTopic(), a.getDifficulty(), a.getQuestion(), a.getOptions());
    }
}
