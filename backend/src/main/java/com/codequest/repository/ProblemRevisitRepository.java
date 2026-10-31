package com.codequest.repository;

import com.codequest.entity.ProblemRevisit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProblemRevisitRepository extends JpaRepository<ProblemRevisit, Long> {
    @EntityGraph(attributePaths = "problem")
    List<ProblemRevisit> findByUserIdAndCompletedAtIsNullAndDueDateLessThanEqualOrderByDueDate(Long userId, LocalDate date);

    @EntityGraph(attributePaths = "problem")
    List<ProblemRevisit> findByUserIdAndCompletedAtIsNullAndDueDateGreaterThanOrderByDueDate(Long userId, LocalDate date);

    long countByUserIdAndCompletedAtIsNullAndDueDateLessThanEqual(Long userId, LocalDate date);

    @EntityGraph(attributePaths = "problem")
    Optional<ProblemRevisit> findByIdAndUserId(Long id, Long userId);

    List<ProblemRevisit> findByProblemIdOrderByRound(Long problemId);
}
