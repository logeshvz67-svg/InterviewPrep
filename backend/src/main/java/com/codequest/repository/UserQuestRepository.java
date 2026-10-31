package com.codequest.repository;

import com.codequest.entity.UserQuest;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface UserQuestRepository extends JpaRepository<UserQuest, Long> {
    @EntityGraph(attributePaths = "quest")
    List<UserQuest> findByUserIdAndForDateOrderById(Long userId, LocalDate forDate);

    java.util.Optional<UserQuest> findByUserIdAndQuestIdAndForDate(Long userId, Long questId, LocalDate forDate);
}
