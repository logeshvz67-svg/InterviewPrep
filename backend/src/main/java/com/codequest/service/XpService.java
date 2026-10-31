package com.codequest.service;

import com.codequest.domain.XpAction;
import com.codequest.dto.GameDtos.XpAward;
import com.codequest.dto.GameDtos.XpEventView;
import com.codequest.dto.PageResponse;
import com.codequest.entity.Profile;
import com.codequest.entity.XpEvent;
import com.codequest.exception.ApiException;
import com.codequest.mapper.Mappers;
import com.codequest.repository.ProfileRepository;
import com.codequest.repository.XpEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * The ONLY place XP is granted. Clients never send XP amounts; they report what they did,
 * and the server decides the reward from XpAction.
 * Guards: (1) each (action, sourceKey) pays once, (2) per-day caps stop farming.
 */
@Service
public class XpService {
    private static final Logger log = LoggerFactory.getLogger(XpService.class);

    private final XpEventRepository events;
    private final ProfileRepository profiles;
    private final StreakService streaks;
    private final TimeService time;
    private final AchievementService achievements;

    public XpService(XpEventRepository events, ProfileRepository profiles, StreakService streaks, TimeService time,
                     AchievementService achievements) {
        this.events = events;
        this.profiles = profiles;
        this.streaks = streaks;
        this.time = time;
        this.achievements = achievements;
    }

    /**
     * Propagation REQUIRED (the default): if the caller already has a transaction (e.g. GymService.logProblem),
     * we join it, so "save problem + give XP" succeed or fail together.
     */
    @Transactional
    public XpAward award(Long userId, XpAction action, String sourceKey, String note) {
        LocalDate today = time.today(userId);
        if (events.existsByUserIdAndActionAndSourceKey(userId, action, sourceKey)) {
            return XpAward.none("Already rewarded");
        }
        if (action.dailyCap() > 0 && events.countByUserIdAndActionAndLocalDate(userId, action, today) >= action.dailyCap()) {
            streaks.recordActivity(userId); // still counts as showing up
            return XpAward.none("Daily XP limit reached for this activity. Your work is still saved!");
        }
        XpEvent e = new XpEvent();
        e.setUserId(userId);
        e.setAction(action);
        e.setAmount(action.xp());
        e.setGold(action.gold());
        e.setSourceKey(sourceKey);
        e.setLocalDate(today);
        e.setNote(note);
        events.save(e);

        Profile p = profiles.findByUserId(userId).orElseThrow(() -> ApiException.notFound("Profile"));
        p.setTotalXp(p.getTotalXp() + action.xp());
        p.setGold(p.getGold() + action.gold());
        // No profiles.save(p) needed: p is "managed" inside this transaction, so Hibernate's
        // dirty checking writes the change automatically when the transaction commits.

        streaks.recordActivity(userId);
        achievements.evaluate(userId, null, 100); // badges are checked whenever XP is earned
        log.debug("XP +{} {} user={}", action.xp(), action, userId);
        return new XpAward(true, action.xp(), action.gold(), null);
    }

    @Transactional(readOnly = true)
    public PageResponse<XpEventView> history(Long userId, int page, int size) {
        return PageResponse.of(events.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size)), Mappers::toView);
    }

    @Transactional(readOnly = true)
    public int todayXp(Long userId) {
        return events.sumForDay(userId, time.today(userId));
    }
}
