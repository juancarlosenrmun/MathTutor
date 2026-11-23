package com.mathtutor.application.security;

import com.mathtutor.common.auth.port.RateLimitPort;
import com.mathtutor.common.port.PasswordEncoderPort;
import com.mathtutor.persistence.entity.PasswordResetToken;
import com.mathtutor.persistence.entity.User;
import com.mathtutor.persistence.repository.PasswordResetTokenRepository;
import com.mathtutor.persistence.repository.RefreshTokenRepository;
import com.mathtutor.persistence.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/** @brief Emite enlaces y aplica el cambio de contraseña bajo el bloqueo de la cuenta. */
@Service
public class PasswordResetService {
    static final Duration VALIDITY = Duration.ofMinutes(30);
    private final SecureRandom random = new SecureRandom();
    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final RefreshTokenRepository sessions;
    private final PasswordEncoderPort passwords;
    private final RateLimitPort limits;

    public PasswordResetService(UserRepository users, PasswordResetTokenRepository tokens,
                                RefreshTokenRepository sessions, PasswordEncoderPort passwords, RateLimitPort limits) {
        this.users = users;
        this.tokens = tokens;
        this.sessions = sessions;
        this.passwords = passwords;
        this.limits = limits;
    }

    /**
     * @param email Correo normalizado.
     * @return Token para correo si la cuenta activa admite otra solicitud.
     */
    @Transactional
    public Optional<String> issue(String email) {
        return users.findActiveByEmail(email).map(User::getId).flatMap(users::findByIdForUpdate)
                .filter(user -> user.isActive() && email.equals(user.getEmail()))
                .filter(user -> limits.tryConsume("password-reset:account:" + user.getId(), 3, VALIDITY))
                .map(user -> {
                    Instant now = Instant.now();
                    byte[] bytes = new byte[32];
                    random.nextBytes(bytes);
                    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
                    tokens.deleteExpired(user.getId(), now);
                    tokens.save(PasswordResetToken.builder().user(user).tokenHash(digest(token))
                            .expiresAt(now.plus(VALIDITY)).build());
                    return token;
                });
    }

    /**
     * @param token Secreto recibido por correo.
     * @param password Nueva contraseña validada.
     * @return Si el token era válido y se consumió al cambiar la contraseña.
     */
    @Transactional
    public boolean confirm(String token, String password) {
        String digest = digest(token);
        var owner = tokens.findUserIdByTokenHash(digest).flatMap(users::findByIdForUpdate).filter(User::isActive);
        if (owner.isEmpty()) return false;
        Instant now = Instant.now();
        if (tokens.findValidByTokenHash(digest, now).isEmpty()) return false;
        User user = owner.get();
        user.setPasswordHash(passwords.encode(password));
        tokens.consumePending(user.getId(), now);
        sessions.revokeAllForUser(user.getId(), now);
        return true;
    }

    private static String digest(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible", e);
        }
    }
}
