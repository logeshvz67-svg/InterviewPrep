package com.codequest.repository;

import com.codequest.entity.Lesson;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    @EntityGraph(attributePaths = "companyCodes")
    Optional<Lesson> findBySlug(String slug);

    @EntityGraph(attributePaths = "companyCodes")
    List<Lesson> findByWorldCodeOrderBySectionAscSortOrderAsc(String worldCode);

    @EntityGraph(attributePaths = "companyCodes")
    List<Lesson> findAllByOrderByWorldCodeAscSectionAscSortOrderAsc();

    Optional<Lesson> findByTopicId(Long topicId);
}
