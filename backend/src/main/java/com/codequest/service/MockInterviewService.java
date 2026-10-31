package com.codequest.service;

import com.codequest.ai.AiService;
import com.codequest.ai.ChatClient.Message;
import com.codequest.domain.XpAction;
import com.codequest.dto.SpeakDtos.*;
import com.codequest.entity.MockInterview;
import com.codequest.entity.QuestionBankItem;
import com.codequest.exception.ApiException;
import com.codequest.repository.MockInterviewRepository;
import com.codequest.repository.ProfileRepository;
import com.codequest.repository.QuestionBankRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Mock Interview "boss fight": 5 questions from the practice bank, spoken by the browser's text-to-speech.
 * With AI: the interviewer may ask one short follow-up per question, and writes the scorecard.
 * Without AI: questions come one after another and the scorecard measures key-point coverage.
 */
@Service
public class MockInterviewService {
    static final int QUESTIONS = 5;
    static final Map<String, List<String>> ROUND_CATEGORIES = Map.of(
            "HR", List.of("HR", "BEHAVIORAL", "PROJECT"),
            "TECHNICAL", List.of("JAVA", "OOP", "COLLECTIONS", "EXCEPTIONS", "JAVA8", "SQL", "DBMS", "OS", "DSA"),
            "SPRING", List.of("SPRING"));

    private final MockInterviewRepository interviews;
    private final QuestionBankRepository bank;
    private final ProfileRepository profiles;
    private final AiService ai;
    private final XpService xp;
    private final TimeService time;
    private final ObjectMapper json;

    public MockInterviewService(MockInterviewRepository interviews, QuestionBankRepository bank, ProfileRepository profiles,
                                AiService ai, XpService xp, TimeService time, ObjectMapper json) {
        this.interviews = interviews;
        this.bank = bank;
        this.profiles = profiles;
        this.ai = ai;
        this.xp = xp;
        this.time = time;
        this.json = json;
    }

    @Transactional
    public InterviewTurn start(Long userId, StartInterviewRequest req) {
        String company = profiles.findByUserId(userId).map(p -> p.getPrimaryCompany()).orElse("INFOSYS_SP");
        List<String> cats = ROUND_CATEGORIES.get(req.roundType());
        final List<QuestionBankItem> pool = new ArrayList<>(bank.findAll().stream()
                .filter(q -> cats.contains(q.getCategory()) && q.relevantTo(company)).toList());
        if (pool.size() < QUESTIONS) { pool.clear(); pool.addAll(bank.findAll().stream().filter(q -> cats.contains(q.getCategory())).toList()); }
        if (pool.isEmpty()) throw ApiException.badRequest("No practice questions loaded for this round yet");
        Collections.shuffle(pool);
        if ("HR".equals(req.roundType())) {
            // HR rounds usually open with "Tell me about yourself": put it first if we have it.
            pool.stream().filter(q -> q.getQuestion().toLowerCase().contains("about yourself")).findFirst().ifPresent(q -> {
                pool.remove(q);
                pool.add(0, q);
            });
        }
        List<QuestionBankItem> picked = pool.subList(0, Math.min(QUESTIONS, pool.size()));

        MockInterview mi = new MockInterview();
        mi.setUserId(userId);
        mi.setRoundType(req.roundType());
        mi.setStatus("RUNNING");
        mi.setQuestionIds(picked.stream().map(QuestionBankItem::getId).toList());
        ArrayNode t = json.createArrayNode();
        String opener = "Hello, and welcome. This is a practice " + req.roundType().toLowerCase() + " round with " + picked.size()
                + " questions. Take your time and think out loud. First question: " + picked.get(0).getQuestion();
        t.addObject().put("role", "interviewer").put("text", opener).put("questionId", picked.get(0).getId());
        mi.setTranscript(t);
        mi.setStartedAt(time.now());
        interviews.save(mi);
        return new InterviewTurn(mi.getId(), opener, 1, picked.size(), false, false, null);
    }

    @Transactional
    public InterviewTurn answer(Long userId, Long id, InterviewAnswerRequest req) {
        MockInterview mi = load(userId, id);
        if (!"RUNNING".equals(mi.getStatus())) throw ApiException.conflict("This interview is finished");
        ArrayNode t = (ArrayNode) mi.getTranscript().deepCopy();
        JsonNode lastInterviewer = lastOf(t, "interviewer");
        long qid = lastInterviewer.path("questionId").asLong();
        boolean wasFollowUp = lastInterviewer.path("followUp").asBoolean(false);
        t.addObject().put("role", "candidate").put("text", req.answer().trim()).put("questionId", qid).put("seconds", req.durationSeconds());

        int idx = mi.getQuestionIds().indexOf(qid);
        String next;
        boolean aiUsed = false;
        String notice = null;
        boolean finished = false;
        // One AI follow-up per main question, if the AI is available.
        if (!wasFollowUp && ai.usable()) {
            QuestionBankItem q = bank.findById(qid).orElseThrow();
            var r = ai.ask(userId, "interview-followup", MentorService.SYSTEM
                            + "\nYou are a polite interviewer. Ask ONE short follow-up question (max 25 words) that probes the candidate's answer. Output only the question.",
                    List.of(new Message("user", "Question: " + q.getQuestion() + "\nAnswer: " + MentorService.trim(req.answer(), 2000))), null);
            if (r.ok()) {
                next = r.text().trim();
                aiUsed = true;
                t.addObject().put("role", "interviewer").put("text", next).put("questionId", qid).put("followUp", true);
                mi.setTranscript(t);
                return new InterviewTurn(mi.getId(), next, idx + 1, mi.getQuestionIds().size(), false, true, null);
            }
            notice = r.userMessage();
        }
        if (idx + 1 < mi.getQuestionIds().size()) {
            QuestionBankItem nq = bank.findById(mi.getQuestionIds().get(idx + 1)).orElseThrow();
            next = (idx + 2 == mi.getQuestionIds().size() ? "Last question. " : "Thank you. Next question. ") + nq.getQuestion();
            t.addObject().put("role", "interviewer").put("text", next).put("questionId", nq.getId());
        } else {
            next = "Thank you, that's the end of the interview. Let's look at your scorecard.";
            t.addObject().put("role", "interviewer").put("text", next);
            finished = true;
        }
        mi.setTranscript(t);
        return new InterviewTurn(mi.getId(), next, Math.min(idx + 2, mi.getQuestionIds().size()), mi.getQuestionIds().size(), finished, aiUsed, notice);
    }

