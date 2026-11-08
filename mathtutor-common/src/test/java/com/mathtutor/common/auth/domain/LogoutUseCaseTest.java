package com.mathtutor.common.auth.domain;

import com.mathtutor.common.auth.port.AuditLogPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests para LogoutUseCase (TDD - Red phase primero).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LogoutUseCase")
class LogoutUseCaseTest {

    @Mock
    private AuditLogPort auditLogPort;

    private LogoutUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new LogoutUseCase(auditLogPort);
    }

    @Nested
    @DisplayName("Logout exitoso")
    class SuccessfulLogout {

        @Test
        @DisplayName("Debe publicar evento de logout")
        void shouldPublishLogoutEvent() {
            // Given
            Long userId = 1L;
            String email = "test@example.com";

            // When
            Result<Void> result = useCase.execute(userId, email);

            // Then
            assertThat(result.isSuccess()).isTrue();

            verify(auditLogPort).save(argThat(event ->
                    event instanceof UserLoggedOutEvent &&
                    ((UserLoggedOutEvent) event).userId() == userId &&
                    ((UserLoggedOutEvent) event).email().equals(email)
            ));
        }

        @Test
        @DisplayName("Debe normalizar email a minúsculas")
        void shouldNormalizeEmailToLowerCase() {
            // Given
            Long userId = 1L;
            String email = "TEST@EXAMPLE.COM";

            // When
            Result<Void> result = useCase.execute(userId, email);

            // Then
            assertThat(result.isSuccess()).isTrue();

            verify(auditLogPort).save(argThat(event ->
                    event instanceof UserLoggedOutEvent &&
                    ((UserLoggedOutEvent) event).email().equals("test@example.com")
            ));
        }

        @Test
        @DisplayName("Debe ignorar espacios en email")
        void shouldTrimEmail() {
            // Given
            Long userId = 1L;
            String email = "  test@example.com  ";

            // When
            Result<Void> result = useCase.execute(userId, email);

            // Then
            assertThat(result.isSuccess()).isTrue();

            verify(auditLogPort).save(argThat(event ->
                    event instanceof UserLoggedOutEvent &&
                    ((UserLoggedOutEvent) event).email().equals("test@example.com")
            ));
        }
    }

    @Nested
    @DisplayName("Validación de entrada")
    class InputValidation {

        @Test
        @DisplayName("Debe fallar si userId es null")
        void shouldFailIfUserIdIsNull() {
            // When
            Result<Void> result = useCase.execute(null, "test@test.com");

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result).isEqualTo(Result.fail(Result.ErrorCode.VALIDATION_ERROR, "Usuario no válido"));
        }

        @Test
        @DisplayName("Debe fallar si email es null")
        void shouldFailIfEmailIsNull() {
            // When
            Result<Void> result = useCase.execute(1L, null);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result).isEqualTo(Result.fail(Result.ErrorCode.VALIDATION_ERROR, "Usuario no válido"));
        }

        @Test
        @DisplayName("Debe fallar si email está vacío")
        void shouldFailIfEmailIsEmpty() {
            // When
            Result<Void> result = useCase.execute(1L, "");

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result).isEqualTo(Result.fail(Result.ErrorCode.VALIDATION_ERROR, "Usuario no válido"));
        }

        @Test
        @DisplayName("Debe fallar si email es solo espacios")
        void shouldFailIfEmailIsBlank() {
            // When
            Result<Void> result = useCase.execute(1L, "   ");

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result).isEqualTo(Result.fail(Result.ErrorCode.VALIDATION_ERROR, "Usuario no válido"));
        }
    }
}