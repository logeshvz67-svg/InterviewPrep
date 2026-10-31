package com.codequest.repository;

import com.codequest.entity.SpringTopicProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringTopicProgressRepository extends JpaRepository<SpringTopicProgress, Long> {
    List<SpringTopicProgress> findByUserId(Long userId);
    Optional<SpringTopicProgress> findByUserIdAndTopicId(Long userId, Long topicId);
}
