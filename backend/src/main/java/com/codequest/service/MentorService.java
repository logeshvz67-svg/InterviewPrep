package com.codequest.service;

import com.codequest.ai.AiService;
import com.codequest.ai.AiService.AiResult;
import com.codequest.ai.ChatClient.Message;
import com.codequest.ai.AiProperties;
import com.codequest.dto.AiDtos.*;
import com.codequest.dto.LearningDtos.ExplainBackRequest;
import com.codequest.dto.LearningDtos.ExplainBackResult;
import com.codequest.dto.LearningDtos.FlashcardView;
import com.codequest.dto.LearningDtos.ManualCardRequest;
import com.codequest.dto.PageResponse;
import com.codequest.entity.*;
import com.codequest.exception.ApiException;
import com.codequest.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * AI Mentor features. EVERY feature works without AI:
 *   doubt chat     -> related lesson + practice-answer search
 *   hint ladder    -> static hints per pattern
 *   code review    -> self-review checklist
 *   quiz generator -> questions from the seeded lessons
 *   explain-back   -> rubric self-check
 *   briefing       -> template built from your data
 * Prompts are SHORT: a compact system prompt + the last N messages + a one-line weak-topic summary.
 */
@Service
public class MentorService {
    static final String SYSTEM = """
            You are "Mentor" in Code Quest, a friendly Java interview coach for a beginner preparing for Infosys \
            (Specialist Programmer / Digital Specialist Engineer) and other IT companies.
            Rules: simple words, short sentences, one everyday analogy, small Java 17 code only when it helps. \
            Be honest: if unsure, say "I'm not sure, verify in the lesson." Never claim a question was actually \
            asked at any company and never state current test patterns. Encourage understanding over memorizing. \
            Keep answers under 200 words unless asked. End with ONE short check question.""";

    private final AiService ai;
    private final AiProperties props;
    private final AiConversationRepository conversations;
    private final AiMessageRepository messages;
    private final LessonRepository lessons;
    private final QuizQuestionRepository questions;
    private final QuestionBankRepository bank;
    private final PatternRepository patterns;
    private final LessonService lessonService;
    private final FlashcardService flashcards;
    private final GymService gym;
    private final MistakeService mistakes;
    private final StreakService streaks;
    private final XpService xp;
    private final StatsService stats;
    private final TimeService time;

    public MentorService(AiService ai, AiProperties props, AiConversationRepository conversations, AiMessageRepository messages,
                         LessonRepository lessons, QuizQuestionRepository questions, QuestionBankRepository bank, PatternRepository patterns,
                         LessonService lessonService, FlashcardService flashcards, GymService gym, MistakeService mistakes,
                         StreakService streaks, XpService xp, StatsService stats, TimeService time) {
        this.ai = ai;
        this.props = props;
        this.conversations = conversations;
        this.messages = messages;
        this.lessons = lessons;
        this.questions = questions;
        this.bank = bank;
        this.patterns = patterns;
        this.lessonService = lessonService;
        this.flashcards = flashcards;
        this.gym = gym;
        this.mistakes = mistakes;
        this.streaks = streaks;
        this.xp = xp;
        this.stats = stats;
        this.time = time;
    }

    // ------------------------------------------------------------------ chat (doubts, Socratic, code review, mistakes)

