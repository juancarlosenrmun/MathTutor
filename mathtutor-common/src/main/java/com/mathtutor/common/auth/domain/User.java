package com.mathtutor.common.auth.domain;

import java.time.Instant;

/**
 * Entidad de dominio para Usuario.
 * Separada de la entidad JPA para mantener arquitectura hexagonal.
 * Solo incluye campos relevantes para autenticación.
 */
public record User(
        Long id,
        String email,
        String passwordHash,
        Role role,
        boolean active,
        Instant anonymizedAt,
        Instant createdAt
) {

    public enum Role {
        ALUMNO,
        PROFESOR,
        ADMIN
    }

    /**
     * Crea un nuevo usuario sin ID (para persistencia).
     */
    public static User create(String email, String passwordHash, Role role) {
        return new User(null, email, passwordHash, role, true, null, Instant.now());
    }

    /**
     * Crea un usuario con ID (reconstruido desde BD).
     */
    public static User withId(Long id, String email, String passwordHash, Role role, boolean active,
                              Instant anonymizedAt, Instant createdAt) {
        return new User(id, email, passwordHash, role, active, anonymizedAt, createdAt);
    }
}