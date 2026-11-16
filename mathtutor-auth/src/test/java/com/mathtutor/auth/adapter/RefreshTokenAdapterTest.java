package com.mathtutor.auth.adapter;

import com.mathtutor.common.auth.port.TokenServicePort;
import com.mathtutor.common.port.PasswordEncoderPort;
import com.mathtutor.persistence.entity.RefreshToken;
import com.mathtutor.persistence.entity.User;
import com.mathtutor.persistence.repository.RefreshTokenRepository;
import com.mathtutor.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenAdapter: almacenamiento indexado y rotaci?n at?mica")
class RefreshTokenAdapterTest {
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoderPort passwordEncoder;
    private RefreshTokenAdapter adapter;

    @BeforeEach void setUp() {
        adapter = new RefreshTokenAdapter(refreshTokenRepository, userRepository);
    }

    private RefreshToken token(boolean revoked, Instant expiry) {
        RefreshToken token = new RefreshToken("hash", 1L, "family", expiry, Instant.now());
        token.setId(42L);
        token.setRevoked(revoked);
        return token;
    }

    private void activeUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(User.builder().id(1L)
                .email("test@example.com").passwordHash("hash").role(User.Role.ALUMNO).active(true).build()));
        lenient().when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(User.builder().id(1L)
                .email("test@example.com").passwordHash("hash").role(User.Role.ALUMNO).active(true).build()));
    }

    @Test void storesConfiguredExpiryInsteadOfSevenDays() {
        org.springframework.test.util.ReflectionTestUtils.setField(adapter, "refreshTokenExpirationDays", 1L);
        adapter.init();
        Instant before = Instant.now();
        adapter.storeRefreshToken(1L, "test@example.com", "ALUMNO", "token");
        verify(refreshTokenRepository).save(argThat(token -> token.getExpiresAt()
                .isAfter(before.plusSeconds(86399)) && token.getExpiresAt().isBefore(Instant.now().plusSeconds(86401))));
    }

    @Test void rejectsNonPositiveAndOverflowExpiryConfiguration() {
        org.springframework.test.util.ReflectionTestUtils.setField(adapter, "refreshTokenExpirationDays", 0L);
        assertThatThrownBy(adapter::init).isInstanceOf(IllegalStateException.class);
        org.springframework.test.util.ReflectionTestUtils.setField(adapter, "refreshTokenExpirationDays", Long.MAX_VALUE);
        assertThatThrownBy(adapter::init).isInstanceOf(IllegalStateException.class);
    }

    @Test void storesDeterministicDigestWithoutPasswordHashing() {
        assertThat(adapter.storeRefreshToken(1L, "test@example.com", "ALUMNO", "random-jwt")).isEqualTo("random-jwt");
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getTokenHash()).matches("[0-9a-f]{64}");
        assertThat(captor.getValue().getFamilyId()).isNotBlank();
        assertThat(captor.getValue().getExpiresAt()).isAfter(Instant.now());
        verifyNoInteractions(passwordEncoder);
    }

    @Test void storesExistingFamily() {
        adapter.storeRefreshTokenWithFamily(1L, "test@example.com", "ALUMNO", "new-jwt", "family");
        verify(refreshTokenRepository).save(argThat(value -> value.getFamilyId().equals("family")));
    }

    @Test void validatesUsingIndexedDigest() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token(false, Instant.now().plusSeconds(60))));
        activeUser();
        var result = adapter.validateRefreshToken("random-jwt").orElseThrow();
        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.role()).isEqualTo("ALUMNO");
        assertThat(result.tokenId()).isEqualTo("42");
        assertThat(result.familyId()).isEqualTo("family");
        verify(refreshTokenRepository, never()).findAll();
        verifyNoInteractions(passwordEncoder);
    }

    @Test void rejectsUnknownToken() {
        assertThat(adapter.validateRefreshToken("unknown")).isEmpty();
    }

    @Test void rejectsRevokedToken() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token(true, Instant.now().plusSeconds(60))));
        assertThat(adapter.validateRefreshToken("revoked")).isEmpty();
        verifyNoInteractions(userRepository);
    }

    @Test void rejectsExpiredToken() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token(false, Instant.now().minusSeconds(1))));
        assertThat(adapter.validateRefreshToken("expired")).isEmpty();
    }

    @Test void rejectsMissingUser() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token(false, Instant.now().plusSeconds(60))));
        assertThat(adapter.validateRefreshToken("missing-user")).isEmpty();
    }

    @Test void rejectsInactiveUser() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token(false, Instant.now().plusSeconds(60))));
        when(userRepository.findById(1L)).thenReturn(Optional.of(User.builder().id(1L).active(false).build()));
        assertThat(adapter.validateRefreshToken("inactive-user")).isEmpty();
    }

    @Test void rotatesUnderDatabaseLockAndPreservesFamily() {
        RefreshToken previous = token(false, Instant.now().plusSeconds(60));
        when(refreshTokenRepository.findUserIdByTokenHash(anyString())).thenReturn(Optional.of(1L));
        when(refreshTokenRepository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(previous));
        activeUser();
        var result = adapter.rotateRefreshToken("old", claims -> "new");
        assertThat(result.orElseThrow().refreshToken()).isEqualTo("new");
        assertThat(previous.isRevoked()).isTrue();
        assertThat(previous.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(argThat(value -> value != previous && value.getFamilyId().equals("family")));
    }

    @Test void revokesWholeFamilyOnReplayWithoutCreatingNewToken() {
        when(refreshTokenRepository.findUserIdByTokenHash(anyString())).thenReturn(Optional.of(1L));
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(User.builder().id(1L).active(true).build()));
        when(refreshTokenRepository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(token(true, Instant.now().plusSeconds(60))));
        assertThat(adapter.rotateRefreshToken("old", claims -> "new")).isEmpty();
        verify(refreshTokenRepository).revokeFamily(eq("family"), any(Instant.class));
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test void rejectsUnknownRotation() {
        assertThat(adapter.rotateRefreshToken("unknown", claims -> "new")).isEmpty();
    }

    @Test void rejectsExpiredRotation() {
        when(refreshTokenRepository.findUserIdByTokenHash(anyString())).thenReturn(Optional.of(1L));
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(User.builder().id(1L).active(true).build()));
        when(refreshTokenRepository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(token(false, Instant.now().minusSeconds(1))));
        assertThat(adapter.rotateRefreshToken("expired", claims -> "new")).isEmpty();
    }

    @Test void doesNotConsumeTokenIfReplacementGenerationFails() {
        RefreshToken previous = token(false, Instant.now().plusSeconds(60));
        when(refreshTokenRepository.findUserIdByTokenHash(anyString())).thenReturn(Optional.of(1L));
        when(refreshTokenRepository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(previous));
        activeUser();
        assertThatThrownBy(() -> adapter.rotateRefreshToken("old", claims -> { throw new IllegalStateException("failure"); }))
                .isInstanceOf(IllegalStateException.class);
        assertThat(previous.isRevoked()).isFalse();
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test void revokesSessionFamilyUnderLockEvenIfTokenAlreadyRevoked() {
        when(refreshTokenRepository.findUserIdByTokenHash(anyString())).thenReturn(Optional.of(1L));
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(User.builder().id(1L).active(true).build()));
        when(refreshTokenRepository.findByTokenHashForUpdate(anyString()))
                .thenReturn(Optional.of(token(true, Instant.now().plusSeconds(60))));
        adapter.revokeRefreshSession("old");
        verify(refreshTokenRepository).revokeFamily(eq("family"), any(Instant.class));
    }

    @Test void unknownLogoutIsIdempotent() {
        adapter.revokeRefreshSession("unknown");
        verify(refreshTokenRepository, never()).revokeFamily(anyString(), any());
    }

    @Test void locksTheUserBeforeRefreshingOrRevokingAnyFamilyMember() {
        RefreshToken previous = token(false, Instant.now().plusSeconds(60));
        when(refreshTokenRepository.findUserIdByTokenHash(anyString())).thenReturn(Optional.of(1L));
        when(refreshTokenRepository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(previous));
        activeUser();

        adapter.rotateRefreshToken("old", ignored -> "new");
        adapter.revokeRefreshSession("old");

        verify(userRepository, times(2)).findByIdForUpdate(1L);
        verify(refreshTokenRepository, times(2)).findByTokenHashForUpdate(anyString());
    }

    @Test void identifiesRevokedTokensByDigest() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token(true, Instant.now().plusSeconds(60))));
        assertThat(adapter.isRefreshTokenRevoked("old")).isTrue();
        assertThat(adapter.getFamilyIdForRevokedToken("old")).isEqualTo("family");
    }

    @Test void unknownTokenHasNoFamily() {
        assertThat(adapter.isRefreshTokenRevoked("unknown")).isFalse();
        assertThat(adapter.getFamilyIdForRevokedToken("unknown")).isNull();
    }

    @Test void activeTokenIsNotReplay() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token(false, Instant.now().plusSeconds(60))));
        assertThat(adapter.isRefreshTokenRevoked("current")).isFalse();
        assertThat(adapter.getFamilyIdForRevokedToken("current")).isNull();
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
}
