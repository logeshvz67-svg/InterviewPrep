package com.codequest.repository;

import com.codequest.entity.FocusSession;
import com.codequest.entity.GymEnums.FocusStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FocusSessionRepository extends JpaRepository<FocusSession, Long> {
    Optional<FocusSession> findFirstByUserIdAndStatusOrderByStartedAtDesc(Long userId, FocusStatus status);
    Optional<FocusSession> findByIdAndUserId(Long id, Long userId);
    long countByUserIdAndStatus(Long userId, FocusStatus status);
    long countByUserIdAndStatusAndLocalDateGreaterThanEqual(Long userId, FocusStatus status, LocalDate from);
    List<FocusSession> findByUserIdAndStatusAndLocalDateGreaterThanEqual(Long userId, FocusStatus status, LocalDate from);
}
