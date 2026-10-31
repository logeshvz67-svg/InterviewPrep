package com.codequest.service;

import com.codequest.domain.QuestTrigger;
import com.codequest.dto.GameDtos.QuestView;
import com.codequest.entity.Profile;
import com.codequest.entity.Quest;
import com.codequest.entity.UserQuest;
import com.codequest.mapper.Mappers;
import com.codequest.repository.ProblemRevisitRepository;
import com.codequest.repository.ProfileRepository;
import com.codequest.repository.QuestRepository;
import com.codequest.repository.UserQuestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Today's quests (max 3). Created lazily the first time the user opens the app each day
 * (no scheduled jobs, which a sleeping free server couldn't run anyway).
 * Quests pay GOLD only; XP comes from the underlying actions, so nothing is rewarded twice.
 */
@Service
public class QuestService {
    public static final int MAX_DAILY_QUESTS = 3;

    private final QuestRepository quests;
    private final UserQuestRepository userQuests;
    private final ProblemRevisitRepository revisits;
    private final ProfileRepository profiles;
    private final StreakService streaks;
    private final TimeService time;

    public QuestService(QuestRepository quests, UserQuestRepository userQuests, ProblemRevisitRepository revisits,
                        ProfileRepository profiles, StreakService streaks, TimeService time) {
        this.quests = quests;
        this.userQuests = userQuests;
        this.revisits = revisits;
        this.profiles = profiles;
        this.streaks = streaks;
        this.time = time;
    }

    @Transactional
    public List<QuestView> today(Long userId) {
        LocalDate today = time.today(userId);
        List<UserQuest> existing = new ArrayList<>(userQuests.findByUserIdAndForDateOrderById(userId, today));
        if (existing.isEmpty()) {
            for (String code : pickCodes(userId, today)) {
                quests.findByCode(code).ifPresent(q -> {
                    UserQuest uq = new UserQuest();
                    uq.setUserId(userId);
                    uq.setQuest(q);
                    uq.setForDate(today);
                    existing.add(userQuests.save(uq));
                });
            }
        }
        return existing.stream().map(Mappers::toView).toList();
    }

    /** Which 3 quests today? Comeback days are gentle; due revisits get priority. */
    List<String> pickCodes(Long userId, LocalDate today) {
        if (streaks.view(userId).comebackPending()) {
            return List.of("COMEBACK_EASY", "TOPIC_STEP", "COMMIT");
        }
        List<String> codes = new ArrayList<>();
        codes.add("TOPIC_STEP");
        if (revisits.countByUserIdAndCompletedAtIsNullAndDueDateLessThanEqual(userId, today) > 0) {
            codes.add("REVISIT");
        } else {
            codes.add(today.getDayOfYear() % 2 == 0 ? "LOG_PROBLEM" : "RECORD_MISTAKE");
        }
        codes.add(today.getDayOfWeek().getValue() >= 6 ? "COMMIT" : "FOCUS_25");
        return codes.subList(0, Math.min(MAX_DAILY_QUESTS, codes.size()));
    }

    /** Called by other services when the user does something. Completes matching quests and pays gold. */
    @Transactional
    public void advance(Long userId, QuestTrigger trigger) {
        LocalDate today = time.today(userId);
        today(userId); // make sure today's quests exist
        for (UserQuest uq : userQuests.findByUserIdAndForDateOrderById(userId, today)) {
            Quest q = uq.getQuest();
            if (uq.isCompleted() || !matches(q.getTriggerType(), trigger)) continue;
            uq.setProgress(uq.getProgress() + 1);
            if (uq.getProgress() >= q.getTarget()) {
                uq.setCompletedAt(time.now());
                profiles.findByUserId(userId).ifPresent((Profile p) -> p.setGold(p.getGold() + q.getGoldReward()));
            }
        }
    }

    /** FOCUS_ANY quests accept any focus session; a 25-minute one also counts. */
    private static boolean matches(QuestTrigger questType, QuestTrigger happened) {
        if (questType == happened) return true;
        return questType == QuestTrigger.FOCUS_ANY && (happened == QuestTrigger.FOCUS_25 || happened == QuestTrigger.MVD_DONE);
    }
}
