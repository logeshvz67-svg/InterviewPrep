package com.codequest.repository;

import com.codequest.entity.Streak;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface StreakRepository extends JpaRepository<Streak, Long> {
    Optional<Streak> findByUserId(Long userId);

    /** SELECT ... FOR UPDATE: two requests at the same moment can't both bump the streak. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Streak s where s.userId = :userId")
    Optional<Streak> findForUpdate(Long userId);
}
