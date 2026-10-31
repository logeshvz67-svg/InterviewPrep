package com.codequest.repository;

import com.codequest.entity.AiConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {
    Optional<AiConversation> findByIdAndUserId(Long id, Long userId);
    Page<AiConversation> findByUserIdOrderByUpdatedAtDesc(Long userId, Pageable pageable);

    /** Full-text search over the messages of YOUR conversations (Postgres to_tsvector). */
    @Query(value = """
            select c.* from ai_conversations c where c.user_id = :userId and exists (
              select 1 from ai_messages m where m.conversation_id = c.id
              and (to_tsvector('english', m.content) @@ plainto_tsquery('english', :q) or c.title ilike concat('%', :q, '%')))
            order by c.updated_at desc""",
            countQuery = """
            select count(*) from ai_conversations c where c.user_id = :userId and exists (
              select 1 from ai_messages m where m.conversation_id = c.id
              and (to_tsvector('english', m.content) @@ plainto_tsquery('english', :q) or c.title ilike concat('%', :q, '%')))""",
            nativeQuery = true)
    Page<AiConversation> search(Long userId, String q, Pageable pageable);
}