    @Transactional
    public ChatResponse chat(Long userId, ChatRequest req) {
        AiConversation c = req.conversationId() == null ? newConversation(userId, req) :
                conversations.findByIdAndUserId(req.conversationId(), userId).orElseThrow(() -> ApiException.notFound("Conversation"));
        messages.save(new AiMessage(c.getId(), userId, "user", req.message().trim(), null, false));

        List<AiMessage> recent = new ArrayList<>(messages.findByConversationIdOrderByIdDesc(c.getId(), PageRequest.of(0, Math.max(2, props.historyMessages()))));
        Collections.reverse(recent);
        List<Message> convo = recent.stream().map(m -> new Message(m.getRole(), m.getContent())).toList();
        String system = SYSTEM + "\n" + modeRules(c.getMode()) + "\nLearner's weak areas: " + weakSummary(userId);
        // Only stand-alone first questions in DOUBT mode are generic enough to cache.
        String cacheKey = "DOUBT".equals(c.getMode()) && recent.size() == 1 ? req.message() : null;
        AiResult r = ai.ask(userId, "chat-" + c.getMode().toLowerCase(), system, convo, cacheKey);

        AiMessage reply;
        String notice = null;
        if (r.ok()) {
            reply = messages.save(new AiMessage(c.getId(), userId, "assistant", r.text() + "\n\n_Tip: verify with the lesson; AI can be wrong._", r.model(), false));
        } else {
            notice = r.userMessage();
            reply = messages.save(new AiMessage(c.getId(), userId, "assistant", fallbackAnswer(c.getMode(), req.message()), null, true));
        }
        c.touch();
        streaks.recordActivity(userId);
        return new ChatResponse(c.getId(), view(reply), r.ok(), notice);
    }

    private AiConversation newConversation(Long userId, ChatRequest req) {
        AiConversation c = new AiConversation();
        c.setUserId(userId);
        c.setMode(req.mode());
        String t = req.message().trim().replaceAll("\\s+", " ");
        c.setTitle(t.length() > 80 ? t.substring(0, 80) + "…" : t);
        c.setTopic(req.topic());
        return conversations.save(c);
    }

    private static String modeRules(String mode) {
        return switch (mode) {
            case "SOCRATIC" -> "Socratic mode: do NOT give the solution. Ask one guiding question at a time and give at most a small nudge.";
            case "CODE_REVIEW" -> "Code review mode: list correctness bugs first, then edge cases, then complexity (Big-O), then naming/readability. Show only the lines that change.";
            case "MISTAKE" -> "Mistake explainer: first ask what the learner was thinking if they haven't said it; then explain the misconception kindly and give one tiny example.";
            default -> "Doubt mode: explain clearly with an analogy and a tiny example.";
        };
    }

    /** Non-AI answer: point to lessons and practice answers that match the question's words. */
    String fallbackAnswer(String mode, String question) {
        if ("CODE_REVIEW".equals(mode)) return CODE_REVIEW_CHECKLIST;
        StringBuilder sb = new StringBuilder("The AI mentor isn't available right now, but here's what the course says:\n\n");
        List<String> words = keywords(question);
        List<Lesson> matches = lessons.findAllByOrderByWorldCodeAscSectionAscSortOrderAsc().stream()
                .filter(l -> words.stream().anyMatch(w -> l.getTitle().toLowerCase().contains(w)))
                .limit(3).toList();
        for (Lesson l : matches) sb.append("📖 **").append(l.getTitle()).append("** (open it from Learn → ").append(l.getWorldCode().replace('_', ' ')).append(")\n");
        Optional<QuestionBankItem> qa = bank.findAll().stream()
                .max(Comparator.comparingLong(q -> words.stream().filter(w -> (q.getQuestion() + " " + q.getTopic()).toLowerCase().contains(w)).count()))
                .filter(q -> words.stream().anyMatch(w -> (q.getQuestion() + " " + q.getTopic()).toLowerCase().contains(w)));
        qa.ifPresent(q -> sb.append("\n**Practice answer: ").append(q.getQuestion()).append("**\n").append(q.getAnswer()).append('\n'));
        if (matches.isEmpty() && qa.isEmpty()) sb.append("I couldn't find a matching lesson. Try different keywords, or check the Java Core Island map.");
        sb.append("\n_Check question: can you explain the main idea in one sentence of your own?_");
        return sb.toString();
    }

