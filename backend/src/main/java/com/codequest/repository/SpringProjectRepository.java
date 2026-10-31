package com.codequest.repository;

import com.codequest.entity.SpringProject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringProjectRepository extends JpaRepository<SpringProject, Long> {
    List<SpringProject> findByUserIdOrderByIdAsc(Long userId);
    Optional<SpringProject> findByIdAndUserId(Long id, Long userId);
}
