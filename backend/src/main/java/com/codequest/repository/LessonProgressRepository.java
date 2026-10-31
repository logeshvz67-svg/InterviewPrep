package com.codequest.repository;

import com.codequest.entity.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {
    List<LessonProgress> findByUserId(Long userId);
    Optional<LessonProgress> findByUserIdAndLessonId(Long userId, Long lessonId);
    long countByUserIdAndStatus(Long userId, LessonProgress.Status status);

    @Query("select count(p) from LessonProgress p where p.userId = :userId and p.completedAt >= :since")
    long countCompletedSince(Long userId, Instant since);
}
