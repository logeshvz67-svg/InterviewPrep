package com.codequest.service;

import com.codequest.domain.QuestTrigger;
import com.codequest.domain.XpAction;
import com.codequest.dto.FocusDtos.*;
import com.codequest.dto.GameDtos.XpAward;
import com.codequest.dto.PageResponse;
import com.codequest.entity.DailyCommitment;
import com.codequest.entity.DistractionEntry;
import com.codequest.entity.FocusSession;
import com.codequest.entity.GymEnums.DistractionCategory;
import com.codequest.entity.GymEnums.FocusMode;
import com.codequest.entity.GymEnums.FocusStatus;
import com.codequest.exception.ApiException;
import com.codequest.mapper.Mappers;
import com.codequest.repository.DailyCommitmentRepository;
import com.codequest.repository.DistractionRepository;
import com.codequest.repository.FocusSessionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Focus tools: Pomodoro / Minimum Viable Day sessions, distraction log, daily commitment.
 *
 * Pomodoro with NO server timer: we save started_at; the browser shows the countdown;
 * on "complete" the server compares now with started_at. The free server can sleep mid-session
 * and nothing is lost, because the truth is in the database.
 */
@Service
public class FocusService {
    public static final int MVD_MINUTES = 20;
    public static final int POMODORO_XP_MINUTES = 25;
    /** Small grace so a browser tab that fires a few seconds early still counts. */
    private static final Duration GRACE = Duration.ofSeconds(45);
    /** A session left running this long after its end is treated as abandoned. */
    private static final Duration STALE_AFTER_END = Duration.ofHours(3);

    private final FocusSessionRepository sessions;
    private final DistractionRepository distractions;
    private final DailyCommitmentRepository commitments;
    private final XpService xp;
    private final QuestService quests;
    private final StreakService streaks;
    private final TimeService time;

    public FocusService(FocusSessionRepository sessions, DistractionRepository distractions, DailyCommitmentRepository commitments,
                        XpService xp, QuestService quests, StreakService streaks, TimeService time) {
        this.sessions = sessions;
        this.distractions = distractions;
        this.commitments = commitments;
        this.xp = xp;
        this.quests = quests;
        this.streaks = streaks;
        this.time = time;
    }

    // ------------------------------------------------------------- sessions

    @Transactional
    public FocusView start(Long userId, StartFocusRequest req) {
        Optional<FocusSession> running = activeEntity(userId);
        if (running.isPresent()) throw ApiException.conflict("You already have a focus session running. Finish or cancel it first.");
        int minutes = req.mode() == FocusMode.MVD ? MVD_MINUTES : req.plannedMinutes();
        FocusSession f = new FocusSession();
        f.setUserId(userId);
        f.setMode(req.mode());
        f.setTopic(req.topic().trim());
        f.setPlannedMinutes(minutes);
        f.setStartedAt(time.now());
        f.setLocalDate(time.today(userId));
        sessions.save(f);
        return Mappers.toView(f, time.now());
    }

    @Transactional
    public FocusView active(Long userId) {
        return activeEntity(userId).map(f -> Mappers.toView(f, time.now())).orElse(null);
    }

