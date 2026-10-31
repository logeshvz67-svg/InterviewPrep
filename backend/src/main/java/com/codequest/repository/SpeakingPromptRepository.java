package com.codequest.repository;

import com.codequest.entity.SpeakingPrompt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpeakingPromptRepository extends JpaRepository<SpeakingPrompt, Long> {
    Optional<SpeakingPrompt> findByExtId(String extId);
    List<SpeakingPrompt> findAllByOrderByIdAsc();
}
