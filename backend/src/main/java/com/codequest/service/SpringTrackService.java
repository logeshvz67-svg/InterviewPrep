package com.codequest.service;

import com.codequest.content.ContentCatalog;
import com.codequest.domain.XpAction;
import com.codequest.dto.AiDtos.ExplainScore;
import com.codequest.dto.GameDtos.XpAward;
import com.codequest.entity.ProjectMilestone;
import com.codequest.entity.SpringProject;
import com.codequest.entity.SpringTopic;
import com.codequest.entity.SpringTopicProgress;
import com.codequest.exception.ApiException;
import com.codequest.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Spring Boot Track: concept map with status, checklist, quiz and explain-back per topic;
 * the 4 projects with milestones and a build log; annotation cards and debug challenges.
 */
@Service
public class SpringTrackService {
    public static final List<String> STATUSES = List.of("NOT_STARTED", "LEARNING", "UNDERSTOOD", "CAN_EXPLAIN");

    private final SpringTopicRepository topics;
    private final SpringTopicProgressRepository progress;
    private final SpringProjectRepository projects;
    private final ProjectMilestoneRepository milestones;
    private final ContentCatalog catalog;
    private final XpService xp;
    private final MentorService mentor;
    private final StreakService streaks;
    private final TimeService time;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public SpringTrackService(SpringTopicRepository topics, SpringTopicProgressRepository progress, SpringProjectRepository projects,
                              ProjectMilestoneRepository milestones, ContentCatalog catalog, XpService xp, MentorService mentor,
                              StreakService streaks, TimeService time, JdbcTemplate jdbc, ObjectMapper json) {
        this.topics = topics;
        this.progress = progress;
        this.projects = projects;
        this.milestones = milestones;
        this.catalog = catalog;
        this.xp = xp;
        this.mentor = mentor;
        this.streaks = streaks;
        this.time = time;
        this.jdbc = jdbc;
        this.json = json;
    }

    public record TopicCard(Long id, String code, String title, String group, int order, String summary, String status,
                            int checklistDone, int checklistTotal, BigDecimal bestQuizPercent) {}

    public record TopicDetail(TopicCard card, List<String> checklist, List<Integer> checklistDone, List<JsonNode> quiz,
                              String explainPrompt, List<String> rubric, String explainBack) {}

    public record QuizAnswers(@NotNull @Size(min = 1, max = 10) List<Integer> answers) {}

    public record QuizOutcome(int correct, int total, BigDecimal percent, boolean passed, List<JsonNode> results, XpAward xp, String status) {}

    public record ExplainRequest(@NotBlank @Size(min = 20, max = 4000) String text, @NotNull List<Integer> checkedRubric, boolean useAi) {}

    public record ExplainOutcome(BigDecimal score, String feedback, boolean aiUsed, XpAward xp, String status) {}

    public record ProjectView(Long id, String code, String title, String description, String githubUrl, String status,
                              List<MilestoneView> milestones, List<Map<String, Object>> logs) {}

    public record MilestoneView(Long id, String title, int order, boolean done) {}

    public record ProjectUpdate(@Size(max = 300) @org.hibernate.validator.constraints.URL String githubUrl,
                                @NotBlank @Pattern(regexp = "NOT_STARTED|IN_PROGRESS|DONE") String status) {}

    public record LogRequest(@NotBlank @Pattern(regexp = "LOG|BLOCKER|LEARNED") String entryType, @NotBlank @Size(max = 4000) String text) {}

    // ------------------------------------------------------------------ concept map

