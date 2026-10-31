package com.codequest.controller;

import com.codequest.dto.LearningDtos.*;
import com.codequest.security.UserPrincipal;
import com.codequest.service.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Worlds, lessons, quizzes")
@Validated
@RestController
@RequestMapping("/api")
public class LearningController {
    private final LessonService lessons;
    private final MentorService mentor;

    public LearningController(LessonService lessons, MentorService mentor) {
        this.lessons = lessons;
        this.mentor = mentor;
    }

    @GetMapping("/worlds/progress")
    public List<WorldSummary> worlds(@AuthenticationPrincipal UserPrincipal me) {
        return lessons.worlds(me.id());
    }

    @GetMapping("/worlds/{code}")
    public WorldDetail world(@AuthenticationPrincipal UserPrincipal me, @PathVariable String code, @RequestParam(required = false) String company) {
        return lessons.world(me.id(), code, company);
    }

    @GetMapping("/lessons/{slug}")
    public LessonView lesson(@AuthenticationPrincipal UserPrincipal me, @PathVariable String slug) {
        return lessons.lesson(me.id(), slug);
    }

    @PostMapping("/lessons/{slug}/steps")
    public LessonView step(@AuthenticationPrincipal UserPrincipal me, @PathVariable String slug, @Valid @RequestBody StepRequest req) {
        return lessons.markStep(me.id(), slug, req);
    }

    @PostMapping("/lessons/{slug}/quiz")
    public QuizResult quiz(@AuthenticationPrincipal UserPrincipal me, @PathVariable String slug, @Valid @RequestBody QuizSubmission req) {
        return lessons.submitQuiz(me.id(), slug, req, false);
    }

    @GetMapping("/lessons/{slug}/prove-it")
    public List<QuestionView> proveIt(@AuthenticationPrincipal UserPrincipal me, @PathVariable String slug) {
        return lessons.proveItQuestions(me.id(), slug);
    }

    @PostMapping("/lessons/{slug}/prove-it")
    public QuizResult submitProveIt(@AuthenticationPrincipal UserPrincipal me, @PathVariable String slug, @Valid @RequestBody QuizSubmission req) {
        return lessons.submitQuiz(me.id(), slug, req, true);
    }

    /** Explain it back. useAi=true asks the AI mentor to score it (falls back to the self-check rubric). */
    @PostMapping("/lessons/{slug}/explain-back")
    public ExplainBackResult explainBack(@AuthenticationPrincipal UserPrincipal me, @PathVariable String slug, @Valid @RequestBody ExplainBackRequest req) {
        return mentor.scoreLessonExplainBack(me.id(), slug, req);
    }
}
