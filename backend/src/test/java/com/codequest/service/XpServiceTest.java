package com.codequest.service;

import com.codequest.domain.XpAction;
import com.codequest.entity.Profile;
import com.codequest.entity.XpEvent;
import com.codequest.repository.ProfileRepository;
import com.codequest.repository.XpEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pure unit test with Mockito: repositories are fakes ("mocks"), so no database is needed.
 * We check the XP rules: award once, respect daily caps, update totals, count the streak.
 */
@ExtendWith(MockitoExtension.class)
class XpServiceTest {
    @Mock XpEventRepository events;
    @Mock ProfileRepository profiles;
    @Mock StreakService streaks;
    @Mock TimeService time;

    XpService xp;
    final LocalDate today = LocalDate.of(2026, 11, 5);

    @BeforeEach
    void setUp() {
        xp = new XpService(events, profiles, streaks, time);
        when(time.today(1L)).thenReturn(today);
    }

    @Test
    void awardsXpAndGoldAndCountsStreak() {
        Profile p = new Profile();
        p.setTotalXp(100);
        p.setGold(5);
        when(events.existsByUserIdAndActionAndSourceKey(1L, XpAction.PROBLEM_WITH_APPROACH, "problem:9")).thenReturn(false);
        when(events.countByUserIdAndActionAndLocalDate(1L, XpAction.PROBLEM_WITH_APPROACH, today)).thenReturn(0L);
        when(profiles.findByUserId(1L)).thenReturn(Optional.of(p));

        var award = xp.award(1L, XpAction.PROBLEM_WITH_APPROACH, "problem:9", "Two Sum");

        assertThat(award.awarded()).isTrue();
        assertThat(award.xp()).isEqualTo(15);
        assertThat(p.getTotalXp()).isEqualTo(115);
        assertThat(p.getGold()).isEqualTo(8);
        ArgumentCaptor<XpEvent> saved = ArgumentCaptor.forClass(XpEvent.class);
        verify(events).save(saved.capture());
        assertThat(saved.getValue().getLocalDate()).isEqualTo(today);
        verify(streaks).recordActivity(1L);
    }

    @Test
    void sameSourceIsNeverRewardedTwice() {
        when(events.existsByUserIdAndActionAndSourceKey(1L, XpAction.MISTAKE_RECORDED, "mistake:3")).thenReturn(true);

        var award = xp.award(1L, XpAction.MISTAKE_RECORDED, "mistake:3", null);

        assertThat(award.awarded()).isFalse();
        verify(events, never()).save(any());
        verifyNoInteractions(profiles);
    }

    @Test
    void dailyCapStopsFarmingButStillCountsTheDay() {
        when(events.existsByUserIdAndActionAndSourceKey(any(), any(), any())).thenReturn(false);
        when(events.countByUserIdAndActionAndLocalDate(1L, XpAction.DAILY_COMMITMENT, today)).thenReturn(1L); // cap = 1

        var award = xp.award(1L, XpAction.DAILY_COMMITMENT, "commit:2026-11-06", null);

        assertThat(award.awarded()).isFalse();
        assertThat(award.reason()).contains("Daily XP limit");
        verify(events, never()).save(any());
        verify(streaks).recordActivity(1L);
    }
}
