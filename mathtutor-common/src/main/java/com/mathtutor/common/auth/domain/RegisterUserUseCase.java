package com.mathtutor.common.auth.domain;

import com.mathtutor.common.auth.port.AuditLogPort;
import com.mathtutor.common.auth.port.UserRepositoryPort;
import com.mathtutor.common.port.PasswordEncoderPort;

/**
 * Caso de uso para registro de usuarios.
 * Valida email, contraseña, verifica unicidad, hashea contraseña y persiste usuario.
 */
public class RegisterUserUseCase {

    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final AuditLogPort auditLogPort;

    public RegisterUserUseCase(UserRepositoryPort userRepositoryPort,
                               PasswordEncoderPort passwordEncoderPort,
                               AuditLogPort auditLogPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordEncoderPort = passwordEncoderPort;
        this.auditLogPort = auditLogPort;
    }

    /**
     * Ejecuta el registro de un usuario.
     *
     * @param email    email del usuario
     * @param password contraseña en texto plano
     * @param role     rol del usuario
     * @return Result con ID del usuario creado o error
     */
    public Result<Long> execute(String email, String password, User.Role role) {
        // Validaciones de entrada
        Result<Long> validationResult = validateInput(email, password, role);
        if (validationResult.isFailure()) {
            return validationResult;
        }

        // Normalizar email
        String normalizedEmail = email.trim().toLowerCase();

        // Verificar unicidad de email
        if (userRepositoryPort.existsByEmail(normalizedEmail)) {
            auditLogPort.save(new UserRegistrationFailedEvent(normalizedEmail, "EMAIL_ALREADY_EXISTS"));
            // Generic error to prevent user enumeration (ASVS L1)
            return Result.fail(Result.ErrorCode.VALIDATION_ERROR, "Credenciales inválidas");
        }

        // Hashear contraseña
        String encodedPassword;
        try {
            encodedPassword = passwordEncoderPort.encode(password);
        } catch (Exception e) {
            auditLogPort.save(new UserRegistrationFailedEvent(normalizedEmail, "PASSWORD_ENCODING_ERROR"));
            return Result.fail(Result.ErrorCode.INTERNAL_ERROR, "Error al procesar la contraseña");
        }

        // Crear y guardar usuario
        User user;
        try {
            user = User.create(normalizedEmail, encodedPassword, role);
            User savedUser = userRepositoryPort.save(user);
            Long userId = savedUser.id();

            // Publicar evento de éxito
            auditLogPort.save(new UserRegisteredEvent(userId, normalizedEmail));

            return Result.ok(userId);
        } catch (Exception e) {
            auditLogPort.save(new UserRegistrationFailedEvent(normalizedEmail, "PERSISTENCE_ERROR"));
            return Result.fail(Result.ErrorCode.INTERNAL_ERROR, "Error interno del servidor");
        }
    }

    private Result<Long> validateInput(String email, String password, User.Role role) {
        // Validar email
        if (email == null || email.trim().isEmpty()) {
            return Result.fail(Result.ErrorCode.VALIDATION_ERROR, "Email es obligatorio");
        }
        String trimmedEmail = email.trim();
        if (!trimmedEmail.matches(EMAIL_REGEX)) {
            return Result.fail(Result.ErrorCode.VALIDATION_ERROR, "Formato de email inválido");
        }

        // Validar contraseña
        var violation = PasswordPolicy.violation(password);
        if (violation.isPresent()) {
            return Result.fail(Result.ErrorCode.VALIDATION_ERROR, violation.get());
        }

        // Validar rol
        if (role == null) {
            return Result.fail(Result.ErrorCode.VALIDATION_ERROR, "Rol es obligatorio");
        }

        if (role != User.Role.ALUMNO) {
            // El rol solicitado no es una fuente de autoridad para el alta pública.
            return Result.fail(Result.ErrorCode.VALIDATION_ERROR, "El registro público solo permite el rol alumno");
        }
        return Result.ok(null);
    }
}
