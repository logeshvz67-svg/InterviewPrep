package com.codequest.repository;

import com.codequest.entity.Flashcard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FlashcardRepository extends JpaRepository<Flashcard, Long> {
    List<Flashcard> findByUserIdAndSuspendedFalseAndNextReviewOnLessThanEqualOrderByNextReviewOn(Long userId, LocalDate date, Pageable pageable);
    long countByUserIdAndSuspendedFalseAndNextReviewOnLessThanEqual(Long userId, LocalDate date);
    Optional<Flashcard> findByIdAndUserId(Long id, Long userId);
    boolean existsByUserIdAndSourceTypeAndSourceRef(Long userId, String sourceType, String sourceRef);
    long countByUserId(Long userId);

    /** q = "" matches everything. (Passing null would make Postgres guess the parameter type and fail.) */
    @Query("select f from Flashcard f where f.userId = :userId and (lower(f.front) like lower(concat('%', :q, '%')) or lower(coalesce(f.topic, '')) like lower(concat('%', :q, '%'))) order by f.nextReviewOn")
    Page<Flashcard> search(Long userId, String q, Pageable pageable);
}
