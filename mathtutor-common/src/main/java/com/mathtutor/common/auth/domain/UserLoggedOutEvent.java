package com.mathtutor.common.auth.domain;

import java.time.Instant;

/**
 * Evento publicado cuando un usuario cierra sesión.
 */
public record UserLoggedOutEvent(
        Long userId,
        String email,
        Instant occurredAt
) implements DomainEvent {

    public UserLoggedOutEvent(Long userId, String email) {
        this(userId, email, Instant.now());
    }
}