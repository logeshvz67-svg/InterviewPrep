package com.codequest.controller;

import com.codequest.dto.FocusDtos.*;
import com.codequest.dto.PageResponse;
import com.codequest.security.UserPrincipal;
import com.codequest.service.FocusService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Focus tools")
@Validated
@RestController
@RequestMapping("/api")
public class FocusController {
    private final FocusService focus;

    public FocusController(FocusService focus) {
        this.focus = focus;
    }

    @PostMapping("/focus/start")
    @ResponseStatus(HttpStatus.CREATED)
    public FocusView start(@AuthenticationPrincipal UserPrincipal me, @Valid @RequestBody StartFocusRequest req) {
        return focus.start(me.id(), req);
    }

    /** 204 No Content when nothing is running. */
    @GetMapping("/focus/active")
    public ResponseEntity<FocusView> active(@AuthenticationPrincipal UserPrincipal me) {
        FocusView v = focus.active(me.id());
        return v == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(v);
    }

    @PostMapping("/focus/{id}/complete")
    public FocusCompleted complete(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        return focus.complete(me.id(), id);
    }

    @PostMapping("/focus/{id}/cancel")
    public FocusView cancel(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        return focus.cancel(me.id(), id);
    }

    @GetMapping("/focus/stats")
    public FocusStats stats(@AuthenticationPrincipal UserPrincipal me, @RequestParam(defaultValue = "7") @Min(1) @Max(90) int days) {
        return focus.stats(me.id(), days);
    }

    @PostMapping("/distractions")
    @ResponseStatus(HttpStatus.CREATED)
    public DistractionView logDistraction(@AuthenticationPrincipal UserPrincipal me, @Valid @RequestBody DistractionRequest req) {
        return focus.logDistraction(me.id(), req);
    }

    @GetMapping("/distractions")
    public PageResponse<DistractionView> distractions(@AuthenticationPrincipal UserPrincipal me,
                                                      @RequestParam(defaultValue = "0") @Min(0) int page,
                                                      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return focus.distractions(me.id(), page, size);
    }

    @GetMapping("/distractions/insights")
    public DistractionInsights insights(@AuthenticationPrincipal UserPrincipal me, @RequestParam(defaultValue = "7") @Min(1) @Max(90) int days) {
        return focus.insights(me.id(), days);
    }

    @PutMapping("/commitments/tomorrow")
    public CommitmentSaved commit(@AuthenticationPrincipal UserPrincipal me, @Valid @RequestBody CommitmentRequest req) {
        return focus.commitTomorrow(me.id(), req);
    }

    @GetMapping("/commitments/today")
    public CommitmentView today(@AuthenticationPrincipal UserPrincipal me) {
        return focus.commitmentFor(me.id(), false);
    }

    @GetMapping("/commitments/tomorrow")
    public CommitmentView tomorrow(@AuthenticationPrincipal UserPrincipal me) {
        return focus.commitmentFor(me.id(), true);
    }

    @PatchMapping("/commitments/today/tasks/{index}/toggle")
    public CommitmentView toggle(@AuthenticationPrincipal UserPrincipal me, @PathVariable int index) {
        return focus.toggleToday(me.id(), index);
    }
}
