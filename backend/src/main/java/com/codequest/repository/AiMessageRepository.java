package com.codequest.repository;

import com.codequest.entity.AiMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiMessageRepository extends JpaRepository<AiMessage, Long> {
    List<AiMessage> findByConversationIdOrderByIdAsc(Long conversationId);
    List<AiMessage> findByConversationIdOrderByIdDesc(Long conversationId, Pageable pageable);
    Optional<AiMessage> findByIdAndUserId(Long id, Long userId);
}
