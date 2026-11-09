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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests para RefreshTokenAdapter.
 * Verifica almacenamiento, validación, rotación y detección de reuso de refresh tokens.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenAdapter")
class RefreshTokenAdapterTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoderPort passwordEncoder;

    private RefreshTokenAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RefreshTokenAdapter(refreshTokenRepository, userRepository, passwordEncoder);
    }

    @Nested
    @DisplayName("storeRefreshToken()")
    class StoreRefreshToken {

        @Test
        @DisplayName("Debe almacenar refresh token con familyId nuevo y retornar token en claro")
        void shouldStoreWithNewFamilyIdAndReturnRawToken() {
            // Given
            String rawToken = "raw-refresh-token-jwt";
            String encodedHash = "argon2id$hash$value";
            when(passwordEncoder.encode(rawToken)).thenReturn(encodedHash);

            RefreshToken savedToken = new RefreshToken(encodedHash, 1L, "family-123", Instant.now().plusSeconds(7 * 24 * 60 * 60), Instant.now());
            savedToken.setId(1L);
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(savedToken);

            // When
            String result = adapter.storeRefreshToken(1L, "test@example.com", "ALUMNO", rawToken);

            // Then
            assertThat(result).isEqualTo(rawToken);
            verify(passwordEncoder).encode(rawToken);
            verify(refreshTokenRepository).save(argThat(token ->
                    token.getTokenHash().equals(encodedHash) &&
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
            when(passwordEncoder.encode(anyString())).thenReturn("hash");
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
            String encodedHash = "argon2id$hash$value";
            when(passwordEncoder.encode(rawToken)).thenReturn(encodedHash);

            RefreshToken savedToken = new RefreshToken(encodedHash, 1L, familyId, Instant.now().plusSeconds(7 * 24 * 60 * 60), Instant.now());
            savedToken.setId(2L);
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(savedToken);

            // When
            String result = adapter.storeRefreshTokenWithFamily(1L, "test@example.com", "ALUMNO", rawToken, familyId);

            // Then
            assertThat(result).isEqualTo(rawToken);
            verify(passwordEncoder).encode(rawToken);
            verify(refreshTokenRepository).save(argThat(token ->
                    token.getTokenHash().equals(encodedHash) &&
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
            String tokenHash = "argon2id$hash$value";
            String familyId = "family-123";
            Instant now = Instant.now();
            Instant expiresAt = now.plusSeconds(7 * 24 * 60 * 60);

            RefreshToken tokenEntity = new RefreshToken(tokenHash, 1L, familyId, expiresAt, now);
            tokenEntity.setId(42L);
            tokenEntity.setRevoked(false);

            when(refreshTokenRepository.findAll()).thenReturn(List.of(tokenEntity));
            when(passwordEncoder.matches(rawToken, tokenHash)).thenReturn(true);

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
        }

        @Test
        @DisplayName("Debe retornar empty cuando token no coincide con ningún hash")
        void shouldReturnEmptyWhenTokenDoesNotMatchAnyHash() {
            // Given
            String rawToken = "invalid-token";
            String tokenHash = "argon2id$hash$value";

            RefreshToken tokenEntity = new RefreshToken(tokenHash, 1L, "family-1", Instant.now().plusSeconds(7 * 24 * 60 * 60), Instant.now());
            when(refreshTokenRepository.findAll()).thenReturn(List.of(tokenEntity));
            when(passwordEncoder.matches(rawToken, tokenHash)).thenReturn(false);

            // When
            Optional<TokenServicePort.TokenValidationResult> result = adapter.validateRefreshToken(rawToken);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Debe retornar empty y revocar familia cuando token ya está revocado (detección de reuso)")
        void shouldReturnEmptyAndRevokeFamilyWhenTokenAlreadyRevoked() {
            // Given
            String rawToken = "revoked-token";
            String tokenHash = "argon2id$hash$value";
            String familyId = "family-reuse";

            RefreshToken tokenEntity = new RefreshToken(tokenHash, 1L, familyId, Instant.now().plusSeconds(7 * 24 * 60 * 60), Instant.now());
            tokenEntity.setId(1L);
            tokenEntity.setRevoked(true); // Ya revocado
            tokenEntity.setRevokedAt(Instant.now());

            when(refreshTokenRepository.findAll()).thenReturn(List.of(tokenEntity));
            when(passwordEncoder.matches(rawToken, tokenHash)).thenReturn(true);
            // revokeTokenFamily se llama internamente

            // When
            Optional<TokenServicePort.TokenValidationResult> result = adapter.validateRefreshToken(rawToken);

            // Then
            assertThat(result).isEmpty();
            verify(refreshTokenRepository).revokeFamily(eq(familyId), any(Instant.class));
        }

        @Test
        @DisplayName("Debe retornar empty cuando token está expirado")
        void shouldReturnEmptyWhenTokenExpired() {
            // Given
            String rawToken = "expired-token";
            String tokenHash = "argon2id$hash$value";

            RefreshToken tokenEntity = new RefreshToken(tokenHash, 1L, "family-1", Instant.now().minusSeconds(3600), Instant.now()); // Expirado hace 1 hora
            when(refreshTokenRepository.findAll()).thenReturn(List.of(tokenEntity));
            when(passwordEncoder.matches(rawToken, tokenHash)).thenReturn(true);

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
            String tokenHash = "argon2id$hash$value";

            RefreshToken tokenEntity = new RefreshToken(tokenHash, 999L, "family-1", Instant.now().plusSeconds(7 * 24 * 60 * 60), Instant.now());
            when(refreshTokenRepository.findAll()).thenReturn(List.of(tokenEntity));
            when(passwordEncoder.matches(rawToken, tokenHash)).thenReturn(true);
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When
            Optional<TokenServicePort.TokenValidationResult> result = adapter.validateRefreshToken(rawToken);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Debe manejar excepción de passwordEncoder.matches y continuar con siguiente token")
        void shouldHandleExceptionInMatchesAndContinue() {
            // Given
            String rawToken = "valid-token";
            String badHash = "bad-hash";
            String goodHash = "argon2id$hash$value";

            RefreshToken badToken = new RefreshToken(badHash, 1L, "family-1", Instant.now().plusSeconds(7 * 24 * 60 * 60), Instant.now());
            RefreshToken goodToken = new RefreshToken(goodHash, 2L, "family-2", Instant.now().plusSeconds(7 * 24 * 60 * 60), Instant.now());
            goodToken.setId(2L);
            goodToken.setRevoked(false);

            when(refreshTokenRepository.findAll()).thenReturn(List.of(badToken, goodToken));
            when(passwordEncoder.matches(rawToken, badHash)).thenThrow(new RuntimeException("Invalid hash format"));
            when(passwordEncoder.matches(rawToken, goodHash)).thenReturn(true);

            User userEntity = User.builder()
                    .id(2L)
                    .email("test2@example.com")
                    .passwordHash("pwd")
                    .role(User.Role.PROFESOR)
                    .active(true)
                    .build();
            when(userRepository.findById(2L)).thenReturn(Optional.of(userEntity));

            // When
            Optional<TokenServicePort.TokenValidationResult> result = adapter.validateRefreshToken(rawToken);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().userId()).isEqualTo(2L);
            assertThat(result.get().email()).isEqualTo("test2@example.com");
            assertThat(result.get().role()).isEqualTo("PROFESOR");
        }

        @Test
        @DisplayName("Debe retornar empty cuando BD está vacía")
        void shouldReturnEmptyWhenDatabaseEmpty() {
            // Given
            when(refreshTokenRepository.findAll()).thenReturn(List.of());

            // When
            Optional<TokenServicePort.TokenValidationResult> result = adapter.validateRefreshToken("any-token");

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
            Instant now = Instant.now();

            // When
            adapter.cleanupExpiredTokens();

            // Then
            verify(refreshTokenRepository).deleteByExpiresAtBefore(now);
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
}