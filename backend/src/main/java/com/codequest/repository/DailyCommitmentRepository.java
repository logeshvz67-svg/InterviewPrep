package com.codequest.repository;

import com.codequest.entity.DailyCommitment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyCommitmentRepository extends JpaRepository<DailyCommitment, Long> {
    Optional<DailyCommitment> findByUserIdAndForDate(Long userId, LocalDate forDate);
}
