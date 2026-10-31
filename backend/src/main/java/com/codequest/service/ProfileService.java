package com.codequest.service;

import com.codequest.domain.RoadmapGenerator;
import com.codequest.domain.RoadmapWeek;
import com.codequest.dto.ProfileDtos.*;
import com.codequest.entity.Profile;
import com.codequest.entity.Roadmap;
import com.codequest.exception.ApiException;
import com.codequest.mapper.Mappers;
import com.codequest.repository.CompanyTagRepository;
import com.codequest.repository.ProfileRepository;
import com.codequest.repository.RoadmapRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class ProfileService {
    private final ProfileRepository profiles;
    private final RoadmapRepository roadmaps;
    private final CompanyTagRepository companies;
    private final TimeService time;

    public ProfileService(ProfileRepository profiles, RoadmapRepository roadmaps, CompanyTagRepository companies, TimeService time) {
        this.profiles = profiles;
        this.roadmaps = roadmaps;
        this.companies = companies;
        this.time = time;
    }

    @Transactional(readOnly = true)
    public ProfileView get(Long userId) {
        return Mappers.toView(load(userId));
    }

    @Transactional
    public ProfileView completeOnboarding(Long userId, OnboardingRequest req) {
        if (!TimeService.isValidZone(req.timezone())) throw ApiException.badRequest("Unknown timezone: " + req.timezone());
        for (String c : req.laterCompanies()) {
            if (!companies.existsByCode(c) || "ALL".equals(c)) throw ApiException.badRequest("Unknown company: " + c);
        }
        Profile p = load(userId);
        p.setCharacterName(req.characterName().trim());
        p.setAvatar(req.avatar());
        p.setPrimaryCompany(req.primaryCompany());
        // Remove duplicates and the primary company from the "Later" lane.
        var later = new LinkedHashSet<>(req.laterCompanies());
        later.remove(req.primaryCompany());
        p.setLaterCompanies(List.copyOf(later));
        p.setTestDate(req.testDate());
        p.setTimezone(req.timezone());
        p.setDailyMinutes(req.dailyMinutes());
        p.setWhyStatement(req.whyStatement());
        p.setSelfAssessment(new HashMap<>(req.selfAssessment())); // informational only; never skips content
        p.setOnboardingComplete(true);
        regenerateRoadmap(userId, p);
        return Mappers.toView(p);
    }

    @Transactional
    public ProfileView update(Long userId, ProfileUpdateRequest req) {
        if (!TimeService.isValidZone(req.timezone())) throw ApiException.badRequest("Unknown timezone: " + req.timezone());
        Profile p = load(userId);
        p.setCharacterName(req.characterName().trim());
        p.setAvatar(req.avatar());
        p.setTimezone(req.timezone());
        p.setDailyMinutes(req.dailyMinutes());
        p.setWhyStatement(req.whyStatement());
        p.setSoundEnabled(req.soundEnabled());
        return Mappers.toView(p);
    }

    /** Small per-user settings stored as JSON. Only known keys with sane values are accepted. */
    @Transactional
    public ProfileView updateSettings(Long userId, java.util.Map<String, Object> in) {
        Profile p = load(userId);
        java.util.Map<String, Object> s = new java.util.HashMap<>(p.getSettings());
        in.forEach((k, v) -> {
            switch (k) {
                case "voiceLang" -> { if (v instanceof String str && str.matches("[a-z]{2}(-[A-Z]{2})?")) s.put(k, str); else throw ApiException.badRequest("voiceLang like en-IN"); }
                case "voiceRate" -> { if (v instanceof Number n && n.doubleValue() >= 0.5 && n.doubleValue() <= 2) s.put(k, n.doubleValue()); else throw ApiException.badRequest("voiceRate 0.5-2"); }
                case "reminderTime" -> { if (v == null || (v instanceof String str && str.matches("([01]\\d|2[0-3]):[0-5]\\d"))) s.put(k, v); else throw ApiException.badRequest("reminderTime HH:mm"); }
                case "readyThreshold" -> { if (v instanceof Number n && n.intValue() >= 40 && n.intValue() <= 100) s.put(k, n.intValue()); else throw ApiException.badRequest("readyThreshold 40-100"); }
                case "aiNoticeSeen", "notificationsOn" -> s.put(k, Boolean.TRUE.equals(v));
                default -> throw ApiException.badRequest("Unknown setting: " + k);
            }
        });
        p.setSettings(s);
        return Mappers.toView(p);
    }

    /** "Switch company": changes the primary target. Readiness and filters recompute from it. */
    @Transactional
    public ProfileView switchCompany(Long userId, SwitchCompanyRequest req) {
        if (!companies.existsByCode(req.companyCode()) || "ALL".equals(req.companyCode())) {
            throw ApiException.badRequest("Unknown company: " + req.companyCode());
        }
        Profile p = load(userId);
        String old = p.getPrimaryCompany();
        p.setPrimaryCompany(req.companyCode());
        var later = new LinkedHashSet<>(p.getLaterCompanies());
        later.remove(req.companyCode());
        if (!old.equals(req.companyCode())) later.add(old);
        p.setLaterCompanies(List.copyOf(later));
        return Mappers.toView(p);
    }

    @Transactional
    public RoadmapView setTestDate(Long userId, TestDateRequest req) {
        Profile p = load(userId);
        p.setTestDate(req.testDate());
        regenerateRoadmap(userId, p);
        return roadmap(userId);
    }

    @Transactional
    public RoadmapView regenerate(Long userId) {
        regenerateRoadmap(userId, load(userId));
        return roadmap(userId);
    }

    @Transactional(readOnly = true)
    public RoadmapView roadmap(Long userId) {
        Roadmap r = roadmaps.findFirstByUserIdAndActiveTrueOrderByIdDesc(userId)
                .orElseThrow(() -> ApiException.notFound("Roadmap (finish onboarding first)"));
        return new RoadmapView(r.getStartDate(), r.getEndDate(), r.getTotalWeeks(), currentWeek(r, time.today(userId)), r.getWeeks());
    }

    public static int currentWeek(Roadmap r, LocalDate today) {
        long w = ChronoUnit.WEEKS.between(r.getStartDate(), today) + 1;
        return (int) Math.max(1, Math.min(r.getTotalWeeks(), w));
    }

    private void regenerateRoadmap(Long userId, Profile p) {
        LocalDate today = time.today(userId);
        // Weeks start on Monday so "Week 3" means the same thing all week.
        LocalDate start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        RoadmapGenerator.Track track = "INFOSYS_DSE".equals(p.getPrimaryCompany()) ? RoadmapGenerator.Track.DSE : RoadmapGenerator.Track.SP;
        List<RoadmapWeek> weeks = RoadmapGenerator.generate(start, p.getTestDate(), track);
        roadmaps.deactivateAll(userId);
        Roadmap r = new Roadmap();
        r.setUserId(userId);
        r.setStartDate(start);
        r.setEndDate(start.plusWeeks(weeks.size()).minusDays(1));
        r.setTotalWeeks(weeks.size());
        r.setWeeks(weeks);
        roadmaps.save(r);
    }

    Profile load(Long userId) {
        return profiles.findByUserId(userId).orElseThrow(() -> ApiException.notFound("Profile"));
    }
}
