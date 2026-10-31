package com.codequest.controller;

import com.codequest.repository.ProblemSolvedRepository;
import com.codequest.repository.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Only ADMIN users reach this (rule in SecurityConfig: /api/admin/** hasRole ADMIN).
 * Phase 3 adds AI diagnostics here; Phase 2 adds the lesson editor.
 */
@Tag(name = "Admin")
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final UserRepository users;
    private final ProblemSolvedRepository problems;

    public AdminController(UserRepository users, ProblemSolvedRepository problems) {
        this.users = users;
        this.problems = problems;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return Map.of("users", users.count(), "problemsLogged", problems.count());
    }
}
