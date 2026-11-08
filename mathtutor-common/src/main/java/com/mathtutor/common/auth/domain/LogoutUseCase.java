package com.mathtutor.common.auth.domain;

import com.mathtutor.common.auth.port.AuditLogPort;

/**
 * Caso de uso para cierre de sesión.
 * Publica evento de auditoría.
 */
public class LogoutUseCase {

    private final AuditLogPort auditLogPort;

    public LogoutUseCase(AuditLogPort auditLogPort) {
        this.auditLogPort = auditLogPort;
    }

    /**
     * Ejecuta el cierre de sesión.
     *
     * @param userId ID del usuario
     * @param email  email del usuario
     * @return Result indicando éxito
     */
    public Result<Void> execute(Long userId, String email) {
        if (userId == null || email == null || email.trim().isEmpty()) {
            return Result.fail(Result.ErrorCode.VALIDATION_ERROR, "Usuario no válido");
        }

        auditLogPort.save(new UserLoggedOutEvent(userId, email.trim().toLowerCase()));
        return Result.ok(null);
    }
}