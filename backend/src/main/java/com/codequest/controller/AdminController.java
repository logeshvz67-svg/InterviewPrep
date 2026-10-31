package com.codequest.controller;

import com.codequest.dto.AiDtos.Diagnostics;
import com.codequest.dto.AiDtos.TestResult;
import com.codequest.repository.ProblemSolvedRepository;
import com.codequest.repository.UserRepository;
import com.codequest.security.UserPrincipal;
import com.codequest.service.AdminContentService;
import com.codequest.service.AiDiagnosticsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Only ADMIN users reach this (rule in SecurityConfig: /api/admin/** hasRole ADMIN).
 * AI diagnostics + lesson editor.
 */
@Tag(name = "Admin")
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final UserRepository users;
    private final ProblemSolvedRepository problems;
    private final AiDiagnosticsService ai;
    private final AdminContentService content;

    public AdminController(UserRepository users, ProblemSolvedRepository problems, AiDiagnosticsService ai, AdminContentService content) {
        this.users = users;
        this.problems = problems;
        this.ai = ai;
        this.content = content;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return Map.of("users", users.count(), "problemsLogged", problems.count());
    }

    @GetMapping("/ai")
    public Diagnostics aiDiagnostics(@AuthenticationPrincipal UserPrincipal me) {
        return ai.diagnostics(me.id());
    }

    @PostMapping("/ai/test")
    public TestResult testAi() {
        return ai.test();
    }

    @GetMapping("/lessons")
    public List<AdminContentService.LessonRow> lessons() {
        return content.list();
    }

    @GetMapping("/lessons/{slug}")
    public Map<String, String> lesson(@PathVariable String slug) {
        return content.getYaml(slug);
    }

    public record YamlBody(@NotBlank @Size(max = 300_000) String yaml) {}

    @PutMapping("/lessons/{slug}")
    public Map<String, String> save(@PathVariable String slug, @Valid @RequestBody YamlBody body) {
        return content.saveYaml(slug, body.yaml());
    }
}
