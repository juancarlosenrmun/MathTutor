package com.mathtutor.application.security;

import com.mathtutor.common.auth.port.RateLimitPort;
import com.mathtutor.common.port.PasswordEncoderPort;
import com.mathtutor.persistence.entity.PasswordResetToken;
import com.mathtutor.persistence.entity.User;
import com.mathtutor.persistence.repository.PasswordResetTokenRepository;
import com.mathtutor.persistence.repository.RefreshTokenRepository;
import com.mathtutor.persistence.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PasswordResetServiceTest {
    private static final String EMAIL = "alumna@example.org";
    private final User account = User.builder().id(4L).email(EMAIL).passwordHash("anterior").build();
    private final UserRepository users = mock(UserRepository.class);
    private final PasswordResetTokenRepository tokens = mock(PasswordResetTokenRepository.class);
    private final RefreshTokenRepository sessions = mock(RefreshTokenRepository.class);
    private final PasswordEncoderPort passwords = mock(PasswordEncoderPort.class);
    private final RateLimitPort limits = mock(RateLimitPort.class);
    private final PasswordResetService resets = new PasswordResetService(users, tokens, sessions, passwords, limits);

    @Test
    void persistsOnlyTheDigestAndUsesTheSameInstantForExpiryAndCleanup() throws Exception {
        when(users.findActiveByEmail(EMAIL)).thenReturn(Optional.of(account));
        when(users.findByIdForUpdate(4L)).thenReturn(Optional.of(account));
        when(limits.tryConsume("password-reset:account:4", 3, PasswordResetService.VALIDITY)).thenReturn(true);
        String raw = resets.issue(EMAIL).orElseThrow();
        assertThat(raw).matches("[A-Za-z0-9_-]{43}");
        var saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        var created = ArgumentCaptor.forClass(Instant.class);
        verify(tokens).deleteExpired(eq(4L), created.capture());
        verify(tokens).save(saved.capture());
        assertThat(saved.getValue().getTokenHash()).isEqualTo(digest(raw)).isNotEqualTo(raw);
        assertThat(saved.getValue().getExpiresAt()).isEqualTo(created.getValue().plus(PasswordResetService.VALIDITY));
        assertThat(saved.getValue().getUsedAt()).isNull();
    }

    @Test
    void stopsIssuingForMissingInactiveRenamedOrRateLimitedAccount() {
        assertThat(resets.issue(EMAIL)).isEmpty();
        when(users.findActiveByEmail(EMAIL)).thenReturn(Optional.of(account));
        when(users.findByIdForUpdate(4L)).thenReturn(Optional.of(account));
        account.setActive(false);
        assertThat(resets.issue(EMAIL)).isEmpty();
        account.setActive(true);
        account.setEmail("nuevo@example.org");
        assertThat(resets.issue(EMAIL)).isEmpty();
        account.setEmail(EMAIL);
        assertThat(resets.issue(EMAIL)).isEmpty();
        verifyNoInteractions(tokens);
    }

    @Test
    void assistedDeliveryUsesTheSelectedPersistentIdentity() {
        assertThat(resets.issue(4L)).isEmpty();
        when(users.findByIdForUpdate(4L)).thenReturn(Optional.of(account));
        when(limits.tryConsume("password-reset:account:4", 3, PasswordResetService.VALIDITY)).thenReturn(true);
        var delivery = resets.issue(4L).orElseThrow();
        assertThat(delivery.email()).isEqualTo(EMAIL);
        assertThat(delivery.token()).matches("[A-Za-z0-9_-]{43}");
        account.setActive(false);
        assertThat(resets.issue(4L)).isEmpty();
    }

    @Test
    void confirmsOnlyAfterCheckingExpiryUnderTheUserLock() throws Exception {
        String raw = "A".repeat(43);
        String hash = digest(raw);
        assertThat(resets.confirm(raw, "NuevaClave123!")).isFalse();
        when(tokens.findUserIdByTokenHash(hash)).thenReturn(Optional.of(4L));
        when(users.findByIdForUpdate(4L)).thenReturn(Optional.of(account));
        assertThat(resets.confirm(raw, "NuevaClave123!")).isFalse();
        assertThat(account.getPasswordHash()).isEqualTo("anterior");
        verifyNoInteractions(sessions, passwords);
        when(tokens.findValidByTokenHash(eq(hash), any())).thenReturn(Optional.of(new PasswordResetToken()));
        when(passwords.encode("NuevaClave123!")).thenReturn("nuevo-hash");
        assertThat(resets.confirm(raw, "NuevaClave123!")).isTrue();
        var consumed = ArgumentCaptor.forClass(Instant.class);
        verify(tokens).consumePending(eq(4L), consumed.capture());
        verify(sessions).revokeAllForUser(4L, consumed.getValue());
        assertThat(account.getPasswordHash()).isEqualTo("nuevo-hash");
    }

    private String digest(String raw) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(raw.getBytes(StandardCharsets.US_ASCII)));
    }
}
