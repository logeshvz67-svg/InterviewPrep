package com.codequest.repository;

import com.codequest.entity.JavaTopicProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JavaTopicProgressRepository extends JpaRepository<JavaTopicProgress, Long> {
    List<JavaTopicProgress> findByUserId(Long userId);
    Optional<JavaTopicProgress> findByUserIdAndTopicId(Long userId, Long topicId);
}
