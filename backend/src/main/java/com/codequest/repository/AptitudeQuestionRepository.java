package com.codequest.repository;

import com.codequest.entity.AptitudeQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AptitudeQuestionRepository extends JpaRepository<AptitudeQuestion, Long> {
    Optional<AptitudeQuestion> findByExtId(String extId);
    List<AptitudeQuestion> findAllByOrderByIdAsc();
}
