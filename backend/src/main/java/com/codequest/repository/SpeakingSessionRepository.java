package com.codequest.repository;

import com.codequest.entity.SpeakingSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpeakingSessionRepository extends JpaRepository<SpeakingSession, Long> {
    Page<SpeakingSession> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Optional<SpeakingSession> findByIdAndUserId(Long id, Long userId);
    List<SpeakingSession> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);
}
