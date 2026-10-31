package com.codequest.repository;

import com.codequest.entity.FlashcardReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface FlashcardReviewRepository extends JpaRepository<FlashcardReview, Long> {
    long countByUserIdAndLocalDate(Long userId, LocalDate date);

    @Query("select count(distinct r.localDate) from FlashcardReview r where r.userId = :userId")
    long countDistinctDays(Long userId);

    @Query("select count(distinct r.localDate) from FlashcardReview r where r.userId = :userId and r.localDate >= :from")
    long countDistinctDaysSince(Long userId, LocalDate from);

    interface TopicAccuracy { String getTopic(); Long getTotal(); Long getRemembered(); }

    @Query("""
            select f.topic as topic, count(r) as total, sum(case when r.grade >= 3 then 1 else 0 end) as remembered
            from FlashcardReview r join Flashcard f on f.id = r.flashcardId
            where r.userId = :userId group by f.topic order by count(r) desc""")
    List<TopicAccuracy> accuracyByTopic(Long userId);
}
