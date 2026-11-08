package com.mathtutor.common.auth.domain;

import com.mathtutor.common.auth.port.AuditLogPort;
import com.mathtutor.common.auth.port.UserRepositoryPort;
import com.mathtutor.common.port.PasswordEncoderPort;

import java.util.Optional;

/**
 * Caso de uso para inicio de sesión.
 * Busca usuario activo, verifica contraseña y publica evento de auditoría.
 */
public class LoginUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final AuditLogPort auditLogPort;

    public LoginUseCase(UserRepositoryPort userRepositoryPort,
                        PasswordEncoderPort passwordEncoderPort,
                        AuditLogPort auditLogPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordEncoderPort = passwordEncoderPort;
        this.auditLogPort = auditLogPort;
    }

    /**
     * Ejecuta el inicio de sesión.
     *
     * @param email    email del usuario
     * @param password contraseña en texto plano
     * @param ipAddress dirección IP del cliente (para auditoría)
     * @return Result con LoginResult (userId, role) o error
     */
    public Result<LoginResult> execute(String email, String password, String ipAddress) {
        // Validaciones básicas
        if (email == null || email.trim().isEmpty()) {
            return Result.fail(Result.ErrorCode.VALIDATION_ERROR, "Email es obligatorio");
        }
        if (password == null) {
            return Result.fail(Result.ErrorCode.VALIDATION_ERROR, "Contraseña es obligatoria");
        }

        String normalizedEmail = email.trim().toLowerCase();

        // Buscar usuario activo
        Optional<User> userOpt = userRepositoryPort.findActiveByEmail(normalizedEmail);
        if (userOpt.isEmpty()) {
            auditLogPort.save(new UserLoginFailedEvent(normalizedEmail, "EMAIL_NOT_FOUND", ipAddress));
            return Result.fail(Result.ErrorCode.EMAIL_NOT_FOUND, "Credenciales inválidas");
        }

        User user = userOpt.get();

        // Verificar contraseña
        if (!passwordEncoderPort.matches(password, user.passwordHash())) {
            auditLogPort.save(new UserLoginFailedEvent(normalizedEmail, "INVALID_PASSWORD", ipAddress));
            return Result.fail(Result.ErrorCode.INVALID_PASSWORD, "Credenciales inválidas");
        }

        // Éxito - publicar evento
        auditLogPort.save(new UserLoggedInEvent(user.id(), normalizedEmail, ipAddress));

        return Result.ok(new LoginResult(user.id(), user.role()));
    }

    /**
     * Resultado de login exitoso.
     */
    public record LoginResult(Long userId, User.Role role) {}
}