    @Transactional(readOnly = true)
    public List<TopicCard> map(Long userId) {
        Map<Long, SpringTopicProgress> mine = progress.findByUserId(userId).stream().collect(Collectors.toMap(SpringTopicProgress::getTopicId, Function.identity()));
        return topics.findAllByOrderBySortOrder().stream().map(t -> card(t, mine.get(t.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public TopicDetail topic(Long userId, String code) {
        SpringTopic t = load(code);
        SpringTopicProgress p = progress.findByUserIdAndTopicId(userId, t.getId()).orElse(null);
        List<JsonNode> quiz = new ArrayList<>();
        for (JsonNode q : t.getContent().path("quiz")) {
            ObjectNode o = json.createObjectNode();
            o.put("prompt", q.path("prompt").asText());
            o.set("options", q.path("options"));   // answers are NOT sent
            quiz.add(o);
        }
        return new TopicDetail(card(t, p), strings(t.getContent().path("checklist")), p == null ? List.of() : p.getChecklistDone(), quiz,
                t.getContent().path("explainBack").path("prompt").asText(), strings(t.getContent().path("explainBack").path("rubric")),
                p == null ? null : p.getExplainBack());
    }

    @Transactional
    public TopicCard setStatus(Long userId, String code, String status) {
        if (!List.of("NOT_STARTED", "LEARNING").contains(status)) throw ApiException.badRequest("UNDERSTOOD is earned by the quiz, CAN_EXPLAIN by the explain-back.");
        SpringTopic t = load(code);
        SpringTopicProgress p = progressFor(userId, t);
        if (STATUSES.indexOf(p.getStatus()) < STATUSES.indexOf(status)) p.setStatus(status);
        streaks.recordActivity(userId);
        return card(t, p);
    }

    @Transactional
    public TopicCard toggleChecklist(Long userId, String code, int index) {
        SpringTopic t = load(code);
        if (index < 0 || index >= t.getContent().path("checklist").size()) throw ApiException.badRequest("No checklist item " + index);
        SpringTopicProgress p = progressFor(userId, t);
        List<Integer> done = new ArrayList<>(p.getChecklistDone());
        if (done.contains(index)) done.remove(Integer.valueOf(index)); else done.add(index);
        p.setChecklistDone(done);
        if ("NOT_STARTED".equals(p.getStatus())) p.setStatus("LEARNING");
        return card(t, p);
    }

    /** 2 of 3 correct (67%+) marks the topic UNDERSTOOD. */
    @Transactional
    public QuizOutcome submitQuiz(Long userId, String code, QuizAnswers req) {
        SpringTopic t = load(code);
        JsonNode quiz = t.getContent().path("quiz");
        int correct = 0;
        List<JsonNode> results = new ArrayList<>();
        for (int i = 0; i < quiz.size(); i++) {
            Integer given = i < req.answers().size() ? req.answers().get(i) : null;
            boolean ok = given != null && given == quiz.get(i).path("answer").asInt();
            if (ok) correct++;
            ObjectNode r = json.createObjectNode();
            r.put("correct", ok);
            r.put("answer", quiz.get(i).path("answer").asInt());
            r.put("explanation", quiz.get(i).path("explanation").asText());
            results.add(r);
        }
        BigDecimal pct = BigDecimal.valueOf(quiz.isEmpty() ? 0 : correct * 100.0 / quiz.size()).setScale(2, RoundingMode.HALF_UP);
        boolean passed = pct.compareTo(BigDecimal.valueOf(66)) >= 0;
        SpringTopicProgress p = progressFor(userId, t);
        if (pct.compareTo(p.getBestQuizPercent()) > 0) p.setBestQuizPercent(pct);
        XpAward award = XpAward.none("Get 2 of 3 right to mark this topic Understood.");
        if (passed) {
            if (STATUSES.indexOf(p.getStatus()) < STATUSES.indexOf("UNDERSTOOD")) p.setStatus("UNDERSTOOD");
            award = xp.award(userId, XpAction.SPRING_TOPIC_UNDERSTOOD, "spring-topic:" + t.getId(), t.getTitle());
        }
        streaks.recordActivity(userId);
        return new QuizOutcome(correct, quiz.size(), pct, passed, results, award, p.getStatus());
    }

    @Transactional
    public ExplainOutcome explain(Long userId, String code, ExplainRequest req) {
        SpringTopic t = load(code);
        List<String> rubric = strings(t.getContent().path("explainBack").path("rubric"));
        ExplainScore s = req.useAi() ? mentor.scoreExplanation(userId, t.getTitle(), t.getContent().path("summary").asText(), rubric, req.text())
                : new ExplainScore(null, null, false);
        long ticked = req.checkedRubric().stream().filter(i -> i >= 0 && i < rubric.size()).distinct().count();
        BigDecimal score = s.score() != null ? s.score() : BigDecimal.valueOf(rubric.isEmpty() ? 0 : ticked * 100.0 / rubric.size()).setScale(2, RoundingMode.HALF_UP);
        SpringTopicProgress p = progressFor(userId, t);
        p.setExplainBack(req.text().trim());
        XpAward award = XpAward.none("Score 60%+ to earn XP and reach 'Can explain'.");
        if (score.compareTo(BigDecimal.valueOf(60)) >= 0) {
            award = xp.award(userId, XpAction.EXPLAIN_IT_BACK, "spring-explain:" + t.getId(), t.getTitle());
            if ("UNDERSTOOD".equals(p.getStatus())) p.setStatus("CAN_EXPLAIN");
        }
        String feedback = s.feedback() != null ? s.feedback()
                : "Self-check: " + ticked + " of " + rubric.size() + " points covered." + (req.useAi() ? " (AI unavailable, so your rubric ticks were used.)" : "");
        return new ExplainOutcome(score, feedback, s.aiUsed(), award, p.getStatus());
    }

    // ------------------------------------------------------------------ reference content

    public JsonNode annotations() { return catalog.springAnnotations(); }
    public JsonNode debugChallenges() { return catalog.debugChallenges(); }

    // ------------------------------------------------------------------ projects

    @Transactional
    public List<ProjectView> projects(Long userId) {
        List<SpringProject> mine = projects.findByUserIdOrderByIdAsc(userId);
        if (mine.isEmpty()) {
            // First visit: create the 4 projects from the templates.
            for (JsonNode tpl : catalog.projectTemplates()) {
                SpringProject p = new SpringProject();
                p.setUserId(userId);
                p.setCode(tpl.path("code").asText());
                p.setTitle(tpl.path("title").asText());
                p.setDescription(tpl.path("description").asText());
                projects.save(p);
                int i = 0;
                for (JsonNode m : tpl.path("milestones")) {
                    ProjectMilestone ms = new ProjectMilestone();
                    ms.setUserId(userId);
                    ms.setProjectId(p.getId());
                    ms.setTitle(m.asText());
                    ms.setSortOrder(i++);
                    milestones.save(ms);
                }
            }
            mine = projects.findByUserIdOrderByIdAsc(userId);
        }
        return mine.stream().map(p -> projectView(userId, p)).toList();
    }

    @Transactional
    public ProjectView updateProject(Long userId, Long id, ProjectUpdate req) {
        SpringProject p = projects.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Project"));
        p.setGithubUrl(req.githubUrl() == null || req.githubUrl().isBlank() ? null : req.githubUrl().trim());
        p.setStatus(req.status());
        return projectView(userId, p);
    }

    /** Ticking a milestone pays +40 XP once (unticking keeps it honest but doesn't take XP back). */
    @Transactional
    public ProjectView toggleMilestone(Long userId, Long milestoneId) {
        ProjectMilestone m = milestones.findByIdAndUserId(milestoneId, userId).orElseThrow(() -> ApiException.notFound("Milestone"));
        if (m.getDoneAt() == null) {
            m.setDoneAt(time.now());
            xp.award(userId, XpAction.SPRING_MILESTONE, "milestone:" + m.getId(), m.getTitle());
        } else {
            m.setDoneAt(null);
        }
        SpringProject p = projects.findByIdAndUserId(m.getProjectId(), userId).orElseThrow();
        if ("NOT_STARTED".equals(p.getStatus())) p.setStatus("IN_PROGRESS");
        return projectView(userId, p);
    }

    @Transactional
    public ProjectView addLog(Long userId, Long projectId, LogRequest req) {
        SpringProject p = projects.findByIdAndUserId(projectId, userId).orElseThrow(() -> ApiException.notFound("Project"));
        jdbc.update("insert into build_logs (user_id, project_id, entry_type, text, local_date) values (?,?,?,?,?)",
                userId, p.getId(), req.entryType(), req.text().trim(), Date.valueOf(time.today(userId)));
        streaks.recordActivity(userId);
        return projectView(userId, p);
    }

    @Transactional
    public void resolveLog(Long userId, Long logId, boolean resolved) {
        int n = jdbc.update("update build_logs set resolved=?, updated_at=now() where id=? and user_id=?", resolved, logId, userId);
        if (n == 0) throw ApiException.notFound("Log entry");
    }

    private ProjectView projectView(Long userId, SpringProject p) {
        List<MilestoneView> ms = milestones.findByProjectIdOrderBySortOrder(p.getId()).stream()
                .map(m -> new MilestoneView(m.getId(), m.getTitle(), m.getSortOrder(), m.getDoneAt() != null)).toList();
        List<Map<String, Object>> logs = jdbc.queryForList(
                "select id, entry_type, text, resolved, local_date, created_at from build_logs where user_id=? and project_id=? order by id desc limit 50", userId, p.getId());
        return new ProjectView(p.getId(), p.getCode(), p.getTitle(), p.getDescription(), p.getGithubUrl(), p.getStatus(), ms, logs);
    }

    // ------------------------------------------------------------------ helpers

    private SpringTopic load(String code) {
        return topics.findByCode(code).orElseThrow(() -> ApiException.notFound("Spring topic"));
    }

    private SpringTopicProgress progressFor(Long userId, SpringTopic t) {
        return progress.findByUserIdAndTopicId(userId, t.getId()).orElseGet(() -> {
            SpringTopicProgress n = new SpringTopicProgress();
            n.setUserId(userId);
            n.setTopicId(t.getId());
            return progress.save(n);
        });
    }

    private static TopicCard card(SpringTopic t, SpringTopicProgress p) {
        return new TopicCard(t.getId(), t.getCode(), t.getTitle(), t.getTopicGroup(), t.getSortOrder(), t.getContent().path("summary").asText(),
                p == null ? "NOT_STARTED" : p.getStatus(), p == null ? 0 : p.getChecklistDone().size(), t.getContent().path("checklist").size(),
                p == null ? BigDecimal.ZERO : p.getBestQuizPercent());
    }

    private static List<String> strings(JsonNode arr) {
        List<String> out = new ArrayList<>();
        arr.forEach(x -> out.add(x.asText()));
        return out;
    }
}
