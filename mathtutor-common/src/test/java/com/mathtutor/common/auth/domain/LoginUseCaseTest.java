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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests para LoginUseCase (TDD - Red phase primero).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoginUseCase")
class LoginUseCaseTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private PasswordEncoderPort passwordEncoderPort;

    @Mock
    private AuditLogPort auditLogPort;

    private LoginUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new LoginUseCase(userRepositoryPort, passwordEncoderPort, auditLogPort);
    }

    @Nested
    @DisplayName("Login exitoso")
    class SuccessfulLogin {

        @Test
        @DisplayName("Debe autenticar usuario con credenciales válidas")
        void shouldAuthenticateWithValidCredentials() {
            // Given
            String email = "test@example.com";
            String password = "Pass123!";
            String ipAddress = "192.168.1.1";
            String encodedPassword = "argon2id$encoded$hash";
            User user = User.withId(1L, email, encodedPassword, User.Role.ALUMNO, true, null, java.time.Instant.now());

            when(userRepositoryPort.findActiveByEmail(email)).thenReturn(Optional.of(user));
            when(passwordEncoderPort.matches(password, encodedPassword)).thenReturn(true);

            // When
            Result<LoginUseCase.LoginResult> result = useCase.execute(email, password, ipAddress);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getOrThrow().userId()).isEqualTo(1L);
            assertThat(result.getOrThrow().role()).isEqualTo(User.Role.ALUMNO);

            verify(userRepositoryPort).findActiveByEmail(email);
            verify(passwordEncoderPort).matches(password, encodedPassword);
            verify(auditLogPort).save(argThat(event ->
                    event instanceof UserLoggedInEvent &&
                    ((UserLoggedInEvent) event).userId() == 1L &&
                    ((UserLoggedInEvent) event).email().equals(email) &&
                    ((UserLoggedInEvent) event).ipAddress().equals(ipAddress)
            ));
        }

        @Test
        @DisplayName("Debe autenticar PROFESOR y ADMIN")
        void shouldAuthenticateProfessorAndAdmin() {
            // Given
            String password = "Pass123!";
            String encodedPassword = "encoded";
            String ip = "10.0.0.1";

            User prof = User.withId(2L, "prof@test.com", encodedPassword, User.Role.PROFESOR, true, null, java.time.Instant.now());
            User admin = User.withId(3L, "admin@test.com", encodedPassword, User.Role.ADMIN, true, null, java.time.Instant.now());

            when(userRepositoryPort.findActiveByEmail("prof@test.com")).thenReturn(Optional.of(prof));
            when(userRepositoryPort.findActiveByEmail("admin@test.com")).thenReturn(Optional.of(admin));
            when(passwordEncoderPort.matches(password, encodedPassword)).thenReturn(true);

            // When & Then
            Result<LoginUseCase.LoginResult> profResult = useCase.execute("prof@test.com", password, ip);
            assertThat(profResult.isSuccess()).isTrue();
            assertThat(profResult.getOrThrow().role()).isEqualTo(User.Role.PROFESOR);

            Result<LoginUseCase.LoginResult> adminResult = useCase.execute("admin@test.com", password, ip);
            assertThat(adminResult.isSuccess()).isTrue();
            assertThat(adminResult.getOrThrow().role()).isEqualTo(User.Role.ADMIN);
        }

        @Test
        @DisplayName("Debe ser case-insensitive para email")
        void shouldBeCaseInsensitiveForEmail() {
            // Given
            String encodedPassword = "encoded";
            User user = User.withId(1L, "test@example.com", encodedPassword, User.Role.ALUMNO, true, null, java.time.Instant.now());

            when(userRepositoryPort.findActiveByEmail("test@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoderPort.matches(anyString(), anyString())).thenReturn(true);

            // When
            Result<LoginUseCase.LoginResult> result = useCase.execute("TEST@EXAMPLE.COM", "Pass123!", "1.2.3.4");

            // Then
            assertThat(result.isSuccess()).isTrue();
            verify(userRepositoryPort).findActiveByEmail("test@example.com");
        }

        @Test
        @DisplayName("Debe ignorar espacios en email")
        void shouldTrimEmail() {
            // Given
            String encodedPassword = "encoded";
            User user = User.withId(1L, "test@example.com", encodedPassword, User.Role.ALUMNO, true, null, java.time.Instant.now());

            when(userRepositoryPort.findActiveByEmail("test@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoderPort.matches(anyString(), anyString())).thenReturn(true);

            // When
            Result<LoginUseCase.LoginResult> result = useCase.execute("  test@example.com  ", "Pass123!", "1.2.3.4");

            // Then
            assertThat(result.isSuccess()).isTrue();
            verify(userRepositoryPort).findActiveByEmail("test@example.com");
        }
    }

    @Nested
    @DisplayName("Validación de entrada")
    class InputValidation {

        @Test
        @DisplayName("Debe fallar si email es null")
        void shouldFailIfEmailIsNull() {
            // When
            Result<LoginUseCase.LoginResult> result = useCase.execute(null, "Pass123!", "1.2.3.4");

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result).isEqualTo(Result.fail(Result.ErrorCode.VALIDATION_ERROR, "Email es obligatorio"));
        }

        @Test
        @DisplayName("Debe fallar si email está vacío")
        void shouldFailIfEmailIsEmpty() {
            // When
            Result<LoginUseCase.LoginResult> result = useCase.execute("", "Pass123!", "1.2.3.4");

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result).isEqualTo(Result.fail(Result.ErrorCode.VALIDATION_ERROR, "Email es obligatorio"));
        }

        @Test
        @DisplayName("Debe fallar si contraseña es null")
        void shouldFailIfPasswordIsNull() {
            // When
            Result<LoginUseCase.LoginResult> result = useCase.execute("test@test.com", null, "1.2.3.4");

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result).isEqualTo(Result.fail(Result.ErrorCode.VALIDATION_ERROR, "Contraseña es obligatoria"));
        }
    }

    @Nested
    @DisplayName("Credenciales inválidas")
    class InvalidCredentials {

        @Test
        @DisplayName("Debe fallar si usuario no existe")
        void shouldFailIfUserNotFound() {
            // Given
            when(userRepositoryPort.findActiveByEmail("none@test.com")).thenReturn(Optional.empty());

            // When
            Result<LoginUseCase.LoginResult> result = useCase.execute("none@test.com", "Pass123!", "1.2.3.4");

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result).isEqualTo(Result.fail(Result.ErrorCode.EMAIL_NOT_FOUND, "Credenciales inválidas"));

            verify(auditLogPort).save(argThat(event ->
                    event instanceof UserLoginFailedEvent &&
                    ((UserLoginFailedEvent) event).email().equals("none@test.com") &&
                    ((UserLoginFailedEvent) event).reason().equals("EMAIL_NOT_FOUND")
            ));
        }

        @Test
        @DisplayName("Debe fallar si usuario está inactivo (no encontrado por findActiveByEmail)")
        void shouldFailIfUserInactive() {
            // Given - findActiveByEmail no encuentra usuarios inactivos
            when(userRepositoryPort.findActiveByEmail("inactive@test.com")).thenReturn(Optional.empty());

            // When
            Result<LoginUseCase.LoginResult> result = useCase.execute("inactive@test.com", "Pass123!", "1.2.3.4");

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result).isEqualTo(Result.fail(Result.ErrorCode.EMAIL_NOT_FOUND, "Credenciales inválidas"));
        }

        @Test
        @DisplayName("Debe fallar si contraseña es incorrecta")
        void shouldFailIfPasswordIncorrect() {
            // Given
            String encodedPassword = "argon2id$encoded$hash";
            User user = User.withId(1L, "test@test.com", encodedPassword, User.Role.ALUMNO, true, null, java.time.Instant.now());

            when(userRepositoryPort.findActiveByEmail("test@test.com")).thenReturn(Optional.of(user));
            when(passwordEncoderPort.matches("WrongPass123!", encodedPassword)).thenReturn(false);

            // When
            Result<LoginUseCase.LoginResult> result = useCase.execute("test@test.com", "WrongPass123!", "1.2.3.4");

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result).isEqualTo(Result.fail(Result.ErrorCode.INVALID_PASSWORD, "Credenciales inválidas"));

            verify(auditLogPort).save(argThat(event ->
                    event instanceof UserLoginFailedEvent &&
                    ((UserLoginFailedEvent) event).email().equals("test@test.com") &&
                    ((UserLoginFailedEvent) event).reason().equals("INVALID_PASSWORD")
            ));
        }
    }
}