package com.codequest.repository;

import com.codequest.entity.Roadmap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface RoadmapRepository extends JpaRepository<Roadmap, Long> {
    Optional<Roadmap> findFirstByUserIdAndActiveTrueOrderByIdDesc(Long userId);

    @Modifying
    @Query("update Roadmap r set r.active = false where r.userId = :userId and r.active = true")
    int deactivateAll(Long userId);
}
