package com.codequest.repository;

import com.codequest.entity.ProblemSolved;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProblemSolvedRepository extends JpaRepository<ProblemSolved, Long> {
    /** Always look up by id AND userId: one user can never read another user's row. */
    @EntityGraph(attributePaths = "patterns")
    Optional<ProblemSolved> findByIdAndUserId(Long id, Long userId);

    @EntityGraph(attributePaths = "patterns")
    Page<ProblemSolved> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = "patterns")
    @Query("select distinct p from ProblemSolved p join p.patterns pat where p.userId = :userId and pat.code = :patternCode")
    Page<ProblemSolved> findByUserIdAndPattern(Long userId, String patternCode, Pageable pageable);

    @EntityGraph(attributePaths = "patterns")
    List<ProblemSolved> findAllByUserId(Long userId);

    long countByUserId(Long userId);
}
