package com.codequest.repository;

import com.codequest.entity.Pattern;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PatternRepository extends JpaRepository<Pattern, Long> {
    List<Pattern> findAllByOrderBySortOrder();
    List<Pattern> findByCodeIn(Collection<String> codes);
}
