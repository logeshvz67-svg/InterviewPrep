package com.codequest.controller;

import com.codequest.dto.DashboardDtos.CommandCenter;
import com.codequest.dto.DashboardDtos.ReadinessView;
import com.codequest.dto.FocusDtos.NextAction;
import com.codequest.security.UserPrincipal;
import com.codequest.service.DashboardService;
import com.codequest.service.ProfileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Command Center")
@RestController
@RequestMapping("/api")
public class DashboardController {
    private final DashboardService dashboard;
    private final ProfileService profiles;

    public DashboardController(DashboardService dashboard, ProfileService profiles) {
        this.dashboard = dashboard;
        this.profiles = profiles;
    }

    @GetMapping("/command-center")
    public CommandCenter commandCenter(@AuthenticationPrincipal UserPrincipal me) {
        return dashboard.commandCenter(me.id());
    }

    @GetMapping("/next-action")
    public NextAction nextAction(@AuthenticationPrincipal UserPrincipal me) {
        return dashboard.nextAction(me.id());
    }

    /** Readiness for any company (defaults to your primary target). */
    @GetMapping("/readiness")
    public ReadinessView readiness(@AuthenticationPrincipal UserPrincipal me, @RequestParam(required = false) String company) {
        String code = company == null || company.isBlank() ? profiles.get(me.id()).primaryCompany() : company;
        return dashboard.readiness(me.id(), code);
    }
}
