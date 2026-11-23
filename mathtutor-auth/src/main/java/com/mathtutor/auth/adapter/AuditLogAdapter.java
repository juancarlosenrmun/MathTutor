package com.mathtutor.auth.adapter;

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
import com.mathtutor.persistence.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador que implementa AuditLogPort usando Spring Data JPA.
 * Persiste eventos de dominio como logs de auditoría.
 */
@Component
public class AuditLogAdapter implements AuditLogPort {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository users;

    public AuditLogAdapter(AuditLogRepository auditLogRepository, UserRepository users) {
        this.auditLogRepository = auditLogRepository;
        this.users = users;
    }

    @Override
    @Transactional
    public void save(DomainEvent event) {
        AuditLog auditLog = new AuditLog();
        auditLog.setAction(event.getClass().getSimpleName());
        auditLog.setCreatedAt(event.occurredAt());

        if (event instanceof UserRegisteredEvent registeredEvent) {
            setUserTarget(auditLog, registeredEvent.userId());
        } else if (event instanceof UserRegistrationFailedEvent failedEvent) {
            auditLog.setTargetType("UserRegistration");
            auditLog.setTargetId(null);
        } else if (event instanceof UserLoggedInEvent loggedInEvent) {
            setUserTarget(auditLog, loggedInEvent.userId());
        } else if (event instanceof UserLoggedOutEvent loggedOutEvent) {
            setUserTarget(auditLog, loggedOutEvent.userId());
        } else if (event instanceof UserLoginFailedEvent loginFailedEvent) {
            auditLog.setTargetType("UserLogin");
            auditLog.setTargetId(null);
        }

        auditLogRepository.save(auditLog);
    }

    /** @brief Conserva el evento, incluso si la cuenta se suprimió antes de registrarlo. */
    private void setUserTarget(AuditLog entry, Long id) {
        entry.setTargetType("User");
        users.findByIdForUpdate(id).filter(user -> user.getAnonymizedAt() == null)
                .map(User::getId).ifPresent(entry::setTargetId);
    }
}
