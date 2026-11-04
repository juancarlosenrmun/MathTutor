package com.mathtutor.persistence.repository;


import com.mathtutor.persistence.PersistenceTestApplication;
import com.mathtutor.persistence.PersistenceTestConfig;
import com.mathtutor.persistence.entity.PasswordResetToken;
import com.mathtutor.persistence.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;


import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integración para PasswordResetTokenRepository.
 */
@SpringBootTest(classes = PersistenceTestApplication.class)
@Import(PersistenceTestConfig.class)
@Transactional

class PasswordResetTokenRepositoryTest {

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindTokenByHash() {
        // Given
        User user = User.builder()
                .email("token@test.com")
                .passwordHash("hash")
                .role(User.Role.ALUMNO)
                .active(true)
                .build();
        userRepository.save(user);

        PasswordResetToken token = PasswordResetToken.builder()
                .tokenHash("token_hash_123")
                .expiresAt(Instant.now().plusSeconds(3600))
                .user(user)
                .build();

        // When
        tokenRepository.save(token);
        var found = tokenRepository.findByTokenHash("token_hash_123");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getTokenHash()).isEqualTo("token_hash_123");
        assertThat(found.get().getUser().getEmail()).isEqualTo("token@test.com");
    }

    @Test
    void shouldFindValidToken() {
        // Given
        User user = User.builder()
                .email("valid@test.com")
                .passwordHash("hash")
                .role(User.Role.ALUMNO)
                .active(true)
                .build();
        userRepository.save(user);

        PasswordResetToken validToken = PasswordResetToken.builder()
                .tokenHash("valid_token")
                .expiresAt(Instant.now().plusSeconds(3600))
                .user(user)
                .build();
        tokenRepository.save(validToken);

        // When
        var found = tokenRepository.findValidByTokenHash("valid_token", Instant.now());

        // Then
        assertThat(found).isPresent();
    }

    @Test
    void shouldNotFindExpiredToken() {
        // Given
        User user = User.builder()
                .email("expired@test.com")
                .passwordHash("hash")
                .role(User.Role.ALUMNO)
                .active(true)
                .build();
        userRepository.save(user);

        PasswordResetToken expiredToken = PasswordResetToken.builder()
                .tokenHash("expired_token")
                .expiresAt(Instant.now().minusSeconds(3600))
                .user(user)
                .build();
        tokenRepository.save(expiredToken);

        // When
        var found = tokenRepository.findValidByTokenHash("expired_token", Instant.now());

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void shouldNotFindUsedToken() {
        // Given
        User user = User.builder()
                .email("used@test.com")
                .passwordHash("hash")
                .role(User.Role.ALUMNO)
                .active(true)
                .build();
        userRepository.save(user);

        PasswordResetToken usedToken = PasswordResetToken.builder()
                .tokenHash("used_token")
                .expiresAt(Instant.now().plusSeconds(3600))
                .usedAt(Instant.now().minusSeconds(100))
                .user(user)
                .build();
        tokenRepository.save(usedToken);

        // When
        var found = tokenRepository.findValidByTokenHash("used_token", Instant.now());

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void shouldDeleteExpiredTokens() {
        // Given
        User user = User.builder()
                .email("cleanup@test.com")
                .passwordHash("hash")
                .role(User.Role.ALUMNO)
                .active(true)
                .build();
        userRepository.save(user);

        PasswordResetToken expiredToken = PasswordResetToken.builder()
                .tokenHash("old_token")
                .expiresAt(Instant.now().minusSeconds(3600))
                .user(user)
                .build();
        tokenRepository.save(expiredToken);

        PasswordResetToken validToken = PasswordResetToken.builder()
                .tokenHash("new_token")
                .expiresAt(Instant.now().plusSeconds(3600))
                .user(user)
                .build();
        tokenRepository.save(validToken);

        // When
        tokenRepository.deleteByExpiresAtBefore(Instant.now());

        // Then
        assertThat(tokenRepository.findByTokenHash("old_token")).isEmpty();
        assertThat(tokenRepository.findByTokenHash("new_token")).isPresent();
    }
}
