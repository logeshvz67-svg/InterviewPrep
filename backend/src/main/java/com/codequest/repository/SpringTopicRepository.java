package com.codequest.repository;

import com.codequest.entity.SpringTopic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringTopicRepository extends JpaRepository<SpringTopic, Long> {
    Optional<SpringTopic> findByCode(String code);
    List<SpringTopic> findAllByOrderBySortOrder();
    List<SpringTopic> findAllByOrderByIdAsc();
}
