package com.mathtutor.auth.adapter;

import com.mathtutor.common.auth.port.TokenServicePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenServiceAdapter (JJWT)")
class TokenServiceAdapterTest {

    @Mock
    private RefreshTokenAdapter refreshTokenAdapter;

    private TokenServiceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new TokenServiceAdapter(refreshTokenAdapter);
        // Set test values via reflection
        ReflectionTestUtils.setField(adapter, "jwtSecret", "test-secret-key-at-least-256-bits-long-for-hs256-signing");
        ReflectionTestUtils.setField(adapter, "accessTokenExpirationMinutes", 15L);
        ReflectionTestUtils.setField(adapter, "refreshTokenExpirationDays", 7L);
        adapter.init();
    }

    @Nested
    @DisplayName("generateAccessToken()")
    class GenerateAccessToken {

        @Test
        @DisplayName("Debe generar access token JWT válido")
        void shouldGenerateValidAccessToken() {
            // When
            String token = adapter.generateAccessToken(1L, "test@example.com", "ALUMNO");

            // Then
            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
            assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
        }

        @Test
        @DisplayName("Debe generar token con claims correctos")
        void shouldGenerateTokenWithCorrectClaims() {
            // When
            String token = adapter.generateAccessToken(42L, "user@test.com", "PROFESOR");
            Map<String, Object> claims = adapter.extractClaims(token);

            // Then
            assertThat(claims.get("userId")).isEqualTo(42L);
            assertThat(claims.get("email")).isEqualTo("user@test.com");
            assertThat(claims.get("role")).isEqualTo("PROFESOR");
            assertThat(claims.get("issuedAt")).isNotNull();
            assertThat(claims.get("expiresAt")).isNotNull();
        }
    }

    @Nested
    @DisplayName("generateRefreshToken()")
    class GenerateRefreshToken {

        @Test
        @DisplayName("Debe generar refresh token JWT válido con tokenId")
        void shouldGenerateValidRefreshTokenWithTokenId() {
            // Given
            String tokenId = UUID.randomUUID().toString();

            // When
            String token = adapter.generateRefreshToken(1L, "test@example.com", "ALUMNO", tokenId);
            Map<String, Object> claims = adapter.extractClaims(token);

            // Then
            assertThat(token).isNotNull();
            assertThat(claims.get("userId")).isEqualTo(1L);
            assertThat(claims.get("email")).isEqualTo("test@example.com");
            assertThat(claims.get("role")).isEqualTo("ALUMNO");
            assertThat(claims.get("tokenId")).isEqualTo(tokenId);
        }
    }

    @Nested
    @DisplayName("validateToken()")
    class ValidateToken {

        @Test
        @DisplayName("Debe validar access token correcto y retornar claims")
        void shouldValidateCorrectAccessToken() throws TokenServicePort.InvalidTokenException {
            // Given
            String token = adapter.generateAccessToken(1L, "test@example.com", "ALUMNO");

            // When
            Map<String, Object> claims = adapter.validateToken(token);

            // Then
            assertThat(claims).isNotNull();
            assertThat(claims.get("userId")).isEqualTo(1L);
            assertThat(claims.get("email")).isEqualTo("test@example.com");
            assertThat(claims.get("role")).isEqualTo("ALUMNO");
        }

        @Test
        @DisplayName("Debe validar refresh token correcto y retornar claims")
        void shouldValidateCorrectRefreshToken() throws TokenServicePort.InvalidTokenException {
            // Given
            String tokenId = UUID.randomUUID().toString();
            String token = adapter.generateRefreshToken(1L, "test@example.com", "ALUMNO", tokenId);

            // Mock validateRefreshToken to return valid result
            when(refreshTokenAdapter.validateRefreshToken(anyString()))
                .thenReturn(Optional.of(new TokenServicePort.TokenValidationResult(1L, "test@example.com", "ALUMNO", "family-1", tokenId)));

            // When
            Map<String, Object> claims = adapter.validateToken(token);

            // Then
            assertThat(claims).isNotNull();
            assertThat(claims.get("userId")).isEqualTo(1L);
            assertThat(claims.get("tokenId")).isEqualTo(tokenId);
        }

        @Test
        @DisplayName("Debe lanzar excepción para token expirado")
        void shouldThrowForExpiredToken() {
            // Given - crear adapter con expiración muy corta
            TokenServiceAdapter shortExpiryAdapter = new TokenServiceAdapter(mock(RefreshTokenAdapter.class));
            ReflectionTestUtils.setField(shortExpiryAdapter, "jwtSecret", "test-secret-key-at-least-256-bits-long-for-hs256-signing");
            ReflectionTestUtils.setField(shortExpiryAdapter, "accessTokenExpirationMinutes", 0L); // Expirado inmediatamente
            ReflectionTestUtils.setField(shortExpiryAdapter, "refreshTokenExpirationDays", 7L);
            shortExpiryAdapter.init();

            String token = shortExpiryAdapter.generateAccessToken(1L, "test@example.com", "ALUMNO");

            // Esperar un momento para que expire
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}

            // When / Then
            assertThatThrownBy(() -> shortExpiryAdapter.validateToken(token))
                    .isInstanceOf(TokenServicePort.InvalidTokenException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        @DisplayName("Debe lanzar excepción para token con firma inválida")
        void shouldThrowForInvalidSignature() {
            // Given - token firmado con clave diferente
            TokenServiceAdapter otherAdapter = new TokenServiceAdapter(mock(RefreshTokenAdapter.class));
            ReflectionTestUtils.setField(otherAdapter, "jwtSecret", "different-secret-key-at-least-256-bits-long-for-hs256-signing");
            ReflectionTestUtils.setField(otherAdapter, "accessTokenExpirationMinutes", 15L);
            ReflectionTestUtils.setField(otherAdapter, "refreshTokenExpirationDays", 7L);
            otherAdapter.init();

            String token = otherAdapter.generateAccessToken(1L, "test@example.com", "ALUMNO");

            // When / Then
            assertThatThrownBy(() -> adapter.validateToken(token))
                    .isInstanceOf(TokenServicePort.InvalidTokenException.class)
                    .hasMessageContaining("Invalid token");
        }

        @Test
        @DisplayName("Debe lanzar excepción para token malformado")
        void shouldThrowForMalformedToken() {
            // When / Then
            assertThatThrownBy(() -> adapter.validateToken("not.a.valid.token"))
                    .isInstanceOf(TokenServicePort.InvalidTokenException.class)
                    .hasMessageContaining("Invalid token");
        }

        @Test
        @DisplayName("Debe lanzar excepción para refresh token revocado")
        void shouldThrowForRevokedRefreshToken() {
            // Given
            String tokenId = UUID.randomUUID().toString();
            String token = adapter.generateRefreshToken(1L, "test@example.com", "ALUMNO", tokenId);
            when(refreshTokenAdapter.validateRefreshToken(anyString()))
                .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> adapter.validateToken(token))
                    .isInstanceOf(TokenServicePort.InvalidTokenException.class)
                    .hasMessageContaining("revoked");
        }
    }

    @Nested
    @DisplayName("revokeRefreshToken() y revokeTokenFamily()")
    class RevokeAndCleanup {

        @Test
        @DisplayName("Debe revocar refresh token")
        void shouldRevokeRefreshToken() {
            // Given
            String tokenId = UUID.randomUUID().toString();
            String token = adapter.generateRefreshToken(1L, "test@example.com", "ALUMNO", tokenId);
            when(refreshTokenAdapter.validateRefreshToken(anyString()))
                .thenReturn(Optional.empty());

            // When
            adapter.revokeRefreshToken(tokenId);

            // Then
            verify(refreshTokenAdapter).revokeRefreshToken(tokenId);
            assertThatThrownBy(() -> adapter.validateToken(token))
                    .isInstanceOf(TokenServicePort.InvalidTokenException.class)
                    .hasMessageContaining("revoked");
        }

        @Test
        @DisplayName("Debe manejar tokenId null en revokeRefreshToken")
        void shouldHandleNullTokenIdInRevoke() {
            // When / Then - no debe lanzar excepción
            adapter.revokeRefreshToken(null);
            verify(refreshTokenAdapter).revokeRefreshToken(null);
        }

        @Test
        @DisplayName("Debe revocar familia de tokens")
        void shouldRevokeTokenFamily() {
            // Given
            String familyId = "family-123";

            // When
            adapter.revokeTokenFamily(familyId);

            // Then
            verify(refreshTokenAdapter).revokeTokenFamily(familyId);
        }
    }

    @Nested
    @DisplayName("storeRefreshToken() y storeRefreshTokenWithFamily()")
    class StoreRefreshToken {

        @Test
        @DisplayName("Debe almacenar refresh token y retornar token JWT")
        void shouldStoreRefreshTokenAndReturnJwt() {
            // Given
            when(refreshTokenAdapter.storeRefreshToken(anyLong(), anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(3)); // Return the rawToken (4th arg)

            // When
            String result = adapter.storeRefreshToken(1L, "test@example.com", "ALUMNO", "raw-token");

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isNotEmpty();
            assertThat(result.split("\\.")).hasSize(3); // Should be a JWT
            verify(refreshTokenAdapter).storeRefreshToken(eq(1L), eq("test@example.com"), eq("ALUMNO"), anyString());
        }

        @Test
        @DisplayName("Debe almacenar refresh token con familyId específico")
        void shouldStoreRefreshTokenWithFamily() {
            // Given
            String familyId = "family-123";
            when(refreshTokenAdapter.storeRefreshTokenWithFamily(anyLong(), anyString(), anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(3)); // Return the rawToken (4th arg)

            // When
            String result = adapter.storeRefreshTokenWithFamily(1L, "test@example.com", "ALUMNO", "raw-token", familyId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isNotEmpty();
            assertThat(result.split("\\.")).hasSize(3); // Should be a JWT
            verify(refreshTokenAdapter).storeRefreshTokenWithFamily(eq(1L), eq("test@example.com"), eq("ALUMNO"), anyString(), eq(familyId));
        }
    }

    @Nested
    @DisplayName("extractClaims()")
    class ExtractClaims {

        @Test
        @DisplayName("Debe extraer claims sin validar firma")
        void shouldExtractClaimsWithoutValidation() {
            // Given - token de otro adapter (firma diferente)
            TokenServiceAdapter otherAdapter = new TokenServiceAdapter(mock(RefreshTokenAdapter.class));
            ReflectionTestUtils.setField(otherAdapter, "jwtSecret", "different-secret-key-at-least-256-bits-long-for-hs256-signing");
            ReflectionTestUtils.setField(otherAdapter, "accessTokenExpirationMinutes", 15L);
            ReflectionTestUtils.setField(otherAdapter, "refreshTokenExpirationDays", 7L);
            otherAdapter.init();

            String token = otherAdapter.generateAccessToken(99L, "other@test.com", "ADMIN");

            // When
            Map<String, Object> claims = adapter.extractClaims(token);

            // Then - debe extraer claims aunque la firma sea diferente
            assertThat(claims.get("userId")).isEqualTo(99L);
            assertThat(claims.get("email")).isEqualTo("other@test.com");
            assertThat(claims.get("role")).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("Debe extraer claims de refresh token con tokenId")
        void shouldExtractClaimsFromRefreshTokenWithTokenId() {
            // Given
            String tokenId = UUID.randomUUID().toString();
            String token = adapter.generateRefreshToken(1L, "test@example.com", "ALUMNO", tokenId);

            // When
            Map<String, Object> claims = adapter.extractClaims(token);

            // Then
            assertThat(claims.get("userId")).isEqualTo(1L);
            assertThat(claims.get("email")).isEqualTo("test@example.com");
            assertThat(claims.get("role")).isEqualTo("ALUMNO");
            assertThat(claims.get("tokenId")).isEqualTo(tokenId);
        }

        @Test
        @DisplayName("Debe manejar claims opcionales ausentes (iat, exp)")
        void shouldHandleMissingOptionalClaims() {
            // Given - crear token válido
            String token = adapter.generateAccessToken(1L, "test@example.com", "ALUMNO");

            // When
            Map<String, Object> claims = adapter.extractClaims(token);

            // Then - issuedAt y expiresAt deberían estar presentes en tokens generados
            assertThat(claims.get("issuedAt")).isNotNull();
            assertThat(claims.get("expiresAt")).isNotNull();
        }

        @Test
        @DisplayName("Debe lanzar excepción para formato JWT inválido en extractClaims")
        void shouldThrowForInvalidJwtFormatInExtractClaims() {
            // When / Then
            assertThatThrownBy(() -> adapter.extractClaims("invalid.token"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid JWT format");
        }
    }

    @Nested
    @DisplayName("Validación de clave secreta")
    class SecretKeyValidation {

        @Test
        @DisplayName("Debe lanzar excepción si clave es menor a 256 bits")
        void shouldThrowIfSecretLessThan256Bits() {
            // Given
            TokenServiceAdapter weakAdapter = new TokenServiceAdapter(mock(RefreshTokenAdapter.class));
            ReflectionTestUtils.setField(weakAdapter, "jwtSecret", "short"); // Menos de 32 chars
            ReflectionTestUtils.setField(weakAdapter, "accessTokenExpirationMinutes", 15L);
            ReflectionTestUtils.setField(weakAdapter, "refreshTokenExpirationDays", 7L);

            // When / Then
            assertThatThrownBy(weakAdapter::init)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("256 bits");
        }
    }
}