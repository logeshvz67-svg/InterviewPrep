package com.codequest.repository;

import com.codequest.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    List<QuizAttempt> findByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserIdAndKindAndPassedTrue(Long userId, String kind);
    List<QuizAttempt> findByUserIdAndLessonIdOrderByCreatedAtDesc(Long userId, Long lessonId);
}
