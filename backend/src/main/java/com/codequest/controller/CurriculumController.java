package com.codequest.controller;

import com.codequest.dto.CurriculumDtos.*;
import com.codequest.security.UserPrincipal;
import com.codequest.service.CurriculumService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Curriculum")
@RestController
@RequestMapping("/api")
public class CurriculumController {
    private final CurriculumService curriculum;

    public CurriculumController(CurriculumService curriculum) {
        this.curriculum = curriculum;
    }

    /** ?company=INFOSYS_SP filters to topics tagged for that company (or ALL). */
    @GetMapping("/curriculum/java")
    public CurriculumView java(@AuthenticationPrincipal UserPrincipal me, @RequestParam(required = false) String company) {
        return curriculum.javaCurriculum(me.id(), company);
    }

    @PutMapping("/curriculum/java/{topicId}/status")
    public TopicView updateStatus(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long topicId,
                                  @Valid @RequestBody StatusUpdateRequest req) {
        return curriculum.updateStatus(me.id(), topicId, req);
    }

    @GetMapping("/worlds")
    public List<WorldView> worlds() {
        return curriculum.worlds();
    }

    @GetMapping("/companies")
    public List<CompanyView> companies() {
        return curriculum.companies();
    }
}
