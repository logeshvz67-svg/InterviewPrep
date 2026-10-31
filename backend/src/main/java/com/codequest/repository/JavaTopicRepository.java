package com.codequest.repository;

import com.codequest.entity.JavaTopic;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JavaTopicRepository extends JpaRepository<JavaTopic, Long> {
    /**
     * @EntityGraph loads the lazy companyCodes in the SAME query (a join).
     * Without it, reading tags for 78 topics would fire 1 + 78 queries: the "N+1 problem".
     */
    @EntityGraph(attributePaths = "companyCodes")
    List<JavaTopic> findAllByOrderBySectionAscSortOrderAsc();
}
