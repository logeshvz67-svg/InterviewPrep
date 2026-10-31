package com.codequest.repository;

import com.codequest.entity.QuestionBankItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionBankRepository extends JpaRepository<QuestionBankItem, Long> {
    Optional<QuestionBankItem> findByExtId(String extId);
    List<QuestionBankItem> findAllByOrderByIdAsc();
}
