package com.mathtutor.auth.adapter;

import com.mathtutor.common.auth.port.TokenServicePort;
import com.mathtutor.common.port.PasswordEncoderPort;
import com.mathtutor.persistence.entity.RefreshToken;
import com.mathtutor.persistence.entity.User;
import com.mathtutor.persistence.repository.RefreshTokenRepository;
import com.mathtutor.persistence.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador para almacenamiento de refresh tokens en base de datos.
 * Implementa rotación de tokens con family tracking y detección de reuso (revocación de familia completa).
 * <p>
 * Refresh tokens son aleatorios de 256 bits → hash SHA-256 para búsqueda indexada + comparación en tiempo constante.
 * Argon2id se usa SOLO para contraseñas (baja entropía). No para tokens de alta entropía.
 */
@Component
public class RefreshTokenAdapter {

    private final RefreshTokenRepository repository;
    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoder; // Solo para contraseñas, no para refresh tokens

    public RefreshTokenAdapter(RefreshTokenRepository repository, UserRepository userRepository, PasswordEncoderPort passwordEncoder) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Calcula SHA-256 del token (hex, 64 chars).
     * Usado para búsqueda indexada en BD (token_hash único).
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

    /**
     * Comparación en tiempo constante de dos hashes hex.
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    /**
     * Almacena un nuevo refresh token en la BD.
     * Genera un familyId nuevo para tracking de rotación.
     *
     * @return refresh token JWT (el mismo que se pasó como rawToken)
     */
    @Transactional
    public String storeRefreshToken(Long userId, String email, String role, String rawToken) {
        // Generar familyId único para esta cadena de rotación
        String familyId = UUID.randomUUID().toString();
        return storeRefreshTokenWithFamily(userId, email, role, rawToken, familyId);
    }

    /**
     * Almacena un refresh token en la BD con un familyId específico (para rotación).
     *
     * @return refresh token JWT (el mismo que se pasó como rawToken)
     */
    @Transactional
    public String storeRefreshTokenWithFamily(Long userId, String email, String role, String rawToken, String familyId) {
        // Hash SHA-256 para búsqueda indexada (token aleatorio 256 bits → Argon2id innecesario)
        String tokenHash = sha256Hex(rawToken);

        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(7 * 24 * 60 * 60); // 7 días

        RefreshToken entity = new RefreshToken(tokenHash, userId, familyId, expiresAt, now);
        repository.save(entity);
        return rawToken; // Devolver el token en claro para que el cliente lo reciba
    }

    /**
     * Valida un refresh token contra la BD.
     * Busca por token_hash (SHA-256) → verifica hash en tiempo constante → chequea revocado/expirado.
     * <p>
     * Read-only transacción: solo lectura. La detección de reuso se maneja
     * en el endpoint /refresh, no aquí.
     */
    @Transactional(readOnly = true)
    public Optional<TokenServicePort.TokenValidationResult> validateRefreshToken(String rawToken) {
        String tokenHash = sha256Hex(rawToken);
        var tokenOpt = repository.findByTokenHash(tokenHash);

        if (tokenOpt.isEmpty()) {
            return Optional.empty();
        }

        var token = tokenOpt.get();

        // Verificación en tiempo constante (defensa en profundidad)
        if (!constantTimeEquals(token.getTokenHash(), tokenHash)) {
            return Optional.empty();
        }

        // Verificar no revocado
        if (token.isRevoked()) {
            return Optional.empty();
        }
        // Verificar no expirado
        if (token.getExpiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }

        // Obtener email y role del usuario
        var userOpt = userRepository.findById(token.getUserId());
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        User user = userOpt.get();
        String email = user.getEmail();
        String role = user.getRole().name();

        return Optional.of(new TokenServicePort.TokenValidationResult(
            token.getUserId(),
            email,
            role,
            token.getFamilyId(),
            token.getId().toString()
        ));
    }

    /**
     * Revoca un token individual (logout normal o rotación).
     */
    @Transactional
    public void revokeRefreshToken(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            return;
        }
        try {
            Long id = Long.valueOf(tokenId);
            repository.findById(id).ifPresent(token -> {
                token.setRevoked(true);
                token.setRevokedAt(Instant.now());
                repository.save(token);
            });
        } catch (NumberFormatException e) {
            // tokenId no es un número válido, no hacer nada
        }
    }

    /**
     * Revoca TODA la familia de tokens (detección de reuso - ataque de replay).
     * Se llama cuando se detecta que un token ya revocado se intenta usar de nuevo.
     */
    @Transactional
    public void revokeTokenFamily(String familyId) {
        int revokedCount = repository.revokeFamily(familyId, Instant.now());
        // Log de seguridad: se revocaron X tokens de la familia Y por detección de reuso
    }

    /**
     * Limpia tokens expirados (job periódico).
     */
    @Transactional
    public void cleanupExpiredTokens() {
        repository.deleteByExpiresAtBefore(Instant.now());
    }

    /**
     * Busca tokens activos de un usuario.
     */
    @Transactional(readOnly = true)
    public Optional<RefreshToken> findActiveByUserId(Long userId) {
        return repository.findByUserIdAndRevokedFalse(userId).stream().findFirst();
    }

    /**
     * Verifica si un refresh token existe en BD pero está revocado (indicando reuso).
     *
     * @param rawToken token de refresco en claro
     * @return true si el token existe y está revocado
     */
    @Transactional(readOnly = true)
    public boolean isRefreshTokenRevoked(String rawToken) {
        String tokenHash = sha256Hex(rawToken);
        var tokenOpt = repository.findByTokenHash(tokenHash);
        return tokenOpt.map(RefreshToken::isRevoked).orElse(false);
    }

    /**
     * Obtiene el familyId de un refresh token revocado (para revocar la familia completa).
     *
     * @param rawToken token de refresco en claro
     * @return familyId si el token existe y está revocado, null en caso contrario
     */
    @Transactional(readOnly = true)
    public String getFamilyIdForRevokedToken(String rawToken) {
        String tokenHash = sha256Hex(rawToken);
        var tokenOpt = repository.findByTokenHash(tokenHash);
        return tokenOpt.filter(RefreshToken::isRevoked).map(RefreshToken::getFamilyId).orElse(null);
    }
}