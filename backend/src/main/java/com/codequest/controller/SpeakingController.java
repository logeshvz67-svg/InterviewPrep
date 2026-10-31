package com.codequest.controller;

import com.codequest.dto.PageResponse;
import com.codequest.dto.SpeakDtos.*;
import com.codequest.security.UserPrincipal;
import com.codequest.service.MockInterviewService;
import com.codequest.service.SpeakingService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Communication Gym and mock interviews")
@Validated
@RestController
@RequestMapping("/api")
public class SpeakingController {
    private final SpeakingService speaking;
    private final MockInterviewService interviews;

    public SpeakingController(SpeakingService speaking, MockInterviewService interviews) {
        this.speaking = speaking;
        this.interviews = interviews;
    }

    @GetMapping("/speaking/prompts")
    public List<PromptView> prompts(@RequestParam(required = false) String category) {
        return speaking.prompts(category);
    }

    @GetMapping("/speaking/library")
    public Map<String, JsonNode> library() {
        return speaking.library();
    }

    @PostMapping("/speaking/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public SessionSaved submit(@AuthenticationPrincipal UserPrincipal me, @Valid @RequestBody SessionRequest req) {
        return speaking.submit(me.id(), req);
    }

    @GetMapping("/speaking/sessions")
    public PageResponse<SessionView> history(@AuthenticationPrincipal UserPrincipal me,
                                             @RequestParam(defaultValue = "0") @Min(0) int page,
                                             @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return speaking.history(me.id(), page, size);
    }

    @GetMapping("/speaking/trends")
    public Trends trends(@AuthenticationPrincipal UserPrincipal me) {
        return speaking.trends(me.id());
    }

    @PostMapping("/interviews")
    @ResponseStatus(HttpStatus.CREATED)
    public InterviewTurn start(@AuthenticationPrincipal UserPrincipal me, @Valid @RequestBody StartInterviewRequest req) {
        return interviews.start(me.id(), req);
    }

    @PostMapping("/interviews/{id}/answer")
    public InterviewTurn answer(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id, @Valid @RequestBody InterviewAnswerRequest req) {
        return interviews.answer(me.id(), id, req);
    }

    @PostMapping("/interviews/{id}/finish")
    public InterviewView finish(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        return interviews.finish(me.id(), id);
    }

    @GetMapping("/interviews")
    public List<InterviewView> list(@AuthenticationPrincipal UserPrincipal me) {
        return interviews.history(me.id());
    }

    @GetMapping("/interviews/{id}")
    public InterviewView get(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        return interviews.get(me.id(), id);
    }
}
