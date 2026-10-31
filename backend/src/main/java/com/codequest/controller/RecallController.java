package com.codequest.controller;

import com.codequest.dto.LearningDtos.*;
import com.codequest.dto.PageResponse;
import com.codequest.security.UserPrincipal;
import com.codequest.service.FlashcardService;
import com.codequest.service.RecallService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Recall engine")
@Validated
@RestController
@RequestMapping("/api")
public class RecallController {
    private final FlashcardService cards;
    private final RecallService recall;

    public RecallController(FlashcardService cards, RecallService recall) {
        this.cards = cards;
        this.recall = recall;
    }

    @GetMapping("/recall/warmup")
    public WarmupView warmup(@AuthenticationPrincipal UserPrincipal me) {
        return recall.warmup(me.id());
    }

    @PostMapping("/recall/warmup/answers")
    public List<QuestionResult> warmupAnswers(@AuthenticationPrincipal UserPrincipal me, @Valid @RequestBody WarmupAnswerRequest req) {
        return recall.answerWarmup(me.id(), req);
    }

    @PostMapping("/recall/complete")
    public RecallComplete complete(@AuthenticationPrincipal UserPrincipal me) {
        return cards.completeRecall(me.id());
    }

    @GetMapping("/flashcards/due")
    public List<FlashcardView> due(@AuthenticationPrincipal UserPrincipal me, @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return cards.due(me.id(), limit);
    }

    @PostMapping("/flashcards/{id}/review")
    public ReviewResult review(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id, @Valid @RequestBody ReviewRequest req) {
        return cards.review(me.id(), id, req);
    }

    @GetMapping("/flashcards")
    public PageResponse<FlashcardView> list(@AuthenticationPrincipal UserPrincipal me, @RequestParam(required = false) String q,
                                            @RequestParam(defaultValue = "0") @Min(0) int page,
                                            @RequestParam(defaultValue = "30") @Min(1) @Max(100) int size) {
        return cards.list(me.id(), q, page, size);
    }

    @PostMapping("/flashcards")
    @ResponseStatus(HttpStatus.CREATED)
    public FlashcardView create(@AuthenticationPrincipal UserPrincipal me, @Valid @RequestBody ManualCardRequest req) {
        return cards.createManual(me.id(), req, "MANUAL", null);
    }

    @PostMapping("/mistakes/{id}/flashcard")
    @ResponseStatus(HttpStatus.CREATED)
    public FlashcardView fromMistake(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        return cards.createFromMistake(me.id(), id);
    }

    @PatchMapping("/flashcards/{id}/suspended")
    public FlashcardView suspend(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id, @RequestParam boolean value) {
        return cards.setSuspended(me.id(), id, value);
    }

    @DeleteMapping("/flashcards/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        cards.delete(me.id(), id);
    }
}
