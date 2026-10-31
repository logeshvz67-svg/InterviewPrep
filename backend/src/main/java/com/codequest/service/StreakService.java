package com.codequest.service;

import com.codequest.domain.StreakCalculator;
import com.codequest.domain.StreakCalculator.State;
import com.codequest.dto.GameDtos.StreakView;
import com.codequest.entity.ActivityDay;
import com.codequest.entity.Streak;
import com.codequest.repository.ActivityDayRepository;
import com.codequest.repository.StreakRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/** Database side of streaks. The math lives in StreakCalculator (pure + unit-tested). */
@Service
public class StreakService {
    private final StreakRepository streaks;
    private final ActivityDayRepository days;
    private final TimeService time;

    public StreakService(StreakRepository streaks, ActivityDayRepository days, TimeService time) {
        this.streaks = streaks;
        this.days = days;
        this.time = time;
    }

    /** Called whenever the user does something that counts as "showing up" today. */
    @Transactional
    public StreakCalculator.Result recordActivity(Long userId) {
        LocalDate today = time.today(userId);
        Streak s = streaks.findForUpdate(userId).orElseGet(() -> {
            Streak n = new Streak();
            n.setUserId(userId);
            return streaks.save(n);
        });
        StreakCalculator.Result r = StreakCalculator.onActivity(toState(s), today);
        if (!r.changed()) return r;

        s.setCurrentStreak(r.state().current());
        s.setLongestStreak(r.state().longest());
        s.setLastActiveDate(r.state().lastActive());
        s.setFreezes(r.state().freezes());
        if (r.comebackTriggered()) {
            s.setComebackPending(true);
            s.setComebackCount(s.getComebackCount() + 1);
        }
        for (LocalDate d : r.frozenDays()) {
            if (!days.existsByUserIdAndLocalDate(userId, d)) days.save(new ActivityDay(userId, d, true));
        }
        if (!days.existsByUserIdAndLocalDate(userId, today)) days.save(new ActivityDay(userId, today, false));
        return r;
    }

    @Transactional(readOnly = true)
    public StreakView view(Long userId) {
        LocalDate today = time.today(userId);
        Streak s = streaks.findByUserId(userId).orElseGet(Streak::new);
        State st = toState(s);
        var status = StreakCalculator.status(st, today);
        // Comeback Mode is ALSO shown before the first action after a long break, so the welcome is kind.
        long away = StreakCalculator.daysAway(st, today);
        boolean comeback = s.isComebackPending()
                || (status == StreakCalculator.DisplayStatus.PAUSED && away > StreakCalculator.COMEBACK_AFTER_DAYS_AWAY);
        return new StreakView(s.getCurrentStreak(), s.getLongestStreak(), s.getFreezes(), StreakCalculator.MAX_FREEZES,
                s.getLastActiveDate(), status, comeback, away);
    }

    /** User saw the welcome-back message; hide it. */
    @Transactional
    public void acknowledgeComeback(Long userId) {
        streaks.findByUserId(userId).ifPresent(s -> s.setComebackPending(false));
    }

    private static State toState(Streak s) {
        return new State(s.getCurrentStreak(), s.getLongestStreak(), s.getLastActiveDate(), s.getFreezes());
    }
}
