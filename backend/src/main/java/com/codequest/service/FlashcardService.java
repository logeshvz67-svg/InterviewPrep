package com.codequest.service;

import com.codequest.domain.QuestTrigger;
import com.codequest.domain.Sm2Scheduler;
import com.codequest.domain.Sm2Scheduler.Grade;
import com.codequest.domain.XpAction;
import com.codequest.dto.GameDtos.XpAward;
import com.codequest.dto.LearningDtos.*;
import com.codequest.dto.PageResponse;
import com.codequest.entity.Flashcard;
import com.codequest.entity.FlashcardReview;
import com.codequest.entity.Lesson;
import com.codequest.entity.Mistake;
import com.codequest.exception.ApiException;
import com.codequest.repository.FlashcardRepository;
import com.codequest.repository.FlashcardReviewRepository;
import com.codequest.repository.MistakeCategoryRepository;
import com.codequest.repository.MistakeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** Flashcards + spaced repetition. All scheduling is computed on the server from the SM-2 rules. */
@Service
public class FlashcardService {
    public static final int RECALL_MIN_REVIEWS = 5;

    private final FlashcardRepository cards;
    private final FlashcardReviewRepository reviews;
    private final MistakeRepository mistakes;
    private final MistakeCategoryRepository categories;
    private final XpService xp;
    private final QuestService quests;
    private final StreakService streaks;
    private final TimeService time;

    public FlashcardService(FlashcardRepository cards, FlashcardReviewRepository reviews, MistakeRepository mistakes,
                            MistakeCategoryRepository categories, XpService xp, QuestService quests, StreakService streaks, TimeService time) {
        this.cards = cards;
        this.reviews = reviews;
        this.mistakes = mistakes;
        this.categories = categories;
        this.xp = xp;
        this.quests = quests;
        this.streaks = streaks;
        this.time = time;
    }

    /** Auto-create the lesson's flashcards (first review tomorrow). Safe to call twice. */
    @Transactional
    public int createFromLesson(Long userId, Lesson l) {
        int n = 0;
        int i = 0;
        for (JsonNode c : l.getContent().path("flashcards")) {
            String ref = "lesson:" + l.getSlug() + ":" + i++;
            if (cards.existsByUserIdAndSourceTypeAndSourceRef(userId, "LESSON", ref)) continue;
            save(userId, c.path("front").asText(), c.path("back").asText(), l.getTitle(), "LESSON", ref);
            n++;
        }
        return n;
    }

    @Transactional
    public FlashcardView createFromMistake(Long userId, Long mistakeId) {
        Mistake m = mistakes.findByIdAndUserId(mistakeId, userId).orElseThrow(() -> ApiException.notFound("Mistake"));
        String ref = "mistake:" + m.getId();
        if (cards.existsByUserIdAndSourceTypeAndSourceRef(userId, "MISTAKE", ref)) throw ApiException.conflict("This mistake is already a flashcard");
        String cat = categories.findByCode(m.getCategoryCode()).map(c -> c.getName()).orElse(m.getCategoryCode());
        return toView(save(userId, "Avoid this (" + cat + "): " + m.getWhatHappened(), m.getLessonLearned(),
                m.getTopic() == null ? cat : m.getTopic(), "MISTAKE", ref));
    }

    @Transactional
    public FlashcardView createManual(Long userId, ManualCardRequest req, String sourceType, String sourceRef) {
        String ref = sourceRef != null ? sourceRef : "manual:" + System.nanoTime();
        if (cards.existsByUserIdAndSourceTypeAndSourceRef(userId, sourceType, ref)) throw ApiException.conflict("Already a flashcard");
        return toView(save(userId, req.front().trim(), req.back().trim(), req.topic(), sourceType, ref));
    }

