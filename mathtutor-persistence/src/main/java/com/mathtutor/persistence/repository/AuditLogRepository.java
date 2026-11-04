package com.mathtutor.persistence.repository;

import com.mathtutor.persistence.entity.AuditLog;
import com.mathtutor.persistence.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repositorio para entidad AuditLog.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByActor(User actor);

    List<AuditLog> findByAction(String action);

    List<AuditLog> findByTargetTypeAndTargetId(String targetType, Long targetId);

    List<AuditLog> findByCreatedAtBetween(Instant start, Instant end);

    Page<AuditLog> findAll(Pageable pageable);
}