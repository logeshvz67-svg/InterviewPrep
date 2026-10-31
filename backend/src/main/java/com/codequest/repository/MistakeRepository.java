package com.codequest.repository;

import com.codequest.entity.Mistake;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MistakeRepository extends JpaRepository<Mistake, Long> {
    Page<Mistake> findByUserId(Long userId, Pageable pageable);
    Page<Mistake> findByUserIdAndResolved(Long userId, boolean resolved, Pageable pageable);
    Optional<Mistake> findByIdAndUserId(Long id, Long userId);
    long countByUserIdAndResolvedTrue(Long userId);

    interface CategoryCount { String getCategoryCode(); Long getTotal(); }

    @Query("select m.categoryCode as categoryCode, count(m) as total from Mistake m where m.userId = :userId and m.createdAt >= :since group by m.categoryCode order by count(m) desc")
    List<CategoryCount> countByCategorySince(Long userId, Instant since);
}