    private Flashcard save(Long userId, String front, String back, String topic, String type, String ref) {
        Flashcard f = new Flashcard();
        f.setUserId(userId);
        f.setFront(front);
        f.setBack(back);
        f.setTopic(topic);
        f.setSourceType(type);
        f.setSourceRef(ref);
        f.setNextReviewOn(time.today(userId).plusDays(1));
        return cards.save(f);
    }

    @Transactional(readOnly = true)
    public List<FlashcardView> due(Long userId, int limit) {
        return cards.findByUserIdAndSuspendedFalseAndNextReviewOnLessThanEqualOrderByNextReviewOn(userId, time.today(userId), PageRequest.of(0, limit))
                .stream().map(FlashcardService::toView).toList();
    }

    @Transactional(readOnly = true)
    public int dueCount(Long userId) {
        return (int) cards.countByUserIdAndSuspendedFalseAndNextReviewOnLessThanEqual(userId, time.today(userId));
    }

    @Transactional(readOnly = true)
    public int reviewedToday(Long userId) {
        return (int) reviews.countByUserIdAndLocalDate(userId, time.today(userId));
    }

    @Transactional
    public ReviewResult review(Long userId, Long id, ReviewRequest req) {
        Flashcard f = cards.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Flashcard"));
        LocalDate today = time.today(userId);
        Grade g = Grade.valueOf(req.grade());
        var next = Sm2Scheduler.review(new Sm2Scheduler.State(f.getEase(), f.getIntervalDays(), f.getRepetitions(), f.getLapses()), g);

        FlashcardReview r = new FlashcardReview();
        r.setUserId(userId);
        r.setFlashcardId(f.getId());
        r.setGrade(g.value);
        r.setIntervalBefore(f.getIntervalDays());
        r.setIntervalAfter(next.intervalDays());
        r.setEaseAfter(next.ease());
        r.setLocalDate(today);
        reviews.save(r);

        f.setEase(next.ease());
        f.setIntervalDays(next.intervalDays());
        f.setRepetitions(next.repetitions());
        f.setLapses(next.lapses());
        f.setNextReviewOn(today.plusDays(next.intervalDays()));
        f.setLastReviewedAt(time.now());
        streaks.recordActivity(userId);
        return new ReviewResult(toView(f), reviewedToday(userId), dueCount(userId));
    }

    /**
     * Recall session reward: once per day, after reviewing at least 5 cards (or every due card if fewer were due).
     */
    @Transactional
    public RecallComplete completeRecall(Long userId) {
        int done = reviewedToday(userId);
        int left = dueCount(userId);
        if (done == 0 || (done < RECALL_MIN_REVIEWS && left > 0)) {
            return new RecallComplete(XpAward.none("Review at least " + RECALL_MIN_REVIEWS + " cards (or all due cards) to finish a recall session."), done,
                    "Keep going: " + Math.max(0, RECALL_MIN_REVIEWS - done) + " more to go.");
        }
        XpAward a = xp.award(userId, XpAction.RECALL_SESSION, "recall:" + time.today(userId), done + " cards");
        quests.advance(userId, QuestTrigger.RECALL_DONE);
        return new RecallComplete(a, done, "Recall done! Remembering beats re-reading every time.");
    }

    @Transactional(readOnly = true)
    public PageResponse<FlashcardView> list(Long userId, String q, int page, int size) {
        String query = q == null ? "" : q.trim();
        return PageResponse.of(cards.search(userId, query, PageRequest.of(page, size)), FlashcardService::toView);
    }

    @Transactional
    public FlashcardView setSuspended(Long userId, Long id, boolean suspended) {
        Flashcard f = cards.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Flashcard"));
        f.setSuspended(suspended);
        return toView(f);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        cards.delete(cards.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Flashcard")));
    }

    public static FlashcardView toView(Flashcard f) {
        return new FlashcardView(f.getId(), f.getFront(), f.getBack(), f.getTopic(), f.getSourceType(), f.getIntervalDays(),
                f.getRepetitions(), f.getNextReviewOn(), f.isSuspended());
    }
}