    @Transactional
    public InterviewView finish(Long userId, Long id) {
        MockInterview mi = load(userId, id);
        if ("COMPLETED".equals(mi.getStatus())) return view(mi);
        Map<Long, QuestionBankItem> qs = new HashMap<>();
        bank.findAllById(mi.getQuestionIds()).forEach(q -> qs.put(q.getId(), q));
        Map<Long, StringBuilder> answers = new LinkedHashMap<>();
        for (JsonNode e : mi.getTranscript()) {
            if ("candidate".equals(e.path("role").asText())) {
                answers.computeIfAbsent(e.path("questionId").asLong(), k -> new StringBuilder()).append(e.path("text").asText()).append(' ');
            }
        }
        if (answers.isEmpty()) throw ApiException.badRequest("Answer at least one question before finishing");

        ObjectNode card = json.createObjectNode();
        ArrayNode per = card.putArray("questions");
        double sum = 0;
        for (Long qid : mi.getQuestionIds()) {
            QuestionBankItem q = qs.get(qid);
            String a = answers.getOrDefault(qid, new StringBuilder()).toString();
            double cov = coverage(q.getKeyPoints(), a);
            sum += cov;
            ObjectNode o = per.addObject();
            o.put("question", q.getQuestion());
            o.put("score", Math.round(cov * 10));
            o.put("answered", !a.isBlank());
            o.put("modelAnswer", q.getAnswer());
            ArrayNode missing = o.putArray("missingPoints");
            for (String kp : q.getKeyPoints()) if (coverage(List.of(kp), a) < 0.5) missing.add(kp);
        }
        double overall = sum / mi.getQuestionIds().size() * 100;
        card.put("method", "Key-point coverage (no AI)");

        // Optional AI scorecard on top.
        StringBuilder transcript = new StringBuilder();
        for (JsonNode e : mi.getTranscript()) transcript.append(e.path("role").asText()).append(": ").append(e.path("text").asText()).append('\n');
        Optional<JsonNode> aiCard = ai.askJson(userId, "interview-scorecard", MentorService.SYSTEM
                        + "\nYou evaluate a practice interview. JSON shape: {\"overall\":0-100,\"communication\":0-10,\"technicalDepth\":0-10,"
                        + "\"confidence\":0-10,\"strengths\":[\"...\"],\"improve\":[\"...\"],\"summary\":\"2-3 kind sentences\"}",
                List.of(new Message("user", MentorService.trim(transcript.toString(), 6000))),
                n -> n.path("overall").isNumber() && n.path("strengths").isArray(), null);
        if (aiCard.isPresent()) {
            card.set("ai", aiCard.get());
            card.put("method", "AI scorecard + key-point coverage (AI can be wrong; compare with the model answers)");
            overall = overall * 0.5 + Math.max(0, Math.min(100, aiCard.get().path("overall").asDouble())) * 0.5;
            mi.setAiUsed(true);
        }
        mi.setScorecard(card);
        mi.setOverallScore(BigDecimal.valueOf(overall).setScale(1, RoundingMode.HALF_UP));
        mi.setStatus("COMPLETED");
        mi.setEndedAt(time.now());
        xp.award(userId, XpAction.MOCK_INTERVIEW, "mockint:" + mi.getId(), mi.getRoundType());
        return view(mi);
    }

    /** Fraction of key points whose important words appear in the answer (simple, transparent, no AI). */
    static double coverage(List<String> keyPoints, String answer) {
        if (keyPoints == null || keyPoints.isEmpty()) return answer.isBlank() ? 0 : 0.5;
        String a = answer.toLowerCase();
        double hit = 0;
        for (String kp : keyPoints) {
            List<String> words = Arrays.stream(kp.toLowerCase().split("[^a-z0-9]+")).filter(w -> w.length() > 3).toList();
            if (words.isEmpty()) continue;
            long found = words.stream().filter(a::contains).count();
            hit += found >= Math.max(1, Math.ceil(words.size() * 0.5)) ? 1 : found > 0 ? 0.5 : 0;
        }
        return hit / keyPoints.size();
    }

    @Transactional(readOnly = true)
    public List<InterviewView> history(Long userId) {
        return interviews.findTop20ByUserIdOrderByStartedAtDesc(userId).stream().map(MockInterviewService::view).toList();
    }

    @Transactional(readOnly = true)
    public InterviewView get(Long userId, Long id) {
        return view(load(userId, id));
    }

    private MockInterview load(Long userId, Long id) {
        return interviews.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Mock interview"));
    }

    private static JsonNode lastOf(ArrayNode t, String role) {
        for (int i = t.size() - 1; i >= 0; i--) if (role.equals(t.get(i).path("role").asText())) return t.get(i);
        return t.get(0);
    }

    static InterviewView view(MockInterview m) {
        return new InterviewView(m.getId(), m.getRoundType(), m.getStatus(), m.getTranscript(), m.getScorecard(), m.getOverallScore(),
                m.isAiUsed(), m.getStartedAt(), m.getEndedAt());
    }
}
