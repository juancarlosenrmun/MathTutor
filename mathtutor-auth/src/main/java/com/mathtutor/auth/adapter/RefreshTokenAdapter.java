package com.mathtutor.auth.adapter;

import com.mathtutor.common.auth.port.TokenServicePort;
import com.mathtutor.persistence.entity.RefreshToken;
import com.mathtutor.persistence.repository.RefreshTokenRepository;
import com.mathtutor.persistence.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/** Almacena tokens de alta entropia por huella SHA-256 indexada, sin guardar el valor en claro. */
@Component
public class RefreshTokenAdapter {
    private final RefreshTokenRepository repository;
    private final UserRepository userRepository;

    @org.springframework.beans.factory.annotation.Value("${jwt.refresh-token-expiration-days:7}")
    private long refreshTokenExpirationDays = 7;

    @jakarta.annotation.PostConstruct
    public void init() {
        if (refreshTokenExpirationDays <= 0 || refreshTokenExpirationDays > Integer.MAX_VALUE / 86400) {
            throw new IllegalStateException("La duracion del refresh debe ser positiva y compatible con maxAge");
        }
    }

    public RefreshTokenAdapter(RefreshTokenRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    @Transactional
    public String storeRefreshToken(Long userId, String email, String role, String rawToken) {
        return storeRefreshTokenWithFamily(userId, email, role, rawToken, UUID.randomUUID().toString());
    }

    @Transactional
    public String storeRefreshTokenWithFamily(Long userId, String email, String role, String rawToken, String familyId) {
        Instant now = Instant.now();
        repository.save(new RefreshToken(digest(rawToken), userId, familyId,
                now.plusSeconds(refreshTokenExpirationDays * 86400), now));
        return rawToken;
    }

    @Transactional(readOnly = true)
    public Optional<TokenServicePort.TokenValidationResult> validateRefreshToken(String rawToken) {
        return repository.findByTokenHash(digest(rawToken)).flatMap(this::validateStoredToken);
    }

    private Optional<TokenServicePort.TokenValidationResult> validateStoredToken(RefreshToken token) {
        if (token.isRevoked() || !token.getExpiresAt().isAfter(Instant.now())) {
            return Optional.empty();
        }
        return userRepository.findById(token.getUserId()).filter(user -> user.isActive())
                .map(user -> new TokenServicePort.TokenValidationResult(user.getId(), user.getEmail(),
                        user.getRole().name(), token.getFamilyId(), token.getId().toString()));
    }

    /** El bloqueo serializa reutilizaciones; sustitucion y revocacion se confirman juntas. */
    @Transactional
    public Optional<Rotation> rotateRefreshToken(String rawToken,
            Function<TokenServicePort.TokenValidationResult, String> tokenFactory) {
        var existing = lockTokenAndOwner(rawToken);
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        RefreshToken previous = existing.get();
        if (previous.isRevoked()) {
            repository.revokeFamily(previous.getFamilyId(), Instant.now());
            return Optional.empty();
        }
        var validation = validateStoredToken(previous);
        if (validation.isEmpty()) {
            return Optional.empty();
        }
        var user = validation.get();
        String replacement = tokenFactory.apply(user);
        previous.setRevoked(true);
        previous.setRevokedAt(Instant.now());
        repository.save(previous);
        storeRefreshTokenWithFamily(user.userId(), user.email(), user.role(), replacement, user.familyId());
        return Optional.of(new Rotation(replacement, user));
    }

    public record Rotation(String refreshToken, TokenServicePort.TokenValidationResult user) {}

    /** Revoca el linaje completo serializando el cierre con cualquier rotacion concurrente. */
    @Transactional
    public void revokeRefreshSession(String rawToken) {
        lockTokenAndOwner(rawToken).ifPresent(token ->
                repository.revokeFamily(token.getFamilyId(), Instant.now()));
    }

    /**
     * Todas las rotaciones y cierres de sesión de un usuario comparten este
     * bloqueo. El token se lee una segunda vez tras tomarlo para ver el estado
     * comprometido más reciente de su familia.
     */
    private Optional<RefreshToken> lockTokenAndOwner(String rawToken) {
        String tokenHash = digest(rawToken);
        Optional<Long> userId = repository.findUserIdByTokenHash(tokenHash);
        if (userId.isEmpty()) {
            return Optional.empty();
        }
        if (userRepository.findByIdForUpdate(userId.get()).isEmpty()) {
            return Optional.empty();
        }
        return repository.findByTokenHashForUpdate(tokenHash);
    }

    @Transactional
    public void revokeRefreshToken(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            return;
        }
        try {
            repository.findById(Long.valueOf(tokenId)).ifPresent(token -> {
                token.setRevoked(true);
                token.setRevokedAt(Instant.now());
                repository.save(token);
            });
        } catch (NumberFormatException ignored) {
            // Identificador de persistencia invalido: no hay token que revocar.
        }
    }

    @Transactional
    public void revokeTokenFamily(String familyId) {
        repository.revokeFamily(familyId, Instant.now());
    }

    @Transactional
    public void cleanupExpiredTokens() {
        repository.deleteByExpiresAtBefore(Instant.now());
    }

    @Transactional(readOnly = true)
    public Optional<RefreshToken> findActiveByUserId(Long userId) {
        return repository.findByUserIdAndRevokedFalse(userId).stream()
                .filter(token -> token.getExpiresAt().isAfter(Instant.now())).findFirst();
    }

    @Transactional(readOnly = true)
    public boolean isRefreshTokenRevoked(String rawToken) {
        return repository.findByTokenHash(digest(rawToken)).map(RefreshToken::isRevoked).orElse(false);
    }

    @Transactional(readOnly = true)
    public String getFamilyIdForRevokedToken(String rawToken) {
        return repository.findByTokenHash(digest(rawToken)).filter(RefreshToken::isRevoked)
                .map(RefreshToken::getFamilyId).orElse(null);
    }

    private String digest(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 no disponible", impossible);
        }
    }
}
