package com.codequest.repository;

import com.codequest.entity.InterviewDiaryEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterviewDiaryRepository extends JpaRepository<InterviewDiaryEntry, Long> {
    List<InterviewDiaryEntry> findByUserIdOrderByInterviewDateDescIdDesc(Long userId);
    Optional<InterviewDiaryEntry> findByIdAndUserId(Long id, Long userId);
}
