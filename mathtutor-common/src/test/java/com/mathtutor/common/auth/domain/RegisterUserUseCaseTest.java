package com.mathtutor.common.auth.domain;

import com.mathtutor.common.auth.port.AuditLogPort;
import com.mathtutor.common.auth.port.UserRepositoryPort;
import com.mathtutor.common.port.PasswordEncoderPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests para RegisterUserUseCase (TDD - Red phase primero).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RegisterUserUseCase")
class RegisterUserUseCaseTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private PasswordEncoderPort passwordEncoderPort;

    @Mock
    private AuditLogPort auditLogPort;

    private RegisterUserUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RegisterUserUseCase(userRepositoryPort, passwordEncoderPort, auditLogPort);
    }

    @Nested
    @DisplayName("Registro exitoso")
    class SuccessfulRegistration {

        @Test
        @DisplayName("Debe registrar usuario con email válido y contraseña fuerte")
        void shouldRegisterUserWithValidEmailAndStrongPassword() {
            // Given
            String email = "test@example.com";
            String rawPassword = "StrongPass123!";
            String encodedPassword = "argon2id$encoded$hash";

            when(userRepositoryPort.existsByEmail(email)).thenReturn(false);
            when(passwordEncoderPort.encode(rawPassword)).thenReturn(encodedPassword);
            when(userRepositoryPort.save(any())).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                // Simular asignación de ID por BD
                return User.withId(1L, user.email(), user.passwordHash(), user.role(), user.active(), user.anonymizedAt(), user.createdAt());
            });

            // When
            Result<Long> result = useCase.execute(email, rawPassword, User.Role.ALUMNO);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getOrThrow()).isEqualTo(1L);

            verify(userRepositoryPort).existsByEmail(email);
            verify(passwordEncoderPort).encode(rawPassword);
            verify(userRepositoryPort).save(argThat(user ->
                    user.email().equals(email) &&
                    user.passwordHash().equals(encodedPassword) &&
                    user.role() == User.Role.ALUMNO &&
                    user.active()
            ));
            verify(auditLogPort).save(argThat(event ->
                    event instanceof UserRegisteredEvent &&
                    ((UserRegisteredEvent) event).email().equals(email) &&
                    ((UserRegisteredEvent) event).userId() == 1L
            ));
        }

        @Test
        @DisplayName("Debe permitir registrar PROFESOR y ADMIN")
        void shouldAllowRegisteringProfessorAndAdmin() {
            // Given
            when(userRepositoryPort.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoderPort.encode(anyString())).thenReturn("encoded");
            when(userRepositoryPort.save(any())).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                return User.withId(1L, user.email(), user.passwordHash(), user.role(), user.active(), user.anonymizedAt(), user.createdAt());
            });

            // When & Then - PROFESOR
            Result<Long> profResult = useCase.execute("prof@test.com", "StrongPass123!", User.Role.PROFESOR);
            assertThat(profResult.isSuccess()).isTrue();

            // When & Then - ADMIN
            Result<Long> adminResult = useCase.execute("admin@test.com", "StrongPass123!", User.Role.ADMIN);
            assertThat(adminResult.isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("Validación de email")
    class EmailValidation {

        @Test
        @DisplayName("Debe fallar con error genérico si email ya existe (no enumeración)")
        void shouldFailWithGenericErrorIfEmailAlreadyExists() {
            // Given
            String email = "existing@test.com";
            // Password must be 12+ chars to pass length check first
            String password = "StrongPass123!";
            when(userRepositoryPort.existsByEmail(email)).thenReturn(true);

            // When
            Result<Long> result = useCase.execute(email, password, User.Role.ALUMNO);

            // Then - Generic error to prevent user enumeration (ASVS L1)
            assertThat(result.isFailure()).isTrue();
            assertThat(result).isEqualTo(Result.fail(Result.ErrorCode.VALIDATION_ERROR, "Credenciales inválidas"));

            verify(userRepositoryPort).existsByEmail(email);
            verifyNoInteractions(passwordEncoderPort);
            verify(userRepositoryPort, never()).save(any());
            verify(auditLogPort).save(argThat(event ->
                    event instanceof UserRegistrationFailedEvent &&
                    ((UserRegistrationFailedEvent) event).email().equals(email) &&
                    ((UserRegistrationFailedEvent) event).reason().equals("EMAIL_ALREADY_EXISTS")
            ));
        }

        @Test
        @DisplayName("Debe fallar si email es null")
        void shouldFailIfEmailIsNull() {
            // When
            Result<Long> result = useCase.execute(null, "Pass123!", User.Role.ALUMNO);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result).isEqualTo(Result.fail(Result.ErrorCode.VALIDATION_ERROR, "Email es obligatorio"));
        }

        @Test
        @DisplayName("Debe fallar si email está vacío")
        void shouldFailIfEmailIsEmpty() {
            // When
            Result<Long> result = useCase.execute("", "Pass123!", User.Role.ALUMNO);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result).isEqualTo(Result.fail(Result.ErrorCode.VALIDATION_ERROR, "Email es obligatorio"));
        }

        @Test
        @DisplayName("Debe fallar si email tiene formato inválido")
        void shouldFailIfEmailFormatInvalid() {
            // When
            Result<Long> result = useCase.execute("invalid-email", "Pass123!", User.Role.ALUMNO);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result).isEqualTo(Result.fail(Result.ErrorCode.VALIDATION_ERROR, "Formato de email inválido"));
        }

        @Test
        @DisplayName("Debe fallar si email es solo espacios")
        void shouldFailIfEmailIsBlank() {
            // When
            Result<Long> result = useCase.execute("   ", "Pass123!", User.Role.ALUMNO);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result).isEqualTo(Result.fail(Result.ErrorCode.VALIDATION_ERROR, "Email es obligatorio"));
        }
    }

    @Nested
    @DisplayName("Validación de contraseña")
    class PasswordValidation {

        @Test
        @DisplayName("Debe fallar si contraseña es null")
        void shouldFailIfPasswordIsNull() {
            // When
            Result<Long> result = useCase.execute("test@test.com", null, User.Role.ALUMNO);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result).isEqualTo(Result.fail(Result.ErrorCode.VALIDATION_ERROR, "Contraseña es obligatoria"));
        }

        @Test
        @DisplayName("Debe fallar si contraseña tiene menos de 12 caracteres")
        void shouldFailIfPasswordTooShort() {
            // When
            Result<Long> result = useCase.execute("test@test.com", "Pass1!", User.Role.ALUMNO);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result).isEqualTo(Result.fail(Result.ErrorCode.VALIDATION_ERROR, "La contraseña debe tener al menos 12 caracteres"));
        }

        @Test
        @DisplayName("Debe fallar si contraseña no tiene mayúscula")
        void shouldFailIfPasswordNoUppercase() {
            // When - 12+ chars but no uppercase
            Result<Long> result = useCase.execute("test@test.com", "password12345!", User.Role.ALUMNO);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result).isEqualTo(Result.fail(Result.ErrorCode.VALIDATION_ERROR, "La contraseña debe contener al menos una mayúscula"));
        }

        @Test
        @DisplayName("Debe fallar si contraseña no tiene minúscula")
        void shouldFailIfPasswordNoLowercase() {
            // When - 12+ chars but no lowercase
            Result<Long> result = useCase.execute("test@test.com", "PASSWORD12345!", User.Role.ALUMNO);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result).isEqualTo(Result.fail(Result.ErrorCode.VALIDATION_ERROR, "La contraseña debe contener al menos una minúscula"));
        }

        @Test
        @DisplayName("Debe fallar si contraseña no tiene dígito")
        void shouldFailIfPasswordNoDigit() {
            // When - 12+ chars but no digit
            Result<Long> result = useCase.execute("test@test.com", "PasswordPass!", User.Role.ALUMNO);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result).isEqualTo(Result.fail(Result.ErrorCode.VALIDATION_ERROR, "La contraseña debe contener al menos un dígito"));
        }

        @Test
        @DisplayName("Debe fallar si contraseña no tiene carácter especial")
        void shouldFailIfPasswordNoSpecialChar() {
            // When - 12+ chars but no special char
            Result<Long> result = useCase.execute("test@test.com", "Password123456", User.Role.ALUMNO);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result).isEqualTo(Result.fail(Result.ErrorCode.VALIDATION_ERROR, "La contraseña debe contener al menos un carácter especial"));
        }

        @Test
        @DisplayName("Debe aceptar contraseña con todos los requisitos")
        void shouldAcceptPasswordWithAllRequirements() {
            // Given
            when(userRepositoryPort.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoderPort.encode(anyString())).thenReturn("encoded");
            when(userRepositoryPort.save(any())).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                return User.withId(1L, user.email(), user.passwordHash(), user.role(), user.active(), user.anonymizedAt(), user.createdAt());
            });

            // When - varios formatos válidos (mínimo 12 chars ASVS L1)
            String[] validPasswords = {
                "StrongPass123!",
                "Str0ng!Passw0rd",
                "My$ecure1Pass",
                "Aa1!bcdefghi"
            };

            for (int i = 0; i < validPasswords.length; i++) {
                String pwd = validPasswords[i];
                Result<Long> result = useCase.execute("test" + i + "@test.com", pwd, User.Role.ALUMNO);
                assertThat(result.isSuccess()).as("Password: %s", pwd).isTrue();
            }
        }
    }

    @Nested
    @DisplayName("Validación de rol")
    class RoleValidation {

        @Test
        @DisplayName("Debe fallar si rol es null")
        void shouldFailIfRoleIsNull() {
            // When
            Result<Long> result = useCase.execute("test@test.com", "StrongPass123!", null);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result).isEqualTo(Result.fail(Result.ErrorCode.VALIDATION_ERROR, "Rol es obligatorio"));
        }
    }

    @Nested
    @DisplayName("Manejo de errores")
    class ErrorHandling {

        @Test
        @DisplayName("Debe fallar si repositorio lanza excepción al guardar")
        void shouldFailIfRepositoryThrowsOnSave() {
            // Given
            when(userRepositoryPort.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoderPort.encode(anyString())).thenReturn("encoded");
            when(userRepositoryPort.save(any())).thenThrow(new RuntimeException("DB error"));

            // When
            Result<Long> result = useCase.execute("test@test.com", "StrongPass123!", User.Role.ALUMNO);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result).isEqualTo(Result.fail(Result.ErrorCode.INTERNAL_ERROR, "Error interno del servidor"));
        }
    }
}