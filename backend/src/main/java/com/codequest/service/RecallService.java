package com.codequest.service;

import com.codequest.domain.QuizGrader;
import com.codequest.dto.LearningDtos.*;
import com.codequest.entity.Lesson;
import com.codequest.entity.LessonProgress;
import com.codequest.entity.QuizAttempt;
import com.codequest.entity.QuizQuestion;
import com.codequest.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Daily 10-minute Warm-up Recall: due flashcards + 3 quiz questions from lessons you already finished.
 * Active recall (pulling from memory) before learning anything new.
 */
@Service
public class RecallService {
    private final FlashcardService flashcards;
    private final LessonProgressRepository progress;
    private final LessonRepository lessons;
    private final QuizQuestionRepository questions;
    private final QuizAttemptRepository attempts;
    private final XpEventRepository xpEvents;
    private final TimeService time;
    private final ObjectMapper json;

    public RecallService(FlashcardService flashcards, LessonProgressRepository progress, LessonRepository lessons,
                         QuizQuestionRepository questions, QuizAttemptRepository attempts, XpEventRepository xpEvents,
                         TimeService time, ObjectMapper json) {
        this.flashcards = flashcards;
        this.progress = progress;
        this.lessons = lessons;
        this.questions = questions;
        this.attempts = attempts;
        this.xpEvents = xpEvents;
        this.time = time;
        this.json = json;
    }

    @Transactional(readOnly = true)
    public WarmupView warmup(Long userId) {
        List<FlashcardView> cards = flashcards.due(userId, 15);
        List<Long> doneLessons = progress.findByUserId(userId).stream().filter(LessonProgress::isCompleted).map(LessonProgress::getLessonId).toList();
        List<QuizQuestion> pool = doneLessons.isEmpty() ? List.of() : questions.findByLessonIdIn(doneLessons);
        // Same 3 questions all day (seeded by date), different tomorrow.
        List<QuizQuestion> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, new Random(userId * 1_000_003L + time.today(userId).toEpochDay()));
        List<QuizQuestion> picked = shuffled.stream().limit(3).toList();
        Map<Long, String> titles = lessons.findAllById(picked.stream().map(QuizQuestion::getLessonId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Lesson::getId, Lesson::getTitle));
        Map<String, String> qLesson = new LinkedHashMap<>();
        picked.forEach(q -> qLesson.put(String.valueOf(q.getId()), titles.getOrDefault(q.getLessonId(), "")));
        List<QuestionView> views = picked.stream()
                .map(q -> new QuestionView(String.valueOf(q.getId()), q.getType(), q.getPrompt(), q.getCode(), q.getOptions())).toList();
        boolean rewarded = xpEvents.existsByUserIdAndActionAndSourceKey(userId, com.codequest.domain.XpAction.RECALL_SESSION, "recall:" + time.today(userId));
        return new WarmupView(cards, views, qLesson, flashcards.dueCount(userId), flashcards.reviewedToday(userId), rewarded);
    }

    /** Grade the warm-up questions (ids here are the database question ids). */
    @Transactional
    public List<QuestionResult> answerWarmup(Long userId, WarmupAnswerRequest req) {
        Map<Long, JsonNode> given = new HashMap<>();
        req.answers().forEach((k, v) -> { try { given.put(Long.valueOf(k), v); } catch (NumberFormatException ignored) { } });
        List<QuizQuestion> qs = questions.findByIdIn(given.keySet());
        List<QuestionResult> out = new ArrayList<>();
        ArrayNode details = json.createArrayNode();
        int correct = 0;
        for (QuizQuestion q : qs) {
            boolean ok = QuizGrader.isCorrect(q.getType(), q.getAnswer(), given.get(q.getId()));
            if (ok) correct++;
            out.add(new QuestionResult(String.valueOf(q.getId()), ok, given.get(q.getId()), LessonService.displayAnswer(q), q.getExplanation()));
            details.addObject().put("questionId", q.getId()).put("correct", ok);
        }
        if (!qs.isEmpty()) {
            QuizAttempt a = new QuizAttempt();
            a.setUserId(userId);
            a.setKind("WARMUP");
            a.setTotal(qs.size());
            a.setCorrect(correct);
            a.setScorePercent(QuizGrader.percent(correct, qs.size()));
            a.setPassed(correct == qs.size());
            a.setDetails(details);
            a.setLocalDate(time.today(userId));
            attempts.save(a);
        }
        return out;
    }
}
