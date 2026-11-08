package com.mathtutor.common.auth.domain;

import com.mathtutor.common.auth.port.AuditLogPort;
import com.mathtutor.common.auth.port.UserRepositoryPort;
import com.mathtutor.common.port.PasswordEncoderPort;

/**
 * Caso de uso para registro de usuarios.
 * Valida email, contraseña, verifica unicidad, hashea contraseña y persiste usuario.
 */
public class RegisterUserUseCase {

    private static final int MIN_PASSWORD_LENGTH = 8;
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
            return Result.fail(Result.ErrorCode.EMAIL_ALREADY_EXISTS, "El email ya está registrado");
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
        if (password == null) {
            return Result.fail(Result.ErrorCode.VALIDATION_ERROR, "Contraseña es obligatoria");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return Result.fail(Result.ErrorCode.VALIDATION_ERROR, "La contraseña debe tener al menos 8 caracteres");
        }
        if (!password.matches(".*[A-Z].*")) {
            return Result.fail(Result.ErrorCode.VALIDATION_ERROR, "La contraseña debe contener al menos una mayúscula");
        }
        if (!password.matches(".*[a-z].*")) {
            return Result.fail(Result.ErrorCode.VALIDATION_ERROR, "La contraseña debe contener al menos una minúscula");
        }
        if (!password.matches(".*\\d.*")) {
            return Result.fail(Result.ErrorCode.VALIDATION_ERROR, "La contraseña debe contener al menos un dígito");
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            return Result.fail(Result.ErrorCode.VALIDATION_ERROR, "La contraseña debe contener al menos un carácter especial");
        }

        // Validar rol
        if (role == null) {
            return Result.fail(Result.ErrorCode.VALIDATION_ERROR, "Rol es obligatorio");
        }

        return Result.ok(null);
    }
}