package com.codequest.controller;

import com.codequest.dto.InterviewDtos.*;
import com.codequest.dto.PageResponse;
import com.codequest.security.UserPrincipal;
import com.codequest.service.*;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Interview Colosseum, aptitude, mock tests, diary, Spring track, reports")
@Validated
@RestController
@RequestMapping("/api")
public class ColosseumController {
    private final PracticeService practice;
    private final ExamService exams;
    private final DiaryService diary;
    private final SpringTrackService spring;
    private final ReportService reports;
    private final CodeRunnerService runner;

    public ColosseumController(PracticeService practice, ExamService exams, DiaryService diary, SpringTrackService spring,
                               ReportService reports, CodeRunnerService runner) {
        this.practice = practice;
        this.exams = exams;
        this.diary = diary;
        this.spring = spring;
        this.reports = reports;
        this.runner = runner;
    }

    // ---- interview question bank
    @GetMapping("/question-bank")
    public PageResponse<BankQuestion> bank(@AuthenticationPrincipal UserPrincipal me, @RequestParam(required = false) String category,
                                           @RequestParam(required = false) String difficulty, @RequestParam(required = false) String company,
                                           @RequestParam(required = false) String q,
                                           @RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return practice.browse(me.id(), category, difficulty, company, q, page, size);
    }

    @PostMapping("/question-bank/{id}/practice")
    public PracticeResult practise(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id, @Valid @RequestBody PracticeRequest req) {
        return practice.practise(me.id(), id, req);
    }

    // ---- aptitude
    @GetMapping("/aptitude/set")
    public List<AptitudeItem> aptitudeSet(@AuthenticationPrincipal UserPrincipal me, @RequestParam(required = false) String section,
                                          @RequestParam(required = false) String topic, @RequestParam(defaultValue = "10") @Min(1) @Max(30) int count) {
        return practice.aptitudeSet(me.id(), section, topic, count);
    }

    @PostMapping("/aptitude/submit")
    public AptitudeGraded aptitudeSubmit(@AuthenticationPrincipal UserPrincipal me, @Valid @RequestBody AptitudeSubmit req) {
        return practice.gradeAptitude(me.id(), req, "PRACTICE");
    }

    @GetMapping("/aptitude/stats")
    public List<TopicAccuracy> aptitudeStats(@AuthenticationPrincipal UserPrincipal me) {
        return practice.aptitudeStats(me.id());
    }

    // ---- exam pattern editor
    @GetMapping("/exam-patterns")
    public List<ExamPatternView> patterns(@AuthenticationPrincipal UserPrincipal me) {
        return exams.listPatterns(me.id());
    }

    @PostMapping("/exam-patterns")
    @ResponseStatus(HttpStatus.CREATED)
    public ExamPatternView createPattern(@AuthenticationPrincipal UserPrincipal me, @Valid @RequestBody ExamPatternRequest req) {
        return exams.savePattern(me.id(), null, req);
    }

    @PutMapping("/exam-patterns/{id}")
    public ExamPatternView updatePattern(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id, @Valid @RequestBody ExamPatternRequest req) {
        return exams.savePattern(me.id(), id, req);
    }

    @DeleteMapping("/exam-patterns/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePattern(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        exams.deletePattern(me.id(), id);
    }

    // ---- timed mock tests
    @PostMapping("/mock-tests")
    @ResponseStatus(HttpStatus.CREATED)
    public MockTestView startMock(@AuthenticationPrincipal UserPrincipal me, @RequestParam Long patternId) {
        return exams.start(me.id(), patternId);
    }

    @GetMapping("/mock-tests")
    public List<MockSummary> mocks(@AuthenticationPrincipal UserPrincipal me) {
        return exams.history(me.id());
    }

    @GetMapping("/mock-tests/{id}")
    public MockTestView mock(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        return exams.get(me.id(), id);
    }

    @PostMapping("/mock-tests/{id}/submit")
    public MockTestView submitMock(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id, @Valid @RequestBody MockSubmit req) {
        return exams.submit(me.id(), id, req);
    }

    // ---- interview diary
    @GetMapping("/diary")
    public List<DiaryView> diary(@AuthenticationPrincipal UserPrincipal me) {
        return diary.list(me.id());
    }

    @PostMapping("/diary")
    @ResponseStatus(HttpStatus.CREATED)
    public DiaryView addDiary(@AuthenticationPrincipal UserPrincipal me, @Valid @RequestBody DiaryRequest req) {
        return diary.save(me.id(), null, req);
    }

