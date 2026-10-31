package com.codequest.controller;

import com.codequest.dto.AiDtos.*;
import com.codequest.dto.LearningDtos.FlashcardView;
import com.codequest.dto.PageResponse;
import com.codequest.security.UserPrincipal;
import com.codequest.service.MentorService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI Mentor")
@Validated
@RestController
@RequestMapping("/api/mentor")
public class MentorController {
    private final MentorService mentor;

    public MentorController(MentorService mentor) {
        this.mentor = mentor;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@AuthenticationPrincipal UserPrincipal me, @Valid @RequestBody ChatRequest req) {
        return mentor.chat(me.id(), req);
    }

    @PostMapping("/hint")
    public HintResponse hint(@AuthenticationPrincipal UserPrincipal me, @Valid @RequestBody HintRequest req) {
        return mentor.hint(me.id(), req);
    }

    @PostMapping("/code-review")
    public ChatResponse codeReview(@AuthenticationPrincipal UserPrincipal me, @Valid @RequestBody CodeReviewRequest req) {
        return mentor.codeReview(me.id(), req);
    }

    @PostMapping("/quiz")
    public QuizGenResponse quiz(@AuthenticationPrincipal UserPrincipal me, @Valid @RequestBody QuizGenRequest req) {
        return mentor.generateQuiz(me.id(), req);
    }

    @GetMapping("/briefing")
    public BriefingResponse briefing(@AuthenticationPrincipal UserPrincipal me,
                                     @RequestParam(defaultValue = "MORNING") @Pattern(regexp = "MORNING|DEBRIEF|WEEKLY_PLAN") String kind) {
        return mentor.briefing(me.id(), kind);
    }

    @GetMapping("/conversations")
    public PageResponse<ConversationView> history(@AuthenticationPrincipal UserPrincipal me, @RequestParam(required = false) String q,
                                                  @RequestParam(defaultValue = "0") @Min(0) int page,
                                                  @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return mentor.history(me.id(), q, page, size);
    }

    @GetMapping("/conversations/{id}")
    public ConversationView conversation(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        return mentor.conversation(me.id(), id);
    }

    @PatchMapping("/conversations/{id}/resolved")
    public ConversationView resolved(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id, @RequestParam boolean value) {
        return mentor.setResolved(me.id(), id, value);
    }

    @DeleteMapping("/conversations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        mentor.deleteConversation(me.id(), id);
    }

    @PostMapping("/messages/{id}/flashcard")
    @ResponseStatus(HttpStatus.CREATED)
    public FlashcardView toFlashcard(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        return mentor.messageToFlashcard(me.id(), id);
    }
}
