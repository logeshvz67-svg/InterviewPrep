package com.codequest.repository;

import com.codequest.entity.ProblemBankItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProblemBankRepository extends JpaRepository<ProblemBankItem, Long> {
    Optional<ProblemBankItem> findBySlug(String slug);
    List<ProblemBankItem> findAllByOrderBySortOrder();
}
