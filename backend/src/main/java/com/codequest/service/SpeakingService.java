package com.codequest.service;

import com.codequest.ai.AiService;
import com.codequest.ai.ChatClient.Message;
import com.codequest.content.ContentCatalog;
import com.codequest.domain.QuestTrigger;
import com.codequest.domain.SpeakingMetrics;
import com.codequest.domain.XpAction;
import com.codequest.dto.GameDtos.XpAward;
import com.codequest.dto.PageResponse;
import com.codequest.dto.SpeakDtos.*;
import com.codequest.entity.SpeakingPrompt;
import com.codequest.entity.SpeakingSession;
import com.codequest.exception.ApiException;
import com.codequest.repository.SpeakingPromptRepository;
import com.codequest.repository.SpeakingSessionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Communication Gym. The browser records speech with the Web Speech API (or the learner types),
 * then sends ONLY the transcript + duration. No audio is stored.
 * Local metrics (WPM, fillers, pauses, length, structure) always work; AI feedback is optional.
 */
@Service
public class SpeakingService {
    private final SpeakingPromptRepository prompts;
    private final SpeakingSessionRepository sessions;
    private final AiService ai;
    private final XpService xp;
    private final QuestService quests;
    private final TimeService time;
    private final ContentCatalog catalog;
    private final JdbcTemplate jdbc;

