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

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador para almacenamiento de refresh tokens en base de datos.
 * Implementa rotación de tokens con family tracking y detección de reuso (revocación de familia completa).
 */
@Component
public class RefreshTokenAdapter {

    private final RefreshTokenRepository repository;
    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoder;

    public RefreshTokenAdapter(RefreshTokenRepository repository, UserRepository userRepository, PasswordEncoderPort passwordEncoder) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
        // Hash del token con Argon2id
        String tokenHash = passwordEncoder.encode(rawToken);

        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(7 * 24 * 60 * 60); // 7 días

        RefreshToken entity = new RefreshToken(tokenHash, userId, familyId, expiresAt, now);
        repository.save(entity);
        return rawToken; // Devolver el token en claro para que el cliente lo reciba
    }

    /**
     * Valida un refresh token contra la BD.
     * Verifica: hash coincide, no revocado, no expirado.
     */
    @Transactional(readOnly = true)
    public Optional<TokenServicePort.TokenValidationResult> validateRefreshToken(String rawToken) {
        // Buscar todos los tokens y verificar hash
        var allTokens = repository.findAll();

        for (var token : allTokens) {
            try {
                if (passwordEncoder.matches(rawToken, token.getTokenHash())) {
                    // Verificar no revocado
                    if (token.isRevoked()) {
                        // DETECCIÓN DE REUSO: Token ya revocado pero se intenta usar -> revocar familia completa
                        revokeTokenFamily(token.getFamilyId());
                        return Optional.empty();
                    }
                    // Verificar no expirado
                    if (token.getExpiresAt().isBefore(Instant.now())) {
                        return Optional.empty();
                    }

                    // Obtener email y role del usuario
                    var userOpt = userRepository.findById(token.getUserId());
                    String email = userOpt.map(User::getEmail).orElse("");
                    String role = userOpt.map(u -> u.getRole().name()).orElse("");

                    return Optional.of(new TokenServicePort.TokenValidationResult(
                        token.getUserId(),
                        email,
                        role,
                        token.getFamilyId(),
                        token.getId().toString()
                    ));
                }
            } catch (Exception e) {
                // Hash inválido, continuar con siguiente
            }
        }
        return Optional.empty();
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
}