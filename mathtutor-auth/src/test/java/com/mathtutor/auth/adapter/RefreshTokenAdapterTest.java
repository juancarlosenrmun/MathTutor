package com.mathtutor.auth.adapter;

import com.mathtutor.common.auth.port.TokenServicePort;
import com.mathtutor.common.port.PasswordEncoderPort;
import com.mathtutor.persistence.entity.RefreshToken;
import com.mathtutor.persistence.entity.User;
import com.mathtutor.persistence.repository.RefreshTokenRepository;
import com.mathtutor.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests para RefreshTokenAdapter.
 * Verifica almacenamiento, validación, rotación y detección de reuso de refresh tokens.
 * <p>
 * Refresh tokens usan SHA-256 para hash indexado (token aleatorio 256 bits).
 * Argon2id (via PasswordEncoderPort) se usa SOLO para contraseñas.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenAdapter")
class RefreshTokenAdapterTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoderPort passwordEncoder; // No se usa para refresh tokens, solo para contraseñas

    private RefreshTokenAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RefreshTokenAdapter(refreshTokenRepository, userRepository, passwordEncoder);
    }

    /**
     * Calcula SHA-256 hex para usar en tests (mirror del método privado del adapter).
     */
    private static String sha256Hex(String rawToken) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    @Nested
    @DisplayName("storeRefreshToken()")
    class StoreRefreshToken {

        @Test
        @DisplayName("Debe almacenar refresh token con familyId nuevo y retornar token en claro")
        void shouldStoreWithNewFamilyIdAndReturnRawToken() {
            // Given
            String rawToken = "raw-refresh-token-jwt";
            String expectedHash = sha256Hex(rawToken);

            RefreshToken savedToken = new RefreshToken(expectedHash, 1L, "family-123", Instant.now().plusSeconds(7 * 24 * 60 * 60), Instant.now());
            savedToken.setId(1L);
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(savedToken);

            // When
            String result = adapter.storeRefreshToken(1L, "test@example.com", "ALUMNO", rawToken);

            // Then
            assertThat(result).isEqualTo(rawToken);
            // passwordEncoder NO debe llamarse para refresh tokens
            verify(passwordEncoder, never()).encode(anyString());
            verify(refreshTokenRepository).save(argThat(token ->
                    token.getTokenHash().equals(expectedHash) &&
                    token.getUserId() == 1L &&
                    token.getFamilyId() != null &&
                    !token.getFamilyId().isBlank() &&
                    token.getExpiresAt().isAfter(Instant.now())
            ));
        }

        @Test
        @DisplayName("Debe generar familyId único en cada llamada")
        void shouldGenerateUniqueFamilyIdEachCall() {
            // Given
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
                RefreshToken t = invocation.getArgument(0);
                t.setId(1L);
                return t;
            });

            // When
            String result1 = adapter.storeRefreshToken(1L, "test@example.com", "ALUMNO", "token1");
            String result2 = adapter.storeRefreshToken(1L, "test@example.com", "ALUMNO", "token2");

            // Then
            assertThat(result1).isEqualTo("token1");
            assertThat(result2).isEqualTo("token2");
            verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
            verify(passwordEncoder, never()).encode(anyString());
        }
    }

    @Nested
    @DisplayName("storeRefreshTokenWithFamily()")
    class StoreRefreshTokenWithFamily {

        @Test
        @DisplayName("Debe almacenar refresh token con familyId específico y retornar token en claro")
        void shouldStoreWithSpecificFamilyIdAndReturnRawToken() {
            // Given
            String rawToken = "raw-refresh-token-jwt";
            String familyId = "existing-family-456";
            String expectedHash = sha256Hex(rawToken);

            RefreshToken savedToken = new RefreshToken(expectedHash, 1L, familyId, Instant.now().plusSeconds(7 * 24 * 60 * 60), Instant.now());
            savedToken.setId(2L);
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(savedToken);

            // When
            String result = adapter.storeRefreshTokenWithFamily(1L, "test@example.com", "ALUMNO", rawToken, familyId);

            // Then
            assertThat(result).isEqualTo(rawToken);
            verify(passwordEncoder, never()).encode(anyString());
            verify(refreshTokenRepository).save(argThat(token ->
                    token.getTokenHash().equals(expectedHash) &&
                    token.getUserId() == 1L &&
                    token.getFamilyId().equals(familyId)
            ));
        }
    }

    @Nested
    @DisplayName("validateRefreshToken()")
    class ValidateRefreshToken {

        @Test
        @DisplayName("Debe retornar TokenValidationResult cuando token es válido")
        void shouldReturnValidationResultWhenTokenValid() {
            // Given
            String rawToken = "valid-raw-token";
            String tokenHash = sha256Hex(rawToken);
            String familyId = "family-123";
            Instant now = Instant.now();
            Instant expiresAt = now.plusSeconds(7 * 24 * 60 * 60);

            RefreshToken tokenEntity = new RefreshToken(tokenHash, 1L, familyId, expiresAt, now);
            tokenEntity.setId(42L);
            tokenEntity.setRevoked(false);

            when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(tokenEntity));

            User userEntity = User.builder()
                    .id(1L)
                    .email("test@example.com")
                    .passwordHash("pwd")
                    .role(User.Role.ALUMNO)
                    .active(true)
                    .build();
            when(userRepository.findById(1L)).thenReturn(Optional.of(userEntity));

            // When
            Optional<TokenServicePort.TokenValidationResult> result = adapter.validateRefreshToken(rawToken);

            // Then
            assertThat(result).isPresent();
            TokenServicePort.TokenValidationResult validation = result.get();
            assertThat(validation.userId()).isEqualTo(1L);
            assertThat(validation.email()).isEqualTo("test@example.com");
            assertThat(validation.role()).isEqualTo("ALUMNO");
            assertThat(validation.familyId()).isEqualTo(familyId);
            assertThat(validation.tokenId()).isEqualTo("42");
            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("Debe retornar empty cuando token no existe en BD")
        void shouldReturnEmptyWhenTokenDoesNotExist() {
            // Given
            String rawToken = "invalid-token";
            String tokenHash = sha256Hex(rawToken);

            when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.empty());

            // When
            Optional<TokenServicePort.TokenValidationResult> result = adapter.validateRefreshToken(rawToken);

            // Then
            assertThat(result).isEmpty();
            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("Debe retornar empty cuando token ya está revocado (detección de reuso se maneja en endpoint /refresh)")
        void shouldReturnEmptyWhenTokenAlreadyRevoked() {
            // Given
            String rawToken = "revoked-token";
            String tokenHash = sha256Hex(rawToken);
            String familyId = "family-reuse";

            RefreshToken tokenEntity = new RefreshToken(tokenHash, 1L, familyId, Instant.now().plusSeconds(7 * 24 * 60 * 60), Instant.now());
            tokenEntity.setId(1L);
            tokenEntity.setRevoked(true); // Ya revocado
            tokenEntity.setRevokedAt(Instant.now());

            when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(tokenEntity));

            // When
            Optional<TokenServicePort.TokenValidationResult> result = adapter.validateRefreshToken(rawToken);

            // Then - validateRefreshToken es readOnly, solo retorna empty para tokens revocados
            // La revocación de familia se hace en el endpoint /refresh (AuthController)
            assertThat(result).isEmpty();
            verify(refreshTokenRepository, never()).revokeFamily(anyString(), any(Instant.class));
        }

        @Test
        @DisplayName("Debe retornar empty cuando token está expirado")
        void shouldReturnEmptyWhenTokenExpired() {
            // Given
            String rawToken = "expired-token";
            String tokenHash = sha256Hex(rawToken);

            RefreshToken tokenEntity = new RefreshToken(tokenHash, 1L, "family-1", Instant.now().minusSeconds(3600), Instant.now()); // Expirado hace 1 hora
            when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(tokenEntity));

            // When
            Optional<TokenServicePort.TokenValidationResult> result = adapter.validateRefreshToken(rawToken);

            // Then
            assertThat(result).isEmpty();
            verify(refreshTokenRepository, never()).revokeFamily(anyString(), any(Instant.class));
        }

        @Test
        @DisplayName("Debe retornar empty cuando usuario no existe en BD")
        void shouldReturnEmptyWhenUserNotFound() {
            // Given
            String rawToken = "valid-token";
            String tokenHash = sha256Hex(rawToken);

            RefreshToken tokenEntity = new RefreshToken(tokenHash, 999L, "family-1", Instant.now().plusSeconds(7 * 24 * 60 * 60), Instant.now());
            tokenEntity.setId(999L);
            tokenEntity.setRevoked(false);
            when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(tokenEntity));
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When
            Optional<TokenServicePort.TokenValidationResult> result = adapter.validateRefreshToken(rawToken);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Debe retornar empty cuando BD está vacía")
        void shouldReturnEmptyWhenDatabaseEmpty() {
            // Given
            String rawToken = "any-token";
            String tokenHash = sha256Hex(rawToken);
            when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.empty());

            // When
            Optional<TokenServicePort.TokenValidationResult> result = adapter.validateRefreshToken(rawToken);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("revokeRefreshToken()")
    class RevokeRefreshToken {

        @Test
        @DisplayName("Debe revocar token individual cuando existe")
        void shouldRevokeIndividualTokenWhenExists() {
            // Given
            String tokenId = "42";
            RefreshToken tokenEntity = new RefreshToken("hash", 1L, "family-1", Instant.now().plusSeconds(3600), Instant.now());
            tokenEntity.setId(42L);
            tokenEntity.setRevoked(false);

            when(refreshTokenRepository.findById(42L)).thenReturn(Optional.of(tokenEntity));

            // When
            adapter.revokeRefreshToken(tokenId);

            // Then
            verify(refreshTokenRepository).findById(42L);
            assertThat(tokenEntity.isRevoked()).isTrue();
            assertThat(tokenEntity.getRevokedAt()).isNotNull();
            verify(refreshTokenRepository).save(tokenEntity);
        }

        @Test
        @DisplayName("Debe no hacer nada cuando token no existe")
        void shouldDoNothingWhenTokenNotExists() {
            // Given
            when(refreshTokenRepository.findById(99L)).thenReturn(Optional.empty());

            // When
            adapter.revokeRefreshToken("99");

            // Then
            verify(refreshTokenRepository).findById(99L);
            verify(refreshTokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe manejar tokenId null sin llamar al repositorio")
        void shouldHandleNullTokenIdWithoutCallingRepository() {
            // When / Then
            adapter.revokeRefreshToken(null);
            verify(refreshTokenRepository, never()).findById(anyLong());
            verify(refreshTokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe manejar tokenId no numérico sin llamar al repositorio")
        void shouldHandleNonNumericTokenIdWithoutCallingRepository() {
            // When / Then
            adapter.revokeRefreshToken("not-a-number");
            verify(refreshTokenRepository, never()).findById(anyLong());
            verify(refreshTokenRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("revokeTokenFamily()")
    class RevokeTokenFamily {

        @Test
        @DisplayName("Debe llamar a repositorio para revocar familia")
        void shouldCallRepositoryToRevokeFamily() {
            // Given
            String familyId = "family-to-revoke";
            Instant now = Instant.now();

            // When
            adapter.revokeTokenFamily(familyId);

            // Then
            verify(refreshTokenRepository).revokeFamily(eq(familyId), any(Instant.class));
        }
    }

    @Nested
    @DisplayName("cleanupExpiredTokens()")
    class CleanupExpiredTokens {

        @Test
        @DisplayName("Debe llamar a repositorio para borrar tokens expirados")
        void shouldCallRepositoryToDeleteExpiredTokens() {
            // Given
            Instant beforeCall = Instant.now();

            // When
            adapter.cleanupExpiredTokens();

            // Then
            Instant afterCall = Instant.now();
            ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
            verify(refreshTokenRepository).deleteByExpiresAtBefore(captor.capture());
            Instant captured = captor.getValue();
            assertThat(captured).isBetween(beforeCall, afterCall);
        }
    }

    @Nested
    @DisplayName("findActiveByUserId()")
    class FindActiveByUserId {

        @Test
        @DisplayName("Debe retornar token activo cuando existe")
        void shouldReturnActiveTokenWhenExists() {
            // Given
            RefreshToken token = new RefreshToken("hash", 1L, "family-1", Instant.now().plusSeconds(3600), Instant.now());
            token.setId(1L);
            token.setRevoked(false);

            when(refreshTokenRepository.findByUserIdAndRevokedFalse(1L)).thenReturn(List.of(token));

            // When
            Optional<RefreshToken> result = adapter.findActiveByUserId(1L);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(1L);
            assertThat(result.get().isRevoked()).isFalse();
        }

        @Test
        @DisplayName("Debe retornar empty cuando no hay tokens activos")
        void shouldReturnEmptyWhenNoActiveTokens() {
            // Given
            when(refreshTokenRepository.findByUserIdAndRevokedFalse(1L)).thenReturn(List.of());

            // When
            Optional<RefreshToken> result = adapter.findActiveByUserId(1L);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Debe retornar el primer token cuando hay múltiples activos")
        void shouldReturnFirstTokenWhenMultipleActive() {
            // Given
            RefreshToken token1 = new RefreshToken("hash1", 1L, "family-1", Instant.now().plusSeconds(3600), Instant.now());
            token1.setId(1L);
            token1.setRevoked(false);

            RefreshToken token2 = new RefreshToken("hash2", 1L, "family-2", Instant.now().plusSeconds(7200), Instant.now());
            token2.setId(2L);
            token2.setRevoked(false);

            when(refreshTokenRepository.findByUserIdAndRevokedFalse(1L)).thenReturn(List.of(token1, token2));

            // When
            Optional<RefreshToken> result = adapter.findActiveByUserId(1L);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("isRefreshTokenRevoked()")
    class IsRefreshTokenRevoked {

        @Test
        @DisplayName("Debe retornar true si token existe y está revocado")
        void shouldReturnTrueWhenTokenExistsAndRevoked() {
            // Given
            String rawToken = "revoked-token";
            String tokenHash = sha256Hex(rawToken);

            RefreshToken tokenEntity = new RefreshToken(tokenHash, 1L, "family-1", Instant.now().plusSeconds(3600), Instant.now());
            tokenEntity.setRevoked(true);

            when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(tokenEntity));

            // When
            boolean result = adapter.isRefreshTokenRevoked(rawToken);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Debe retornar false si token no existe")
        void shouldReturnFalseWhenTokenNotExists() {
            // Given
            String rawToken = "non-existent-token";
            String tokenHash = sha256Hex(rawToken);

            when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.empty());

            // When
            boolean result = adapter.isRefreshTokenRevoked(rawToken);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Debe retornar false si token existe pero no está revocado")
        void shouldReturnFalseWhenTokenExistsButNotRevoked() {
            // Given
            String rawToken = "active-token";
            String tokenHash = sha256Hex(rawToken);

            RefreshToken tokenEntity = new RefreshToken(tokenHash, 1L, "family-1", Instant.now().plusSeconds(3600), Instant.now());
            tokenEntity.setRevoked(false);

            when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(tokenEntity));

            // When
            boolean result = adapter.isRefreshTokenRevoked(rawToken);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("getFamilyIdForRevokedToken()")
    class GetFamilyIdForRevokedToken {

        @Test
        @DisplayName("Debe retornar familyId si token existe y está revocado")
        void shouldReturnFamilyIdWhenTokenRevoked() {
            // Given
            String rawToken = "revoked-token";
            String tokenHash = sha256Hex(rawToken);
            String familyId = "family-reuse";

            RefreshToken tokenEntity = new RefreshToken(tokenHash, 1L, familyId, Instant.now().plusSeconds(3600), Instant.now());
            tokenEntity.setRevoked(true);

            when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(tokenEntity));

            // When
            String result = adapter.getFamilyIdForRevokedToken(rawToken);

            // Then
            assertThat(result).isEqualTo(familyId);
        }

        @Test
        @DisplayName("Debe retornar null si token no existe")
        void shouldReturnNullWhenTokenNotExists() {
            // Given
            String rawToken = "non-existent-token";
            String tokenHash = sha256Hex(rawToken);

            when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.empty());

            // When
            String result = adapter.getFamilyIdForRevokedToken(rawToken);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Debe retornar null si token existe pero no está revocado")
        void shouldReturnNullWhenTokenNotRevoked() {
            // Given
            String rawToken = "active-token";
            String tokenHash = sha256Hex(rawToken);

            RefreshToken tokenEntity = new RefreshToken(tokenHash, 1L, "family-1", Instant.now().plusSeconds(3600), Instant.now());
            tokenEntity.setRevoked(false);

            when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(tokenEntity));

            // When
            String result = adapter.getFamilyIdForRevokedToken(rawToken);

            // Then
            assertThat(result).isNull();
        }
    }
}