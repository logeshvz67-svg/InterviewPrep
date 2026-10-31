package com.codequest.controller;

import com.codequest.dto.ProfileDtos.*;
import com.codequest.security.UserPrincipal;
import com.codequest.service.ProfileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Profile, onboarding and roadmap")
@RestController
@RequestMapping("/api")
public class ProfileController {
    private final ProfileService profiles;

    public ProfileController(ProfileService profiles) {
        this.profiles = profiles;
    }

    @GetMapping("/profile")
    public ProfileView get(@AuthenticationPrincipal UserPrincipal me) {
        return profiles.get(me.id());
    }

    @PutMapping("/profile")
    public ProfileView update(@AuthenticationPrincipal UserPrincipal me, @Valid @RequestBody ProfileUpdateRequest req) {
        return profiles.update(me.id(), req);
    }

    @PutMapping("/profile/settings")
    public ProfileView settings(@AuthenticationPrincipal UserPrincipal me, @RequestBody java.util.Map<String, Object> settings) {
        return profiles.updateSettings(me.id(), settings);
    }

    @PutMapping("/onboarding")
    public ProfileView onboard(@AuthenticationPrincipal UserPrincipal me, @Valid @RequestBody OnboardingRequest req) {
        return profiles.completeOnboarding(me.id(), req);
    }

    @PutMapping("/profile/company")
    public ProfileView switchCompany(@AuthenticationPrincipal UserPrincipal me, @Valid @RequestBody SwitchCompanyRequest req) {
        return profiles.switchCompany(me.id(), req);
    }

    @PutMapping("/profile/test-date")
    public RoadmapView setTestDate(@AuthenticationPrincipal UserPrincipal me, @Valid @RequestBody TestDateRequest req) {
        return profiles.setTestDate(me.id(), req);
    }

    @GetMapping("/roadmap")
    public RoadmapView roadmap(@AuthenticationPrincipal UserPrincipal me) {
        return profiles.roadmap(me.id());
    }

    @PostMapping("/roadmap/regenerate")
    public RoadmapView regenerate(@AuthenticationPrincipal UserPrincipal me) {
        return profiles.regenerate(me.id());
    }
}