    @Transactional
    public FocusCompleted complete(Long userId, Long id) {
        FocusSession f = sessions.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Focus session"));
        if (f.getStatus() != FocusStatus.RUNNING) throw ApiException.conflict("This session is already " + f.getStatus().name().toLowerCase());
        Instant now = time.now();
        Instant plannedEnd = f.getStartedAt().plus(Duration.ofMinutes(f.getPlannedMinutes()));
        if (now.plus(GRACE).isBefore(plannedEnd)) {
            long left = Duration.between(now, plannedEnd).toMinutes() + 1;
            throw ApiException.badRequest("Timer isn't finished yet: about " + left + " min left. You can do it!");
        }
        int actual = (int) Math.min(Duration.between(f.getStartedAt(), now).toMinutes(), f.getPlannedMinutes() + 30L);
        f.setStatus(FocusStatus.COMPLETED);
        f.setEndedAt(now);
        f.setActualMinutes(Math.max(actual, f.getPlannedMinutes()));

        XpAward award = XpAward.none("Focus sessions of 25+ minutes earn XP");
        if (f.getMode() == FocusMode.POMODORO && f.getPlannedMinutes() >= POMODORO_XP_MINUTES) {
            award = xp.award(userId, XpAction.FOCUS_SESSION_25, "focus:" + f.getId(), f.getTopic());
            quests.advance(userId, QuestTrigger.FOCUS_25);
        } else if (f.getMode() == FocusMode.MVD) {
            quests.advance(userId, QuestTrigger.MVD_DONE);
            award = XpAward.none("Minimum Viable Day complete. Streak protected!");
        } else {
            quests.advance(userId, QuestTrigger.FOCUS_ANY);
        }
        streaks.recordActivity(userId); // any finished session keeps the streak alive
        return new FocusCompleted(Mappers.toView(f, now), award, true);
    }

    @Transactional
    public FocusView cancel(Long userId, Long id) {
        FocusSession f = sessions.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Focus session"));
        if (f.getStatus() == FocusStatus.RUNNING) {
            f.setStatus(FocusStatus.CANCELLED);
            f.setEndedAt(time.now());
        }
        return Mappers.toView(f, time.now());
    }

    @Transactional(readOnly = true)
    public FocusStats stats(Long userId, int days) {
        LocalDate from = time.today(userId).minusDays(days - 1L);
        List<FocusSession> done = sessions.findByUserIdAndStatusAndLocalDateGreaterThanEqual(userId, FocusStatus.COMPLETED, from);
        Map<String, Integer> byTopic = new TreeMap<>();
        Map<LocalDate, Integer> byDay = new TreeMap<>();
        int total = 0;
        for (FocusSession f : done) {
            int m = f.getActualMinutes() == null ? 0 : f.getActualMinutes();
            total += m;
            byTopic.merge(f.getTopic(), m, Integer::sum);
            byDay.merge(f.getLocalDate(), m, Integer::sum);
        }
        return new FocusStats(days, total, byTopic, byDay);
    }

    /** Running session, auto-cancelling one that was clearly abandoned (tab closed hours ago). */
    private Optional<FocusSession> activeEntity(Long userId) {
        Optional<FocusSession> f = sessions.findFirstByUserIdAndStatusOrderByStartedAtDesc(userId, FocusStatus.RUNNING);
        if (f.isPresent()) {
            FocusSession s = f.get();
            Instant end = s.getStartedAt().plus(Duration.ofMinutes(s.getPlannedMinutes()));
            if (time.now().isAfter(end.plus(STALE_AFTER_END))) {
                s.setStatus(FocusStatus.CANCELLED);
                s.setEndedAt(end);
                return Optional.empty();
            }
        }
        return f;
    }

    // ------------------------------------------------------------- distractions

    @Transactional
    public DistractionView logDistraction(Long userId, DistractionRequest req) {
        ZonedDateTime local = time.localNow(userId);
        DistractionEntry d = new DistractionEntry();
        d.setUserId(userId);
        d.setCategory(req.category());
        d.setNote(req.note());
        d.setLocalDate(local.toLocalDate());
        d.setLocalHour(local.getHour());
        distractions.save(d);
        return Mappers.toView(d);
    }

