package com.mathtutor.persistence.repository;

import com.mathtutor.persistence.entity.ConversationTurn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationTurnRepository extends JpaRepository<ConversationTurn, Long> {
    Optional<ConversationTurn> findBySessionIdAndRequestId(Long sessionId, UUID requestId);
    List<ConversationTurn> findBySessionIdAndStatus(Long sessionId, String status);
    List<ConversationTurn> findBySessionId(Long sessionId, Pageable page);
    List<ConversationTurn> findBySessionIdAndStatus(Long sessionId, String status, Pageable page);
}
