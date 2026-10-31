package com.codequest.controller;

import com.codequest.dto.GameDtos.*;
import com.codequest.dto.PageResponse;
import com.codequest.security.UserPrincipal;
import com.codequest.service.QuestService;
import com.codequest.service.StreakService;
import com.codequest.service.XpService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Read-only game state. Note: there is NO endpoint to add XP; only server actions award it. */
@Tag(name = "XP, streaks and quests")
@Validated
@RestController
@RequestMapping("/api")
public class GameController {
    private final XpService xp;
    private final StreakService streaks;
    private final QuestService quests;

    public GameController(XpService xp, StreakService streaks, QuestService quests) {
        this.xp = xp;
        this.streaks = streaks;
        this.quests = quests;
    }

    @GetMapping("/xp/events")
    public PageResponse<XpEventView> xpHistory(@AuthenticationPrincipal UserPrincipal me,
                                               @RequestParam(defaultValue = "0") @Min(0) int page,
                                               @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return xp.history(me.id(), page, size);
    }

    @GetMapping("/streak")
    public StreakView streak(@AuthenticationPrincipal UserPrincipal me) {
        return streaks.view(me.id());
    }

    @PostMapping("/streak/comeback/ack")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ackComeback(@AuthenticationPrincipal UserPrincipal me) {
        streaks.acknowledgeComeback(me.id());
    }

    @GetMapping("/quests/today")
    public List<QuestView> todayQuests(@AuthenticationPrincipal UserPrincipal me) {
        return quests.today(me.id());
    }
}
