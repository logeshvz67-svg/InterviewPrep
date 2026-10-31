package com.codequest.repository;

import com.codequest.domain.XpAction;
import com.codequest.entity.XpEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface XpEventRepository extends JpaRepository<XpEvent, Long> {
    boolean existsByUserIdAndActionAndSourceKey(Long userId, XpAction action, String sourceKey);
    long countByUserIdAndActionAndLocalDate(Long userId, XpAction action, LocalDate localDate);
    Page<XpEvent> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("select coalesce(sum(e.amount), 0) from XpEvent e where e.userId = :userId and e.localDate = :date")
    int sumForDay(Long userId, LocalDate date);

    /** Projection: just (date, xp) pairs for charts. */
    interface DailyXp { LocalDate getDay(); Long getXp(); }

    @Query("select e.localDate as day, sum(e.amount) as xp from XpEvent e where e.userId = :userId and e.localDate >= :from group by e.localDate order by e.localDate")
    List<DailyXp> dailyTotals(Long userId, LocalDate from);
}
