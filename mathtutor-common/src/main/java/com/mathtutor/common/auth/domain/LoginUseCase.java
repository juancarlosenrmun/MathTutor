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
 * <p>
 * Rate limiting dual:
 * <ul>
 *   <li>IP-based: previene ataques distribuidos (botnets)</li>
 *   <li>Email-based: previene credential stuffing contra cuentas específicas</li>
 * </ul>
 * Ambos usan mismo límite: 5 intentos / 15 min (ASVS L1).
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
     * @param ipAddress dirección IP del cliente (para auditoría y rate limiting IP)
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
        String ipRateLimitKey = "login:ip:" + ipAddress;
        System.out.println("LoginUseCase.execute: ipRateLimitKey=" + ipRateLimitKey);
        if (!rateLimitPort.tryConsume(ipRateLimitKey, MAX_LOGIN_ATTEMPTS, LOGIN_WINDOW)) {
            auditLogPort.save(new UserLoginFailedEvent(normalizedEmail, "RATE_LIMIT_EXCEEDED", ipAddress));
            return Result.fail(Result.ErrorCode.TOO_MANY_REQUESTS, "Demasiados intentos de login. Intente de nuevo en 15 minutos.");
        }

        // Rate limiting por email (ASVS L1: 5 intentos / 15 min)
        // Previene credential stuffing contra cuentas específicas
        String emailRateLimitKey = "login:email:" + normalizedEmail;
        System.out.println("LoginUseCase.execute: emailRateLimitKey=" + emailRateLimitKey);
        if (!rateLimitPort.tryConsume(emailRateLimitKey, MAX_LOGIN_ATTEMPTS, LOGIN_WINDOW)) {
            auditLogPort.save(new UserLoginFailedEvent(normalizedEmail, "RATE_LIMIT_EXCEEDED_EMAIL", ipAddress));
            return Result.fail(Result.ErrorCode.TOO_MANY_REQUESTS, "Demasiados intentos para esta cuenta. Intente de nuevo en 15 minutos.");
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

        // Login exitoso - resetear rate limits para esta IP y email
        rateLimitPort.reset(ipRateLimitKey);
        rateLimitPort.reset(emailRateLimitKey);

        // Éxito - publicar evento
        auditLogPort.save(new UserLoggedInEvent(user.id(), normalizedEmail, ipAddress));

        return Result.ok(new LoginResult(user.id(), user.role()));
    }

    /**
     * Resultado de login exitoso.
     */
    public record LoginResult(Long userId, User.Role role) {}
}