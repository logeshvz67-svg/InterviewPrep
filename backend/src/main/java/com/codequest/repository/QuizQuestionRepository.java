package com.codequest.repository;

import com.codequest.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {
    List<QuizQuestion> findByLessonIdOrderBySortOrder(Long lessonId);
    List<QuizQuestion> findByLessonIdIn(Collection<Long> lessonIds);
    List<QuizQuestion> findByIdIn(Collection<Long> ids);

    @Modifying
    @Query("delete from QuizQuestion q where q.lessonId = :lessonId")
    void deleteByLessonId(Long lessonId);
}
