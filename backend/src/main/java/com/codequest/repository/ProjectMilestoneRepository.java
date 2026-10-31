package com.codequest.repository;

import com.codequest.entity.ProjectMilestone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectMilestoneRepository extends JpaRepository<ProjectMilestone, Long> {
    List<ProjectMilestone> findByProjectIdOrderBySortOrder(Long projectId);
    Optional<ProjectMilestone> findByIdAndUserId(Long id, Long userId);
}
