package com.mathtutor.common.auth.domain;

import java.time.Instant;

/**
 * Evento publicado cuando falla un intento de registro.
 */
public record UserRegistrationFailedEvent(
        String email,
        String reason,
        Instant occurredAt
) implements DomainEvent {

    public UserRegistrationFailedEvent(String email, String reason) {
        this(email, reason, Instant.now());
    }
}