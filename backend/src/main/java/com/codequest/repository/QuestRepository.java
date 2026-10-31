package com.codequest.repository;

import com.codequest.entity.Quest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuestRepository extends JpaRepository<Quest, Long> {
    Optional<Quest> findByCode(String code);
    java.util.List<Quest> findByKindOrderById(String kind);
}
