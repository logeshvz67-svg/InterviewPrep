package com.codequest.repository;

import com.codequest.entity.MistakeCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MistakeCategoryRepository extends JpaRepository<MistakeCategory, Long> {
    Optional<MistakeCategory> findByCode(String code);
    boolean existsByCode(String code);
}
