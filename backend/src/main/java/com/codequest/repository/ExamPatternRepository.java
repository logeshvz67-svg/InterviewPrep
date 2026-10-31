package com.codequest.repository;

import com.codequest.entity.ExamPatternConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamPatternRepository extends JpaRepository<ExamPatternConfig, Long> {
    List<ExamPatternConfig> findByUserIdOrderByCompanyCodeAscNameAsc(Long userId);
    Optional<ExamPatternConfig> findByIdAndUserId(Long id, Long userId);
}
