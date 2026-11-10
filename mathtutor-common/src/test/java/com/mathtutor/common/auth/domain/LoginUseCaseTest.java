package com.mathtutor.common.auth.domain;

import com.mathtutor.common.auth.port.AuditLogPort;
import com.mathtutor.common.auth.port.RateLimitPort;
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
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
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

    @Mock
    private RateLimitPort rateLimitPort;

    private LoginUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new LoginUseCase(userRepositoryPort, passwordEncoderPort, auditLogPort, rateLimitPort);
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
            when(rateLimitPort.tryConsume(eq("login:ip:" + ipAddress), eq(5), any())).thenReturn(true);
            when(rateLimitPort.tryConsume(eq("login:email:" + email), eq(5), any())).thenReturn(true);

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
            when(rateLimitPort.tryConsume(eq("login:ip:" + ip), eq(5), any())).thenReturn(true);
            when(rateLimitPort.tryConsume(eq("login:email:prof@test.com"), eq(5), any())).thenReturn(true);
            when(rateLimitPort.tryConsume(eq("login:email:admin@test.com"), eq(5), any())).thenReturn(true);

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
            when(rateLimitPort.tryConsume(anyString(), eq(5), any())).thenReturn(true);

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
            when(rateLimitPort.tryConsume(anyString(), eq(5), any())).thenReturn(true);

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
            when(rateLimitPort.tryConsume(anyString(), eq(5), any())).thenReturn(true);

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
            when(rateLimitPort.tryConsume(anyString(), eq(5), any())).thenReturn(true);

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
            when(rateLimitPort.tryConsume(anyString(), eq(5), any())).thenReturn(true);

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

    @Nested
    @DisplayName("Rate Limiting (ASVS L1: 5 intentos / 15 min)")
    class RateLimiting {

        @Test
        @DisplayName("Debe permitir login cuando rate limit no se excede")
        void shouldAllowLoginWhenRateLimitNotExceeded() {
            // Given
            String encodedPassword = "argon2id$encoded$hash";
            User user = User.withId(1L, "test@test.com", encodedPassword, User.Role.ALUMNO, true, null, java.time.Instant.now());

            when(userRepositoryPort.findActiveByEmail("test@test.com")).thenReturn(Optional.of(user));
            when(passwordEncoderPort.matches("Pass123!", encodedPassword)).thenReturn(true);
            when(rateLimitPort.tryConsume(eq("login:ip:192.168.1.1"), eq(5), any())).thenReturn(true);
            when(rateLimitPort.tryConsume(eq("login:email:test@test.com"), eq(5), any())).thenReturn(true);

            // When
            Result<LoginUseCase.LoginResult> result = useCase.execute("test@test.com", "Pass123!", "192.168.1.1");

            // Then
            assertThat(result.isSuccess()).isTrue();
            verify(rateLimitPort).tryConsume("login:ip:192.168.1.1", 5, java.time.Duration.ofMinutes(15));
            verify(rateLimitPort).tryConsume("login:email:test@test.com", 5, java.time.Duration.ofMinutes(15));
            verify(rateLimitPort).reset("login:ip:192.168.1.1");
            verify(rateLimitPort).reset("login:email:test@test.com");
        }

        @Test
        @DisplayName("Debe bloquear login cuando rate limit se excede (429)")
        void shouldBlockLoginWhenRateLimitExceeded() {
            // Given - rate limit excedido
            when(rateLimitPort.tryConsume(eq("login:ip:192.168.1.1"), eq(5), any())).thenReturn(false);

            // When
            Result<LoginUseCase.LoginResult> result = useCase.execute("test@test.com", "Pass123!", "192.168.1.1");

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result).isInstanceOf(Result.Failure.class);
            Result.Failure<LoginUseCase.LoginResult> failure = (Result.Failure<LoginUseCase.LoginResult>) result;
            assertThat(failure.code()).isEqualTo(Result.ErrorCode.TOO_MANY_REQUESTS);
            assertThat(failure.message()).isEqualTo("Demasiados intentos de login. Intente de nuevo en 15 minutos.");

            verify(rateLimitPort).tryConsume("login:ip:192.168.1.1", 5, java.time.Duration.ofMinutes(15));
            verify(rateLimitPort, never()).reset(anyString());
            verify(auditLogPort).save(argThat(event ->
                    event instanceof UserLoginFailedEvent &&
                    ((UserLoginFailedEvent) event).email().equals("test@test.com") &&
                    ((UserLoginFailedEvent) event).reason().equals("RATE_LIMIT_EXCEEDED")
            ));
        }

        @Test
        @DisplayName("Debe aplicar rate limit antes de verificar usuario (previene enumeración)")
        void shouldApplyRateLimitBeforeUserCheck() {
            // Given - rate limit excedido, usuario no existe
            when(rateLimitPort.tryConsume(eq("login:ip:192.168.1.1"), eq(5), any())).thenReturn(false);

            // When
            Result<LoginUseCase.LoginResult> result = useCase.execute("nonexistent@test.com", "Pass123!", "192.168.1.1");

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result).isInstanceOf(Result.Failure.class);
            Result.Failure<LoginUseCase.LoginResult> failure = (Result.Failure<LoginUseCase.LoginResult>) result;
            assertThat(failure.code()).isEqualTo(Result.ErrorCode.TOO_MANY_REQUESTS);
            // No debe llamar a userRepositoryPort porque rate limit bloquea antes
            verify(userRepositoryPort, never()).findActiveByEmail(anyString());
            verify(passwordEncoderPort, never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("Debe usar IP y email para rate limit (dual protection)")
        void shouldUseBothIpAndEmailForRateLimit() {
            // Given
            String encodedPassword = "argon2id$encoded$hash";
            User user = User.withId(1L, "test@test.com", encodedPassword, User.Role.ALUMNO, true, null, java.time.Instant.now());

            when(userRepositoryPort.findActiveByEmail("test@test.com")).thenReturn(Optional.of(user));
            when(passwordEncoderPort.matches("Pass123!", encodedPassword)).thenReturn(true);
            when(rateLimitPort.tryConsume(eq("login:ip:192.168.1.1"), eq(5), any())).thenReturn(true);
            when(rateLimitPort.tryConsume(eq("login:email:test@test.com"), eq(5), any())).thenReturn(true);

            // When
            useCase.execute("test@test.com", "Pass123!", "192.168.1.1");

            // Then - Verificar que se llaman AMBOS rate limits (IP y email)
            verify(rateLimitPort).tryConsume("login:ip:192.168.1.1", 5, java.time.Duration.ofMinutes(15));
            verify(rateLimitPort).tryConsume("login:email:test@test.com", 5, java.time.Duration.ofMinutes(15));
        }

        @Test
        @DisplayName("Debe resetear rate limit tras login exitoso")
        void shouldResetRateLimitAfterSuccessfulLogin() {
            // Given
            String encodedPassword = "argon2id$encoded$hash";
            User user = User.withId(1L, "test@test.com", encodedPassword, User.Role.ALUMNO, true, null, java.time.Instant.now());

            when(userRepositoryPort.findActiveByEmail("test@test.com")).thenReturn(Optional.of(user));
            when(passwordEncoderPort.matches("Pass123!", encodedPassword)).thenReturn(true);
            when(rateLimitPort.tryConsume(eq("login:ip:192.168.1.1"), eq(5), any())).thenReturn(true);
            when(rateLimitPort.tryConsume(eq("login:email:test@test.com"), eq(5), any())).thenReturn(true);

            // When
            useCase.execute("test@test.com", "Pass123!", "192.168.1.1");

            // Then
            verify(rateLimitPort).reset("login:ip:192.168.1.1");
            verify(rateLimitPort).reset("login:email:test@test.com");
        }

        @Test
        @DisplayName("No debe resetear rate limit tras login fallido")
        void shouldNotResetRateLimitAfterFailedLogin() {
            // Given
            String encodedPassword = "argon2id$encoded$hash";
            User user = User.withId(1L, "test@test.com", encodedPassword, User.Role.ALUMNO, true, null, java.time.Instant.now());

            when(userRepositoryPort.findActiveByEmail("test@test.com")).thenReturn(Optional.of(user));
            when(passwordEncoderPort.matches("WrongPass123!", encodedPassword)).thenReturn(false);
            when(rateLimitPort.tryConsume(eq("login:ip:192.168.1.1"), eq(5), any())).thenReturn(true);
            when(rateLimitPort.tryConsume(eq("login:email:test@test.com"), eq(5), any())).thenReturn(true);

            // When
            useCase.execute("test@test.com", "WrongPass123!", "192.168.1.1");

            // Then
            verify(rateLimitPort, never()).reset(anyString());
        }
    }

    @Nested
    @DisplayName("Email-based Rate Limiting (Credential Stuffing Protection)")
    class EmailBasedRateLimiting {

        @Test
        @DisplayName("Debe permitir login cuando email rate limit no se excede")
        void shouldAllowLoginWhenEmailRateLimitNotExceeded() {
            // Given
            String encodedPassword = "argon2id$encoded$hash";
            User user = User.withId(1L, "test@test.com", encodedPassword, User.Role.ALUMNO, true, null, java.time.Instant.now());

            when(userRepositoryPort.findActiveByEmail("test@test.com")).thenReturn(Optional.of(user));
            when(passwordEncoderPort.matches("Pass123!", encodedPassword)).thenReturn(true);
            when(rateLimitPort.tryConsume(eq("login:ip:192.168.1.1"), eq(5), any())).thenReturn(true);
            when(rateLimitPort.tryConsume(eq("login:email:test@test.com"), eq(5), any())).thenReturn(true);

            // When
            Result<LoginUseCase.LoginResult> result = useCase.execute("test@test.com", "Pass123!", "192.168.1.1");

            // Then
            assertThat(result.isSuccess()).isTrue();
            verify(rateLimitPort).tryConsume("login:email:test@test.com", 5, java.time.Duration.ofMinutes(15));
            verify(rateLimitPort).reset("login:email:test@test.com");
        }

        @Test
        @DisplayName("Debe bloquear login cuando email rate limit se excede (credential stuffing)")
        void shouldBlockLoginWhenEmailRateLimitExceeded() {
            // Given - IP rate limit OK, pero email rate limit excedido
            when(rateLimitPort.tryConsume(eq("login:ip:192.168.1.1"), eq(5), any())).thenReturn(true);
            when(rateLimitPort.tryConsume(eq("login:email:test@test.com"), eq(5), any())).thenReturn(false);

            // When
            Result<LoginUseCase.LoginResult> result = useCase.execute("test@test.com", "Pass123!", "192.168.1.1");

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result).isInstanceOf(Result.Failure.class);
            Result.Failure<LoginUseCase.LoginResult> failure = (Result.Failure<LoginUseCase.LoginResult>) result;
            assertThat(failure.code()).isEqualTo(Result.ErrorCode.TOO_MANY_REQUESTS);
            assertThat(failure.message()).isEqualTo("Demasiados intentos para esta cuenta. Intente de nuevo en 15 minutos.");

            verify(rateLimitPort).tryConsume("login:ip:192.168.1.1", 5, java.time.Duration.ofMinutes(15));
            verify(rateLimitPort).tryConsume("login:email:test@test.com", 5, java.time.Duration.ofMinutes(15));
            verify(rateLimitPort, never()).reset(anyString());
            verify(auditLogPort).save(argThat(event ->
                    event instanceof UserLoginFailedEvent &&
                    ((UserLoginFailedEvent) event).email().equals("test@test.com") &&
                    ((UserLoginFailedEvent) event).reason().equals("RATE_LIMIT_EXCEEDED_EMAIL")
            ));
        }

        @Test
        @DisplayName("Debe aplicar email rate limit antes de verificar usuario (previene credential stuffing)")
        void shouldApplyEmailRateLimitBeforeUserCheck() {
            // Given - IP rate limit OK, email rate limit excedido, usuario no existe
            when(rateLimitPort.tryConsume(eq("login:ip:192.168.1.1"), eq(5), any())).thenReturn(true);
            when(rateLimitPort.tryConsume(eq("login:email:nonexistent@test.com"), eq(5), any())).thenReturn(false);

            // When
            Result<LoginUseCase.LoginResult> result = useCase.execute("nonexistent@test.com", "Pass123!", "192.168.1.1");

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result).isInstanceOf(Result.Failure.class);
            Result.Failure<LoginUseCase.LoginResult> failure = (Result.Failure<LoginUseCase.LoginResult>) result;
            assertThat(failure.code()).isEqualTo(Result.ErrorCode.TOO_MANY_REQUESTS);
            // No debe llamar a userRepositoryPort porque email rate limit bloquea antes
            verify(userRepositoryPort, never()).findActiveByEmail(anyString());
            verify(passwordEncoderPort, never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("Debe bloquear credential stuffing: mismo email desde diferentes IPs")
        void shouldBlockCredentialStuffingSameEmailDifferentIps() {
            // Given - simular 5 intentos fallidos desde diferentes IPs para el mismo email
            // IP rate limit siempre pasa (claves diferentes), email rate limit falla en el 5to
            when(rateLimitPort.tryConsume(eq("login:ip:192.168.1.1"), eq(5), any())).thenReturn(true);
            when(rateLimitPort.tryConsume(eq("login:ip:192.168.1.2"), eq(5), any())).thenReturn(true);
            when(rateLimitPort.tryConsume(eq("login:ip:10.0.0.1"), eq(5), any())).thenReturn(true);
            when(rateLimitPort.tryConsume(eq("login:ip:172.16.0.1"), eq(5), any())).thenReturn(true);
            when(rateLimitPort.tryConsume(eq("login:ip:203.0.113.1"), eq(5), any())).thenReturn(true);

            // Email rate limit: 4 true, 5to false
            when(rateLimitPort.tryConsume(eq("login:email:victim@test.com"), eq(5), any()))
                    .thenReturn(true, true, true, true, false);

            // Password siempre incorrecto
            when(userRepositoryPort.findActiveByEmail("victim@test.com"))
                    .thenReturn(Optional.of(User.withId(1L, "victim@test.com", "encoded", User.Role.ALUMNO, true, null, java.time.Instant.now())));
            when(passwordEncoderPort.matches("wrongpass", "encoded")).thenReturn(false);

            String email = "victim@test.com";
            String password = "wrongpass";

            // When - 4 intentos desde diferentes IPs (deben pasar IP rate limit, fallar password)
            Result<LoginUseCase.LoginResult> result1 = useCase.execute(email, password, "192.168.1.1");
            Result<LoginUseCase.LoginResult> result2 = useCase.execute(email, password, "192.168.1.2");
            Result<LoginUseCase.LoginResult> result3 = useCase.execute(email, password, "10.0.0.1");
            Result<LoginUseCase.LoginResult> result4 = useCase.execute(email, password, "172.16.0.1");

            // 5to intento desde otra IP - debe bloquear por email rate limit
            Result<LoginUseCase.LoginResult> result5 = useCase.execute(email, password, "203.0.113.1");

            // Then
            assertThat(result1.isFailure()).isTrue(); // falla por password
            assertThat(result2.isFailure()).isTrue();
            assertThat(result3.isFailure()).isTrue();
            assertThat(result4.isFailure()).isTrue();
            assertThat(result5.isFailure()).isTrue();
            Result.Failure<LoginUseCase.LoginResult> failure5 = (Result.Failure<LoginUseCase.LoginResult>) result5;
            assertThat(failure5.code()).isEqualTo(Result.ErrorCode.TOO_MANY_REQUESTS);
            assertThat(failure5.message()).isEqualTo("Demasiados intentos para esta cuenta. Intente de nuevo en 15 minutos.");

            // Verificar que se llamó a email rate limit 5 veces
            verify(rateLimitPort, times(5)).tryConsume(eq("login:email:victim@test.com"), eq(5), any());
        }

        @Test
        @DisplayName("Debe resetear email rate limit tras login exitoso")
        void shouldResetEmailRateLimitAfterSuccessfulLogin() {
            // Given
            String encodedPassword = "argon2id$encoded$hash";
            User user = User.withId(1L, "test@test.com", encodedPassword, User.Role.ALUMNO, true, null, java.time.Instant.now());

            when(userRepositoryPort.findActiveByEmail("test@test.com")).thenReturn(Optional.of(user));
            when(passwordEncoderPort.matches("Pass123!", encodedPassword)).thenReturn(true);
            when(rateLimitPort.tryConsume(eq("login:ip:192.168.1.1"), eq(5), any())).thenReturn(true);
            when(rateLimitPort.tryConsume(eq("login:email:test@test.com"), eq(5), any())).thenReturn(true);

            // When
            useCase.execute("test@test.com", "Pass123!", "192.168.1.1");

            // Then
            verify(rateLimitPort).reset("login:email:test@test.com");
        }

        @Test
        @DisplayName("No debe resetear email rate limit tras login fallido por password")
        void shouldNotResetEmailRateLimitAfterFailedLogin() {
            // Given
            String encodedPassword = "argon2id$encoded$hash";
            User user = User.withId(1L, "test@test.com", encodedPassword, User.Role.ALUMNO, true, null, java.time.Instant.now());

            when(userRepositoryPort.findActiveByEmail("test@test.com")).thenReturn(Optional.of(user));
            when(passwordEncoderPort.matches("WrongPass123!", encodedPassword)).thenReturn(false);
            when(rateLimitPort.tryConsume(eq("login:ip:192.168.1.1"), eq(5), any())).thenReturn(true);
            when(rateLimitPort.tryConsume(eq("login:email:test@test.com"), eq(5), any())).thenReturn(true);

            // When
            useCase.execute("test@test.com", "WrongPass123!", "192.168.1.1");

            // Then
            verify(rateLimitPort, never()).reset("login:email:test@test.com");
        }

        @Test
        @DisplayName("Email rate limit debe ser case-insensitive")
        void shouldBeCaseInsensitiveForEmailRateLimit() {
            // Given
            String encodedPassword = "argon2id$encoded$hash";
            User user = User.withId(1L, "test@test.com", encodedPassword, User.Role.ALUMNO, true, null, java.time.Instant.now());

            when(userRepositoryPort.findActiveByEmail("test@test.com")).thenReturn(Optional.of(user));
            when(passwordEncoderPort.matches("Pass123!", encodedPassword)).thenReturn(true);
            when(rateLimitPort.tryConsume(eq("login:ip:1.2.3.4"), eq(5), any())).thenReturn(true);
            when(rateLimitPort.tryConsume(eq("login:email:test@test.com"), eq(5), any())).thenReturn(true);

            // When - email con mayúsculas y espacios
            useCase.execute("  TEST@TEST.COM  ", "Pass123!", "1.2.3.4");

            // Then - clave de rate limit debe ser normalizada
            verify(rateLimitPort).tryConsume("login:email:test@test.com", 5, java.time.Duration.ofMinutes(15));
        }
    }
}