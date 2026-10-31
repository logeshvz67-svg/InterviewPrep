package com.codequest.repository;

import com.codequest.entity.MockTest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MockTestRepository extends JpaRepository<MockTest, Long> {
    Optional<MockTest> findByIdAndUserId(Long id, Long userId);
    List<MockTest> findTop30ByUserIdOrderByStartedAtDesc(Long userId);
}
