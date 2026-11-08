package com.mathtutor.common.auth.domain;

import java.time.Instant;

/**
 * Evento publicado cuando un usuario se registra exitosamente.
 */
public record UserRegisteredEvent(
        Long userId,
        String email,
        Instant occurredAt
) implements DomainEvent {

    public UserRegisteredEvent(Long userId, String email) {
        this(userId, email, Instant.now());
    }
}