    static List<String> keywords(String q) {
        Set<String> stop = Set.of("what", "is", "the", "a", "an", "and", "or", "of", "in", "to", "how", "why", "does", "do", "difference",
                "between", "java", "explain", "me", "i", "can", "you", "please", "with", "for", "vs", "it", "this", "that", "are", "use");
        return Arrays.stream(q.toLowerCase().split("[^a-z0-9#+]+")).filter(w -> w.length() > 2 && !stop.contains(w)).distinct().limit(6).toList();
    }

    static final String CODE_REVIEW_CHECKLIST = """
            The AI reviewer isn't available right now. Review your own code with this checklist (it catches most bugs):
            1. **Correctness:** dry-run it on the sample input. Does every branch return the right thing?
            2. **Edge cases:** empty input, one element, all equal, negatives, maximum size.
            3. **Off-by-one:** check every loop bound (< vs <=) and every index (i+1, n-1).
            4. **Null safety:** what if a map lookup or input is null?
            5. **Overflow:** can sums/products exceed int? Use long.
            6. **Complexity:** say the Big-O of time and space. Does it fit the input limits?
            7. **Readability:** clear names, no dead code, one job per method.""";

    @Transactional(readOnly = true)
    public PageResponse<ConversationView> history(Long userId, String q, int page, int size) {
        var pageable = PageRequest.of(page, size);
        var result = q == null || q.isBlank() ? conversations.findByUserIdOrderByUpdatedAtDesc(userId, pageable) : conversations.search(userId, q.trim(), pageable);
        return PageResponse.of(result, c -> new ConversationView(c.getId(), c.getMode(), c.getTitle(), c.getTopic(), c.isResolved(), c.getHintLevel(), c.getUpdatedAt(), List.of()));
    }

    @Transactional(readOnly = true)
    public ConversationView conversation(Long userId, Long id) {
        AiConversation c = conversations.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Conversation"));
        return new ConversationView(c.getId(), c.getMode(), c.getTitle(), c.getTopic(), c.isResolved(), c.getHintLevel(), c.getUpdatedAt(),
                messages.findByConversationIdOrderByIdAsc(c.getId()).stream().map(MentorService::view).toList());
    }

    @Transactional
    public ConversationView setResolved(Long userId, Long id, boolean resolved) {
        AiConversation c = conversations.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Conversation"));
        c.setResolved(resolved);
        return conversation(userId, id);
    }

