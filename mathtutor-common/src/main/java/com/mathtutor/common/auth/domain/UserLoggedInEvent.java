package com.mathtutor.common.auth.domain;

import java.time.Instant;

/**
 * Evento publicado cuando un usuario inicia sesión exitosamente.
 */
public record UserLoggedInEvent(
        Long userId,
        String email,
        Instant occurredAt,
        String ipAddress
) implements DomainEvent {

    public UserLoggedInEvent(Long userId, String email, String ipAddress) {
        this(userId, email, Instant.now(), ipAddress);
    }
}