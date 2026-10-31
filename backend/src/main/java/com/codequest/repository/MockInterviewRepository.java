package com.codequest.repository;

import com.codequest.entity.MockInterview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MockInterviewRepository extends JpaRepository<MockInterview, Long> {
    Optional<MockInterview> findByIdAndUserId(Long id, Long userId);
    List<MockInterview> findTop20ByUserIdOrderByStartedAtDesc(Long userId);
    List<MockInterview> findTop3ByUserIdAndStatusOrderByStartedAtDesc(Long userId, String status);
}