    @Transactional
    public void deleteConversation(Long userId, Long id) {
        conversations.delete(conversations.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Conversation")));
    }

    /** "Convert to flashcard": the question becomes the front, the answer the back. */
    @Transactional
    public FlashcardView messageToFlashcard(Long userId, Long messageId) {
        AiMessage answer = messages.findByIdAndUserId(messageId, userId).orElseThrow(() -> ApiException.notFound("Message"));
        if (!"assistant".equals(answer.getRole())) throw ApiException.badRequest("Pick a mentor answer to turn into a flashcard");
        List<AiMessage> all = messages.findByConversationIdOrderByIdAsc(answer.getConversationId());
        String front = "Explain this:";
        for (AiMessage m : all) {
            if (m.getId().equals(answer.getId())) break;
            if ("user".equals(m.getRole())) front = m.getContent();
        }
        String back = answer.getContent().replace("\n\n_Tip: verify with the lesson; AI can be wrong._", "");
        return flashcards.createManual(userId, new ManualCardRequest(trim(front, 500), trim(back, 2000), "Mentor"), "AI", "ai-msg:" + messageId);
    }

    // ------------------------------------------------------------------ hint ladder

    static final String[] LEVEL_NAMES = {"", "Nudge", "Pattern name", "Pseudo-code", "Full approach"};

    @Transactional
    public HintResponse hint(Long userId, HintRequest req) {
        String levelGoal = switch (req.level()) {
            case 1 -> "Give ONE small nudge question that points toward the key observation. No pattern names, no code.";
            case 2 -> "Name the pattern/data structure that fits and say in one sentence why. No code.";
            case 3 -> "Give short numbered pseudo-code (not Java). No full solution.";
            default -> "Explain the full approach step by step with time/space complexity, then a compact Java solution.";
        };
        String user = "Problem: " + req.problemTitle() + "\n" + (req.problemText() == null ? "" : trim(req.problemText(), 1500))
                + (req.myAttempt() == null || req.myAttempt().isBlank() ? "" : "\nMy attempt/idea so far: " + trim(req.myAttempt(), 1200));
        AiResult r = ai.ask(userId, "hint", SYSTEM + "\nHint ladder level " + req.level() + ": " + levelGoal, List.of(new Message("user", user)), null);
        if (r.ok()) return new HintResponse(req.level(), LEVEL_NAMES[req.level()], r.text(), true, null);
        return new HintResponse(req.level(), LEVEL_NAMES[req.level()], staticHint(req.patternCode(), req.level()), false, r.userMessage());
    }

    String staticHint(String patternCode, int level) {
        Pattern p = patternCode == null ? null : patterns.findByCodeIn(List.of(patternCode)).stream().findFirst().orElse(null);
        String name = p == null ? "the right pattern" : p.getName();
        return switch (level) {
            case 1 -> "What do you need to remember as you walk through the input once? Write the brute force first and say its Big-O: what work is repeated?";
            case 2 -> p == null ? "Look at the Pattern Map: which pattern removes the repeated work you found?" : "Try **" + name + "**: " + p.getDescription();
            case 3 -> STATIC_PSEUDO.getOrDefault(patternCode, "1. Write the brute force.\n2. Find what's recomputed.\n3. Store it (map/array/pointer) so each step is O(1).\n4. Handle edge cases.\n5. Dry-run on the example.");
            default -> "Full solutions aren't stored in the app (to keep you solving). Try once more with the pseudo-code; if still stuck, read the platform's editorial, then re-solve from scratch in 3 days (it's in your revisit queue).";
        };
    }

    static final Map<String, String> STATIC_PSEUDO = Map.ofEntries(
            Map.entry("TWO_POINTERS", "1. Sort if needed.\n2. left = 0, right = n-1.\n3. While left < right: compare; move the pointer that improves the answer.\n4. Record answers as you go."),
            Map.entry("SLIDING_WINDOW", "1. left = 0.\n2. For right in 0..n-1: add a[right] to the window.\n3. While the window breaks the rule: remove a[left], left++.\n4. Update the best answer with the window size/sum."),
            Map.entry("PREFIX_SUM", "1. prefix[0] = 0; prefix[i+1] = prefix[i] + a[i].\n2. Sum(l..r) = prefix[r+1] - prefix[l].\n3. For 'subarray sum = k': count prefix values in a HashMap."),
            Map.entry("HASHING", "1. Create a HashMap/HashSet.\n2. For each element: check if what you need is already in the map.\n3. Then add the current element.\n4. Return when found."),
            Map.entry("BINARY_SEARCH", "1. lo, hi = search range.\n2. While lo <= hi: mid = lo + (hi-lo)/2.\n3. If condition(mid) move hi = mid-1 else lo = mid+1.\n4. Answer is lo (or the last valid mid)."),
            Map.entry("STACK_QUEUE", "1. Use a Deque as a stack.\n2. For each item: while stack top is 'worse' than current, pop (and record answer).\n3. Push current.\n4. Leftovers have no answer."),
            Map.entry("LINKED_LIST", "1. Use a dummy head.\n2. Use slow/fast pointers or prev/curr/next.\n3. Re-link carefully: save next before changing curr.next.\n4. Return dummy.next."),
            Map.entry("TREES", "1. Base case: node == null.\n2. Get answers from left and right recursively.\n3. Combine them for this node.\n4. Return the combined answer."),
            Map.entry("GRAPHS", "1. Build adjacency list.\n2. visited set.\n3. BFS with a queue (shortest steps) or DFS (explore all).\n4. Process neighbours not yet visited."),
            Map.entry("DP", "1. Define dp[i] in words.\n2. Base cases.\n3. Transition: dp[i] from smaller states.\n4. Order the loops so smaller states are ready.\n5. Answer is dp[n] (or max over dp)."),
            Map.entry("BACKTRACKING", "1. solve(index, current choice list).\n2. If complete: record answer, return.\n3. For each option: choose, solve(next), un-choose."),
            Map.entry("RECURSION", "1. Base case (smallest input).\n2. Assume the function works for a smaller input.\n3. Build the answer for n from the smaller answer."),
            Map.entry("GREEDY", "1. Sort by the key that makes the local best choice safe.\n2. Walk once, taking the best choice each time.\n3. Convince yourself why the choice is never wrong."),
            Map.entry("HEAPS", "1. PriorityQueue of size k.\n2. Add each element; if size > k, poll.\n3. The heap holds the top-k."),
            Map.entry("SORTING", "1. Sort with a comparator that matches the goal.\n2. Walk once, comparing neighbours/merging intervals."),
            Map.entry("BIT_MANIPULATION", "1. Think in binary.\n2. Use XOR to cancel pairs, & to test bits, << >> to shift.\n3. n & (n-1) removes the lowest set bit."));

    // ------------------------------------------------------------------ code review (one-shot)

    @Transactional
    public ChatResponse codeReview(Long userId, CodeReviewRequest req) {
        String msg = (req.problem() == null ? "" : "Problem: " + req.problem() + "\n") + "Review my Java code:\n```java\n" + req.code() + "\n```";
        return chat(userId, new ChatRequest(null, "CODE_REVIEW", msg, req.problem()));
    }

    // ------------------------------------------------------------------ quiz generator

    @Transactional(readOnly = true)
    public QuizGenResponse generateQuiz(Long userId, QuizGenRequest req) {
        String system = SYSTEM + "\nCreate " + req.count() + " multiple-choice questions on the topic for a beginner preparing for interviews. "
                + "JSON shape: {\"questions\":[{\"prompt\":\"...\",\"code\":null,\"options\":[\"a\",\"b\",\"c\",\"d\"],\"answer\":0,\"explanation\":\"...\"}]}. "
                + "answer is the 0-based index. Exactly 4 options. Code (if any) must compile.";
        Optional<JsonNode> out = ai.askJson(userId, "quiz-gen", system, List.of(new Message("user", "Topic: " + req.topic())),
                n -> validQuiz(n), "quiz:" + req.topic() + ":" + req.count());
        if (out.isPresent()) {
            List<GeneratedQuestion> qs = new ArrayList<>();
            for (JsonNode q : out.get().path("questions")) {
                List<String> opts = new ArrayList<>();
                q.path("options").forEach(o -> opts.add(o.asText()));
                qs.add(new GeneratedQuestion(q.path("prompt").asText(), q.path("code").isTextual() ? q.path("code").asText() : null, opts,
                        q.path("answer").asInt(), q.path("explanation").asText()));
            }
            return new QuizGenResponse(req.topic(), qs, true, "AI (verify with the lesson)", null);
        }
        return new QuizGenResponse(req.topic(), seededQuiz(req.topic(), req.count()), false, "Seeded lesson questions",
                ai.usable() ? AiService.RESTING : AiService.OFF);
    }

    static boolean validQuiz(JsonNode n) {
        JsonNode qs = n.path("questions");
        if (!qs.isArray() || qs.isEmpty()) return false;
        for (JsonNode q : qs) {
            if (q.path("prompt").asText("").isBlank() || q.path("options").size() != 4) return false;
            int a = q.path("answer").asInt(-1);
            if (a < 0 || a > 3 || q.path("explanation").asText("").isBlank()) return false;
        }
        return true;
    }

    private List<GeneratedQuestion> seededQuiz(String topic, int count) {
        List<String> words = keywords(topic);
        List<Long> lessonIds = lessons.findAllByOrderByWorldCodeAscSectionAscSortOrderAsc().stream()
                .filter(l -> words.isEmpty() || words.stream().anyMatch(w -> (l.getTitle() + " " + l.getSlug()).toLowerCase().contains(w)))
                .map(BaseEntity::getId).toList();
        List<QuizQuestion> pool = new ArrayList<>(questions.findByLessonIdIn(lessonIds.isEmpty()
                ? lessons.findAll().stream().map(BaseEntity::getId).toList() : lessonIds));
        pool.removeIf(q -> !com.codequest.domain.QuizGrader.isOptionType(q.getType()));
        Collections.shuffle(pool);
        List<GeneratedQuestion> out = new ArrayList<>();
        for (QuizQuestion q : pool.subList(0, Math.min(count, pool.size()))) {
            List<String> opts = new ArrayList<>();
            q.getOptions().forEach(o -> opts.add(o.asText()));
            out.add(new GeneratedQuestion(q.getPrompt(), q.getCode(), opts, q.getAnswer().path("index").asInt(), q.getExplanation()));
        }
        return out;
    }

    // ------------------------------------------------------------------ explain it back scoring

    /** AI scores accuracy and clarity (JSON); without AI the learner's rubric self-check is used. */
    public ExplainScore scoreExplanation(Long userId, String concept, String referenceAnswer, List<String> rubric, String text) {
        String system = SYSTEM + "\nYou grade a learner's spoken-style explanation. JSON shape: "
                + "{\"accuracy\":0-10,\"clarity\":0-10,\"missing\":[\"...\"],\"feedback\":\"2-4 kind, specific sentences\"}. Be fair, not harsh.";
        String user = "Concept: " + concept + "\nKey points: " + String.join("; ", rubric) + "\nReference: " + trim(referenceAnswer, 800)
                + "\nLearner's explanation: " + trim(text, 2500);
        Optional<JsonNode> out = ai.askJson(userId, "explain-back", system, List.of(new Message("user", user)),
                n -> n.path("accuracy").isNumber() && n.path("clarity").isNumber(), null);
        if (out.isEmpty()) return new ExplainScore(null, null, false);
        JsonNode n = out.get();
        double acc = clamp(n.path("accuracy").asDouble(), 0, 10);
        double cla = clamp(n.path("clarity").asDouble(), 0, 10);
        BigDecimal score = BigDecimal.valueOf((acc * 0.7 + cla * 0.3) * 10).setScale(2, RoundingMode.HALF_UP);
        StringBuilder fb = new StringBuilder(n.path("feedback").asText(""));
        if (n.path("missing").isArray() && !n.path("missing").isEmpty()) {
            fb.append("\n\nYou could add: ");
            List<String> miss = new ArrayList<>();
            n.path("missing").forEach(m -> miss.add(m.asText()));
            fb.append(String.join("; ", miss));
        }
        fb.append("\n\nAccuracy ").append((int) acc).append("/10 · Clarity ").append((int) cla).append("/10 (AI feedback: verify with the lesson).");
        return new ExplainScore(score, fb.toString(), true);
    }

    @Transactional
    public ExplainBackResult scoreLessonExplainBack(Long userId, String slug, ExplainBackRequest req) {
        Lesson l = lessons.findBySlug(slug).orElseThrow(() -> ApiException.notFound("Lesson"));
        ExplainScore s = new ExplainScore(null, null, false);
        if (req.useAi()) {
            List<String> rubric = new ArrayList<>();
            l.getContent().path("explainBack").path("rubric").forEach(r -> rubric.add(r.asText()));
            s = scoreExplanation(userId, l.getTitle(), l.getContent().path("explainBack").path("modelAnswer").asText(), rubric, req.text());
        }
        ExplainBackResult res = lessonService.saveExplainBack(userId, slug, req.text(), req.checkedRubric(), s.score(), s.feedback());
        if (req.useAi() && !s.aiUsed()) {
            return new ExplainBackResult(res.score(), (ai.usable() ? AiService.RESTING : AiService.OFF) + "\n" + res.feedback(),
                    res.modelAnswer(), false, res.xp(), res.topicStatus());
        }
        return res;
    }

    // ------------------------------------------------------------------ daily briefing / debrief / weekly plan

    @Transactional(readOnly = true)
    public BriefingResponse briefing(Long userId, String kind) {
        String facts = factsLine(userId);
        String goal = switch (kind) {
            case "DEBRIEF" -> "Write a short end-of-day debrief: celebrate one thing, name one thing to fix tomorrow, suggest tomorrow's first 20-minute task.";
            case "WEEKLY_PLAN" -> "Suggest a realistic plan adjustment for next week (max 5 bullets) based on weak areas. Prefer process goals (sessions, recall days) over volume.";
            default -> "Write a short, warm morning briefing: today's 3 priorities in order, and one tip. Max 120 words.";
        };
        AiResult r = ai.ask(userId, "briefing-" + kind.toLowerCase(), SYSTEM + "\n" + goal + " No check question needed.",
                List.of(new Message("user", "My data: " + facts)), null);
        if (r.ok()) return new BriefingResponse(kind, r.text(), true);
        return new BriefingResponse(kind, templateBriefing(kind, userId), false);
    }

    private String templateBriefing(String kind, Long userId) {
        var pm = gym.patternMap(userId);
        int due = flashcards.dueCount(userId) + gym.dueRevisitCount(userId);
        String next = pm.recommendedNext() == null ? "mixed practice" : pm.recommendedNext().name();
        return switch (kind) {
            case "DEBRIEF" -> "Today you showed up, and that's the habit that gets offers. Before bed: pick tomorrow's 3 tasks on the Focus page. "
                    + "Tomorrow, start with " + (due > 0 ? due + " recall items" : "one 25-minute focus block") + ".";
            case "WEEKLY_PLAN" -> "Next week:\n- Recall on at least 4 days (10 min each).\n- Finish 3 lessons on your current roadmap week.\n- Solve 5 problems, mostly " + next
                    + ".\n- Record every mistake honestly.\n- One speaking session every other day.";
            default -> "Good morning! Today:\n1. Warm-up recall (" + due + " items due).\n2. One lesson or 25-minute focus block.\n3. One problem in " + next
                    + ".\nTip: type the code yourself. Copying feels fast but doesn't stick.";
        };
    }

    private String factsLine(Long userId) {
        var streak = streaks.view(userId);
        return "streak " + streak.current() + " days; lessons done " + stats.lessonsCompleted(userId) + "; problems " + stats.problems(userId)
                + "; due recall " + (flashcards.dueCount(userId) + gym.dueRevisitCount(userId)) + "; weak: " + weakSummary(userId);
    }

    /** One short line about weak spots (keeps prompts small). */
    String weakSummary(Long userId) {
        List<String> parts = new ArrayList<>();
        gym.patternMap(userId).patterns().stream().filter(p -> "WEAK".equals(p.strength())).limit(3).forEach(p -> parts.add(p.name()));
        mistakes.weeklyTop(userId).stream().limit(2).forEach(m -> parts.add("often: " + m.categoryName()));
        return parts.isEmpty() ? "none recorded yet" : String.join(", ", parts);
    }

    // ------------------------------------------------------------------ helpers

    static MessageView view(AiMessage m) {
        return new MessageView(m.getId(), m.getRole(), m.getContent(), m.isFallback(), m.getCreatedAt());
    }

    static String trim(String s, int max) { return s == null ? "" : s.length() > max ? s.substring(0, max) : s; }

    static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }
}
