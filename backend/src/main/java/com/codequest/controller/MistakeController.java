package com.codequest.controller;

import com.codequest.dto.GymDtos.*;
import com.codequest.dto.PageResponse;
import com.codequest.security.UserPrincipal;
import com.codequest.service.MistakeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Mistake Notebook")
@Validated
@RestController
@RequestMapping("/api")
public class MistakeController {
    private final MistakeService mistakes;

    public MistakeController(MistakeService mistakes) {
        this.mistakes = mistakes;
    }

    @GetMapping("/mistake-categories")
    public List<MistakeCategoryView> categories() {
        return mistakes.categories();
    }

    @PostMapping("/mistakes")
    @ResponseStatus(HttpStatus.CREATED)
    public MistakeService.Saved create(@AuthenticationPrincipal UserPrincipal me, @Valid @RequestBody MistakeRequest req) {
        return mistakes.create(me.id(), req);
    }

    @GetMapping("/mistakes")
    public PageResponse<MistakeView> list(@AuthenticationPrincipal UserPrincipal me,
                                          @RequestParam(required = false) Boolean resolved,
                                          @RequestParam(defaultValue = "0") @Min(0) int page,
                                          @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return mistakes.list(me.id(), resolved, page, size);
    }

    @PatchMapping("/mistakes/{id}/resolved")
    public MistakeView setResolved(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id, @RequestParam boolean value) {
        return mistakes.setResolved(me.id(), id, value);
    }

    @DeleteMapping("/mistakes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        mistakes.delete(me.id(), id);
    }

    @GetMapping("/mistakes/weekly-top")
    public List<MistakeTrend> weeklyTop(@AuthenticationPrincipal UserPrincipal me) {
        return mistakes.weeklyTop(me.id());
    }
}
