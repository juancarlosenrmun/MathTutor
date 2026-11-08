package com.mathtutor.common.auth.domain;

import java.time.Instant;

/**
 * Evento publicado cuando falla un intento de inicio de sesión.
 */
public record UserLoginFailedEvent(
        String email,
        String reason,
        String ipAddress,
        Instant occurredAt
) implements DomainEvent {

    public UserLoginFailedEvent(String email, String reason, String ipAddress) {
        this(email, reason, ipAddress, Instant.now());
    }
}