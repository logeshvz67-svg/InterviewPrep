package com.codequest.controller;

import com.codequest.dto.PageResponse;
import com.codequest.security.UserPrincipal;
import com.codequest.service.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "Quests, badges, records, problem bank, data")
@Validated
@RestController
@RequestMapping("/api")
public class ProgressController {
    private final QuestBoardService board;
    private final AchievementService badges;
    private final RecordsService records;
    private final ProblemBankService bank;
    private final AccountDataService data;

    public ProgressController(QuestBoardService board, AchievementService badges, RecordsService records,
                              ProblemBankService bank, AccountDataService data) {
        this.board = board;
        this.badges = badges;
        this.records = records;
        this.bank = bank;
        this.data = data;
    }

    @GetMapping("/quests/board")
    public QuestBoardService.Board questBoard(@AuthenticationPrincipal UserPrincipal me) {
        return board.board(me.id());
    }

    @GetMapping("/achievements")
    public List<AchievementService.BadgeView> achievements(@AuthenticationPrincipal UserPrincipal me) {
        return badges.list(me.id());
    }

    @GetMapping("/records")
    public Map<String, Object> records(@AuthenticationPrincipal UserPrincipal me) {
        return records.records(me.id());
    }

    @GetMapping("/problem-bank")
    public PageResponse<ProblemBankService.BankProblem> browse(@AuthenticationPrincipal UserPrincipal me,
                                                               @RequestParam(required = false) String pattern,
                                                               @RequestParam(required = false) String difficulty,
                                                               @RequestParam(required = false) String company,
                                                               @RequestParam(required = false) String status,
                                                               @RequestParam(defaultValue = "0") @Min(0) int page,
                                                               @RequestParam(defaultValue = "30") @Min(1) @Max(100) int size) {
        return bank.browse(me.id(), pattern, difficulty, company, status, page, size);
    }

    @GetMapping("/problem-bank/suggestions")
    public List<ProblemBankService.Suggestion> suggestions(@AuthenticationPrincipal UserPrincipal me) {
        return bank.suggestions(me.id());
    }

    @GetMapping("/problem-bank/coverage")
    public List<ProblemBankService.PatternCoverage> coverage(@AuthenticationPrincipal UserPrincipal me) {
        return bank.coverage(me.id());
    }

    @GetMapping("/export/json")
    public ResponseEntity<Map<String, Object>> exportJson(@AuthenticationPrincipal UserPrincipal me) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=code-quest-export.json")
                .body(data.exportAll(me.id()));
    }

    @GetMapping(value = "/export/problems.csv", produces = "text/csv")
    public ResponseEntity<String> exportCsv(@AuthenticationPrincipal UserPrincipal me) {
        List<Map<String, Object>> rows = records.problemsForCsv(me.id());
        StringBuilder sb = new StringBuilder("solved_on,title,platform,difficulty,patterns,minutes,attempts,hint_level,confidence,link,approach\n");
        for (Map<String, Object> r : rows) {
            sb.append(r.values().stream().map(ProgressController::csv).collect(Collectors.joining(","))).append('\n');
        }
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=problems.csv")
                .contentType(MediaType.parseMediaType("text/csv")).body(sb.toString());
    }

    public record DeleteRequest(@NotBlank String password) {}

    @PostMapping("/account/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@AuthenticationPrincipal UserPrincipal me, @RequestBody @jakarta.validation.Valid DeleteRequest req) {
        data.deleteAccount(me.id(), req.password());
    }

    static String csv(Object v) {
        if (v == null) return "";
        String s = v.toString().replace("\"", "\"\"");
        // Neutralise spreadsheet formula injection (cells starting with = + - @).
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) s = "'" + s;
        return "\"" + s + "\"";
    }
}
