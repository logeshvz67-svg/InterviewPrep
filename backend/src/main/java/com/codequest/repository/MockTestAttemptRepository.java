package com.codequest.repository;

import com.codequest.entity.MockTestAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MockTestAttemptRepository extends JpaRepository<MockTestAttempt, Long> {
    Optional<MockTestAttempt> findByMockTestId(Long mockTestId);
    List<MockTestAttempt> findTop3ByUserIdOrderBySubmittedAtDesc(Long userId);
}
