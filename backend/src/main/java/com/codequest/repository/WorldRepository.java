package com.codequest.repository;

import com.codequest.entity.World;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorldRepository extends JpaRepository<World, Long> {
    List<World> findAllByOrderBySortOrder();
}
