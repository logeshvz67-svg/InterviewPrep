package com.codequest.repository;

import com.codequest.entity.DistractionEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DistractionRepository extends JpaRepository<DistractionEntry, Long> {
    Page<DistractionEntry> findByUserId(Long userId, Pageable pageable);
    List<DistractionEntry> findByUserIdAndLocalDateGreaterThanEqual(Long userId, LocalDate from);
}