    public SpeakingService(SpeakingPromptRepository prompts, SpeakingSessionRepository sessions, AiService ai, XpService xp,
                           QuestService quests, TimeService time, ContentCatalog catalog, JdbcTemplate jdbc) {
        this.prompts = prompts;
        this.sessions = sessions;
        this.ai = ai;
        this.xp = xp;
        this.quests = quests;
        this.time = time;
        this.catalog = catalog;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<PromptView> prompts(String category) {
        return prompts.findAllByOrderByIdAsc().stream()
                .filter(p -> category == null || category.isBlank() || p.getCategory().equals(category))
                .map(SpeakingService::view).toList();
    }

    public Map<String, JsonNode> library() {
        return Map.of("phrases", catalog.phrases(), "vocabulary", catalog.vocabulary(), "pronunciation", catalog.pronunciation());
    }

    @Transactional
    public SessionSaved submit(Long userId, SessionRequest req) {
        SpeakingPrompt prompt = req.promptId() == null ? null : prompts.findById(req.promptId()).orElseThrow(() -> ApiException.notFound("Prompt"));
        String framework = prompt == null ? (req.mode().equals("EXPLAIN_ALOUD") ? "CEU" : "NONE") : prompt.getFramework();
        int limit = prompt == null ? 120 : prompt.getTimeLimitSeconds();
        SpeakingMetrics.Result m = SpeakingMetrics.compute(req.transcript(), req.durationSeconds(), req.longPauses(), limit, framework);

        SpeakingSession s = new SpeakingSession();
        s.setUserId(userId);
        s.setPromptId(prompt == null ? null : prompt.getId());
        s.setMode(req.mode());
        s.setPromptText(req.promptText().trim());
        s.setTranscript(req.transcript().trim());
        s.setInputMethod(req.inputMethod());
        s.setDurationSeconds(req.durationSeconds());
        s.setWordCount(m.words());
        s.setWpm(m.wpm());
        s.setFillerCount(m.fillerCount());
        s.setFillers(m.fillers());
        s.setLongPauses(req.longPauses());
        Map<String, Integer> scores = new LinkedHashMap<>(m.scores());
        s.setScores(scores);
        s.setOverallScore(BigDecimal.valueOf(m.overall()));
        s.setLocalDate(time.today(userId));

        String notice = null;
        String followUp = null;
        if (req.useAi()) {
            Optional<JsonNode> fb = aiFeedback(userId, s, framework, req.mode());
            if (fb.isPresent()) {
                JsonNode n = fb.get();
                for (String k : List.of("structure", "clarity", "grammar", "vocabulary", "confidence")) {
                    scores.put(k, (int) Math.max(0, Math.min(10, n.path(k).asDouble(5))));
                }
                s.setScores(scores);
                s.setFeedback(n.path("feedback").asText(null));
                s.setImprovedAnswer(n.path("improvedAnswer").asText(null));
                followUp = n.path("followUpQuestion").asText(null);
                s.setAiUsed(true);
                double ai10 = scores.entrySet().stream().filter(e -> List.of("structure", "clarity", "grammar", "vocabulary", "confidence").contains(e.getKey()))
                        .mapToInt(Map.Entry::getValue).average().orElse(5);
                // Blend: half local delivery metrics, half AI content feedback.
                s.setOverallScore(BigDecimal.valueOf(Math.round((m.overall() * 0.5 + ai10 * 10 * 0.5) * 10) / 10.0));
            } else {
                notice = ai.usable() ? AiService.RESTING : AiService.OFF;
            }
        }
        if (s.getFeedback() == null) s.setFeedback(localFeedback(m, framework));
        sessions.save(s);
        XpAward award = m.words() >= 30
                ? xp.award(userId, XpAction.SPEAKING_SESSION, "speak:" + s.getId(), s.getPromptText())
                : XpAward.none("Speak at least 30 words to earn XP.");
        quests.advance(userId, QuestTrigger.SPEAKING_DONE);
        return new SessionSaved(view(s, followUp), award, notice);
    }

    private Optional<JsonNode> aiFeedback(Long userId, SpeakingSession s, String framework, String mode) {
        String fw = switch (framework) {
            case "STAR" -> "STAR (Situation, Task, Action, Result)";
            case "PPF" -> "Present -> Past -> Future";
            case "CEU" -> "Concept -> Example -> Use case";
            default -> "clear beginning, middle, end";
        };
        String system = MentorService.SYSTEM + "\nYou are an interview communication coach. Score 0-10 for structure, clarity, grammar, vocabulary, confidence. "
                + "Expected framework: " + fw + ". JSON shape: {\"structure\":n,\"clarity\":n,\"grammar\":n,\"vocabulary\":n,\"confidence\":n,"
                + "\"feedback\":\"3-5 short kind bullet points\",\"improvedAnswer\":\"a better version in the learner's voice, simple words, under 150 words\","
                + "\"followUpQuestion\":" + ("EXPLAIN_ALOUD".equals(mode) ? "\"one interviewer-style follow-up question\"" : "null") + "}";
        String user = "Question: " + s.getPromptText() + "\nTranscript (speech-to-text, may have recognition errors): " + MentorService.trim(s.getTranscript(), 3000)
                + "\nMeasured: " + s.getWpm() + " wpm, " + s.getFillerCount() + " fillers, " + s.getDurationSeconds() + "s.";
        return ai.askJson(userId, "speaking", system, List.of(new Message("user", user)),
                n -> n.path("structure").isNumber() && n.path("clarity").isNumber() && n.path("feedback").isTextual(), null);
    }

    static String localFeedback(SpeakingMetrics.Result m, String framework) {
        List<String> tips = new ArrayList<>();
        if (m.wpm() < 100) tips.add("Pace: " + m.wpm() + " words/min is a little slow. Aim for 110-160: practise the first sentence until it flows.");
        else if (m.wpm() > 165) tips.add("Pace: " + m.wpm() + " words/min is fast. Pause after each key point; slow is confident.");
        else tips.add("Pace: " + m.wpm() + " words/min is in the comfortable range. Nice!");
        if (m.fillerCount() > 0) {
            String top = m.fillers().entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("");
            tips.add("Fillers: " + m.fillerCount() + " (most: \"" + top + "\"). Replace them with a short silent pause.");
        } else tips.add("Fillers: none detected. Great control!");
        if (m.scores().getOrDefault("structure", 0) < 7) {
            tips.add(switch (framework) {
                case "STAR" -> "Structure: say the Situation, your Task, the Action YOU took, and the Result, in that order.";
                case "PPF" -> "Structure: Present (who you are now) → Past (what you did) → Future (what you want).";
                case "CEU" -> "Structure: Concept (what it is) → Example (a tiny one) → Use case (where it's used).";
                default -> "Structure: use signposts like 'First…', 'Then…', 'Finally…'.";
            });
        } else tips.add("Structure: easy to follow. Keep using the framework.");
        if (m.words() < 40) tips.add("Length: a bit short. Add one concrete example.");
        return String.join("\n", tips);
    }

    @Transactional(readOnly = true)
    public PageResponse<SessionView> history(Long userId, int page, int size) {
        return PageResponse.of(sessions.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size)), s -> view(s, null));
    }

    @Transactional(readOnly = true)
    public Trends trends(Long userId) {
        List<LocalDate> days = jdbc.queryForList("select distinct local_date from speaking_sessions where user_id=? order by 1 desc", LocalDate.class, userId);
        LocalDate today = time.today(userId);
        int streak = 0;
        LocalDate expect = days.isEmpty() || !days.get(0).isBefore(today.minusDays(1)) ? (days.isEmpty() ? null : days.get(0)) : null;
        for (LocalDate d : days) {
            if (expect == null || !d.equals(expect)) break;
            streak++;
            expect = d.minusDays(1);
        }
        List<Map<String, Object>> weekly = jdbc.queryForList("""
                select date_trunc('week', local_date)::date as week, count(*) as sessions, round(avg(wpm)) as wpm,
                       round(avg(filler_count),1) as fillers, round(avg(overall_score),1) as score,
                       round(avg((scores->>'confidence')::numeric),1) as confidence
                from speaking_sessions where user_id=? group by 1 order by 1""", userId);
        return new Trends(streak, (int) sessions.count(), weekly);
    }

    static PromptView view(SpeakingPrompt p) {
        return new PromptView(p.getId(), p.getCategory(), p.getPrompt(), p.getFramework(), p.getTimeLimitSeconds(), p.getTips(), p.getSampleAnswer());
    }

    static SessionView view(SpeakingSession s, String followUp) {
        return new SessionView(s.getId(), s.getMode(), s.getPromptText(), s.getTranscript(), s.getInputMethod(), s.getDurationSeconds(),
                s.getWordCount(), s.getWpm(), s.getFillerCount(), s.getFillers(), s.getLongPauses(), s.getScores(), s.getOverallScore(),
                s.getFeedback(), s.getImprovedAnswer(), followUp, s.isAiUsed(), s.getLocalDate(), s.getCreatedAt());
    }
}