    @Transactional(readOnly = true)
    public PageResponse<DistractionView> distractions(Long userId, int page, int size) {
        return PageResponse.of(distractions.findByUserId(userId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))), Mappers::toView);
    }

    @Transactional(readOnly = true)
    public DistractionInsights insights(Long userId, int days) {
        LocalDate from = time.today(userId).minusDays(days - 1L);
        List<DistractionEntry> list = distractions.findByUserIdAndLocalDateGreaterThanEqual(userId, from);
        Map<DistractionCategory, Long> byCat = list.stream().collect(Collectors.groupingBy(DistractionEntry::getCategory, () -> new EnumMap<>(DistractionCategory.class), Collectors.counting()));
        Map<Integer, Long> byHour = list.stream().collect(Collectors.groupingBy(DistractionEntry::getLocalHour, TreeMap::new, Collectors.counting()));
        String top = byCat.entrySet().stream().max(Map.Entry.comparingByValue()).map(e -> e.getKey().name()).orElse(null);
        Integer worstHour = byHour.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
        return new DistractionInsights(days, list.size(), byCat, byHour, top, worstHour, tipFor(top));
    }

    private static String tipFor(String category) {
        if (category == null) return "No distractions logged. Nice focus!";
        return switch (DistractionCategory.valueOf(category)) {
            case PHONE, SOCIAL_MEDIA, MESSAGES -> "Put your phone in another room for one Pomodoro. Just 25 minutes.";
            case VIDEO -> "Close video tabs before starting. Watch as a reward after the session.";
            case TIRED -> "Try your hardest topic at the time you feel freshest, and use the Minimum Viable Day on tired days.";
            case HUNGRY -> "Keep water and a small snack ready before you start.";
            case OVERTHINKING -> "Open the 'Do this now' screen: it shows only ONE next step.";
            case NOISE -> "Try earphones with no lyrics, or study at a quieter hour.";
            case WORK -> "Block a fixed slot after work and protect it like a meeting.";
            case OTHER -> "Look at the notes you wrote; the pattern usually shows itself after a week.";
        };
    }

    // ------------------------------------------------------------- commitments

    /** Pick tomorrow's (up to) 3 tasks. +10 XP once per day. */
    @Transactional
    public CommitmentSaved commitTomorrow(Long userId, CommitmentRequest req) {
        LocalDate today = time.today(userId);
        LocalDate tomorrow = today.plusDays(1);
        DailyCommitment c = commitments.findByUserIdAndForDate(userId, tomorrow).orElseGet(() -> {
            DailyCommitment n = new DailyCommitment();
            n.setUserId(userId);
            n.setForDate(tomorrow);
            return n;
        });
        c.setTasks(req.tasks().stream().map(t -> new DailyCommitment.Task(t.text().trim(), false)).collect(Collectors.toCollection(ArrayList::new)));
        commitments.save(c);
        XpAward award = xp.award(userId, XpAction.DAILY_COMMITMENT, "commit:" + tomorrow, null);
        quests.advance(userId, QuestTrigger.DAILY_COMMITMENT);
        return new CommitmentSaved(toView(c), award);
    }

    @Transactional(readOnly = true)
    public CommitmentView commitmentFor(Long userId, boolean tomorrow) {
        LocalDate d = time.today(userId).plusDays(tomorrow ? 1 : 0);
        return commitments.findByUserIdAndForDate(userId, d).map(FocusService::toView).orElse(new CommitmentView(d, List.of()));
    }

    @Transactional
    public CommitmentView toggleToday(Long userId, int index) {
        LocalDate today = time.today(userId);
        DailyCommitment c = commitments.findByUserIdAndForDate(userId, today).orElseThrow(() -> ApiException.notFound("Today's commitment"));
        if (index < 0 || index >= c.getTasks().size()) throw ApiException.badRequest("No task at position " + index);
        List<DailyCommitment.Task> tasks = new ArrayList<>(c.getTasks());
        DailyCommitment.Task t = tasks.get(index);
        tasks.set(index, new DailyCommitment.Task(t.text(), !t.done()));
        c.setTasks(tasks); // new list instance so Hibernate notices the JSON changed
        return toView(c);
    }

    private static CommitmentView toView(DailyCommitment c) {
        return new CommitmentView(c.getForDate(), c.getTasks().stream().map(t -> new CommitmentTask(t.text(), t.done())).toList());
    }

}
