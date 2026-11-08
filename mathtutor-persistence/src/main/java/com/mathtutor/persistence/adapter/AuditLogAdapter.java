package com.mathtutor.persistence.adapter;

import com.mathtutor.common.auth.domain.DomainEvent;
import com.mathtutor.common.auth.domain.UserLoggedInEvent;
import com.mathtutor.common.auth.domain.UserLoggedOutEvent;
import com.mathtutor.common.auth.domain.UserLoginFailedEvent;
import com.mathtutor.common.auth.domain.UserRegisteredEvent;
import com.mathtutor.common.auth.domain.UserRegistrationFailedEvent;
import com.mathtutor.common.auth.port.AuditLogPort;
import com.mathtutor.persistence.entity.AuditLog;
import com.mathtutor.persistence.entity.User;
import com.mathtutor.persistence.repository.AuditLogRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Adaptador que implementa AuditLogPort usando Spring Data JPA.
 * Persiste eventos de dominio como logs de auditoría.
 */
@Component
public class AuditLogAdapter implements AuditLogPort {

    private final AuditLogRepository auditLogRepository;

    public AuditLogAdapter(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional
    public void save(DomainEvent event) {
        AuditLog auditLog = new AuditLog();
        auditLog.setAction(event.getClass().getSimpleName());
        auditLog.setCreatedAt(event.occurredAt());

        if (event instanceof UserRegisteredEvent registeredEvent) {
            auditLog.setTargetType("User");
            auditLog.setTargetId(registeredEvent.userId());
            // actor se establecerá si hay usuario autenticado en el contexto
        } else if (event instanceof UserRegistrationFailedEvent failedEvent) {
            auditLog.setTargetType("UserRegistration");
            auditLog.setTargetId(null);
        } else if (event instanceof UserLoggedInEvent loggedInEvent) {
            auditLog.setTargetType("User");
            auditLog.setTargetId(loggedInEvent.userId());
        } else if (event instanceof UserLoggedOutEvent loggedOutEvent) {
            auditLog.setTargetType("User");
            auditLog.setTargetId(loggedOutEvent.userId());
        } else if (event instanceof UserLoginFailedEvent loginFailedEvent) {
            auditLog.setTargetType("UserLogin");
            auditLog.setTargetId(null);
        }

        auditLogRepository.save(auditLog);
    }
}