    @PutMapping("/diary/{id}")
    public DiaryView updateDiary(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id, @Valid @RequestBody DiaryRequest req) {
        return diary.save(me.id(), id, req);
    }

    @DeleteMapping("/diary/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDiary(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        diary.delete(me.id(), id);
    }

    // ---- Spring track
    @GetMapping("/spring/topics")
    public List<SpringTrackService.TopicCard> springMap(@AuthenticationPrincipal UserPrincipal me) {
        return spring.map(me.id());
    }

    @GetMapping("/spring/topics/{code}")
    public SpringTrackService.TopicDetail springTopic(@AuthenticationPrincipal UserPrincipal me, @PathVariable String code) {
        return spring.topic(me.id(), code);
    }

    @PutMapping("/spring/topics/{code}/status")
    public SpringTrackService.TopicCard springStatus(@AuthenticationPrincipal UserPrincipal me, @PathVariable String code,
                                                     @RequestParam @Pattern(regexp = "NOT_STARTED|LEARNING") String status) {
        return spring.setStatus(me.id(), code, status);
    }

    @PostMapping("/spring/topics/{code}/checklist/{index}")
    public SpringTrackService.TopicCard springChecklist(@AuthenticationPrincipal UserPrincipal me, @PathVariable String code, @PathVariable int index) {
        return spring.toggleChecklist(me.id(), code, index);
    }

    @PostMapping("/spring/topics/{code}/quiz")
    public SpringTrackService.QuizOutcome springQuiz(@AuthenticationPrincipal UserPrincipal me, @PathVariable String code,
                                                     @Valid @RequestBody SpringTrackService.QuizAnswers req) {
        return spring.submitQuiz(me.id(), code, req);
    }

    @PostMapping("/spring/topics/{code}/explain")
    public SpringTrackService.ExplainOutcome springExplain(@AuthenticationPrincipal UserPrincipal me, @PathVariable String code,
                                                           @Valid @RequestBody SpringTrackService.ExplainRequest req) {
        return spring.explain(me.id(), code, req);
    }

    @GetMapping("/spring/annotations")
    public JsonNode annotations() {
        return spring.annotations();
    }

    @GetMapping("/spring/debug-challenges")
    public JsonNode debugChallenges() {
        return spring.debugChallenges();
    }

    @GetMapping("/spring/projects")
    public List<SpringTrackService.ProjectView> projects(@AuthenticationPrincipal UserPrincipal me) {
        return spring.projects(me.id());
    }

    @PutMapping("/spring/projects/{id}")
    public SpringTrackService.ProjectView updateProject(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id,
                                                        @Valid @RequestBody SpringTrackService.ProjectUpdate req) {
        return spring.updateProject(me.id(), id, req);
    }

    @PostMapping("/spring/milestones/{id}/toggle")
    public SpringTrackService.ProjectView toggleMilestone(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        return spring.toggleMilestone(me.id(), id);
    }

    @PostMapping("/spring/projects/{id}/logs")
    public SpringTrackService.ProjectView addLog(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id,
                                                 @Valid @RequestBody SpringTrackService.LogRequest req) {
        return spring.addLog(me.id(), id, req);
    }

    @PatchMapping("/spring/logs/{id}/resolved")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resolveLog(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id, @RequestParam boolean value) {
        spring.resolveLog(me.id(), id, value);
    }

    // ---- reports
    @PostMapping("/reports")
    public Map<String, Object> report(@AuthenticationPrincipal UserPrincipal me, @RequestParam @Pattern(regexp = "WEEKLY|MONTHLY") String kind,
                                      @RequestParam(defaultValue = "false") boolean useAi) {
        return reports.generate(me.id(), kind, useAi);
    }

    @GetMapping("/reports")
    public List<Map<String, Object>> reports(@AuthenticationPrincipal UserPrincipal me) {
        return reports.list(me.id());
    }

    // ---- optional code runner
    public record RunRequest(@NotBlank @Size(max = 20000) String code, @Size(max = 10000) String stdin) {}

    @GetMapping("/code/status")
    public Map<String, Boolean> runnerStatus() {
        return Map.of("enabled", runner.enabled());
    }

    @PostMapping("/code/run")
    public Map<String, Object> run(@Valid @RequestBody RunRequest req) {
        return runner.run(req.code(), req.stdin());
    }
}
