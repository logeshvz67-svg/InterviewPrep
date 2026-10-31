package com.codequest.repository;

import com.codequest.entity.ActivityDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ActivityDayRepository extends JpaRepository<ActivityDay, Long> {
    boolean existsByUserIdAndLocalDate(Long userId, LocalDate localDate);
    List<ActivityDay> findByUserIdAndLocalDateGreaterThanEqualOrderByLocalDate(Long userId, LocalDate from);
}
