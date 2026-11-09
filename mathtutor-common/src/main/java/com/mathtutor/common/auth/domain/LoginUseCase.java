package com.mathtutor.common.auth.domain;

import com.mathtutor.common.auth.port.AuditLogPort;
import com.mathtutor.common.auth.port.RateLimitPort;
import com.mathtutor.common.auth.port.UserRepositoryPort;
import com.mathtutor.common.port.PasswordEncoderPort;

import java.util.Optional;

/**
 * Caso de uso para inicio de sesión.
 * Busca usuario activo, verifica contraseña y publica evento de auditoría.
 * Incluye rate limiting para prevenir ataques de fuerza bruta (ASVS L1).
 */
public class LoginUseCase {

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final java.time.Duration LOGIN_WINDOW = java.time.Duration.ofMinutes(15);

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final AuditLogPort auditLogPort;
    private final RateLimitPort rateLimitPort;

    public LoginUseCase(UserRepositoryPort userRepositoryPort,
                        PasswordEncoderPort passwordEncoderPort,
                        AuditLogPort auditLogPort,
                        RateLimitPort rateLimitPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordEncoderPort = passwordEncoderPort;
        this.auditLogPort = auditLogPort;
        this.rateLimitPort = rateLimitPort;
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
        System.out.println("LoginUseCase.execute: email=" + email + ", ipAddress=" + ipAddress);
        // Validaciones básicas
        if (email == null || email.trim().isEmpty()) {
            return Result.fail(Result.ErrorCode.VALIDATION_ERROR, "Email es obligatorio");
        }
        if (password == null) {
            return Result.fail(Result.ErrorCode.VALIDATION_ERROR, "Contraseña es obligatoria");
        }

        String normalizedEmail = email.trim().toLowerCase();

        // Rate limiting por IP (ASVS L1: 5 intentos / 15 min)
        // Usamos IP para no revelar si el email existe (previene enumeración)
        String rateLimitKey = "login:ip:" + ipAddress;
        System.out.println("LoginUseCase.execute: rateLimitKey=" + rateLimitKey);
        if (!rateLimitPort.tryConsume(rateLimitKey, MAX_LOGIN_ATTEMPTS, LOGIN_WINDOW)) {
            auditLogPort.save(new UserLoginFailedEvent(normalizedEmail, "RATE_LIMIT_EXCEEDED", ipAddress));
            return Result.fail(Result.ErrorCode.TOO_MANY_REQUESTS, "Demasiados intentos de login. Intente de nuevo en 15 minutos.");
        }

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

        // Login exitoso - resetear rate limit para esta IP
        rateLimitPort.reset(rateLimitKey);

        // Éxito - publicar evento
        auditLogPort.save(new UserLoggedInEvent(user.id(), normalizedEmail, ipAddress));

        return Result.ok(new LoginResult(user.id(), user.role()));
    }

    /**
     * Resultado de login exitoso.
     */
    public record LoginResult(Long userId, User.Role role) {}
}