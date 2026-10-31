package com.codequest.controller;

import com.codequest.dto.GameDtos.XpAward;
import com.codequest.dto.GymDtos.*;
import com.codequest.dto.PageResponse;
import com.codequest.security.UserPrincipal;
import com.codequest.service.GymService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Problem-Solving Gym")
@Validated
@RestController
@RequestMapping("/api")
public class GymController {
    private final GymService gym;

    public GymController(GymService gym) {
        this.gym = gym;
    }

    @GetMapping("/patterns")
    public List<PatternView> patterns() {
        return gym.allPatterns();
    }

    @PostMapping("/problems")
    @ResponseStatus(HttpStatus.CREATED)
    public ProblemSaved log(@AuthenticationPrincipal UserPrincipal me, @Valid @RequestBody ProblemRequest req) {
        return gym.log(me.id(), req);
    }

    @GetMapping("/problems")
    public PageResponse<ProblemView> list(@AuthenticationPrincipal UserPrincipal me,
                                          @RequestParam(required = false) String pattern,
                                          @RequestParam(defaultValue = "0") @Min(0) int page,
                                          @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return gym.list(me.id(), pattern, page, size);
    }

    @GetMapping("/problems/{id}")
    public ProblemView get(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        return gym.get(me.id(), id);
    }

    @PutMapping("/problems/{id}")
    public ProblemView update(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id, @Valid @RequestBody ProblemRequest req) {
        return gym.update(me.id(), id, req);
    }

    @DeleteMapping("/problems/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        gym.delete(me.id(), id);
    }

    @GetMapping("/gym/pattern-map")
    public PatternMap patternMap(@AuthenticationPrincipal UserPrincipal me) {
        return gym.patternMap(me.id());
    }

    @GetMapping("/revisits/due")
    public List<RevisitView> due(@AuthenticationPrincipal UserPrincipal me) {
        return gym.dueRevisits(me.id());
    }

    @GetMapping("/revisits/upcoming")
    public List<RevisitView> upcoming(@AuthenticationPrincipal UserPrincipal me) {
        return gym.upcomingRevisits(me.id());
    }

    @PostMapping("/revisits/{id}/complete")
    public XpAward completeRevisit(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id,
                                   @Valid @RequestBody RevisitCompleteRequest req) {
        return gym.completeRevisit(me.id(), id, req);
    }
}
