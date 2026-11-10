package com.mathtutor.persistence.adapter;

import com.mathtutor.common.auth.port.TokenServicePort;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementación de TokenServicePort usando JJWT (Java JWT).
 * Genera y valida tokens JWT para autenticación stateless.
 */
@Component
public class TokenServiceAdapter implements TokenServicePort {

    // En producción, esto debería venir de configuración externa (Spring Vault, env vars)
    @Value("${jwt.secret:change-me-in-production-use-at-least-256-bits-secret-key-for-hs256}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration-minutes:15}")
    private long accessTokenExpirationMinutes;

    @Value("${jwt.refresh-token-expiration-days:7}")
    private long refreshTokenExpirationDays;

    private SecretKey secretKey;

    // Almacén en memoria para tokens de refresco revocados (en producción usar Redis/BD)
    private final Map<String, Instant> revokedRefreshTokens = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // Validar que la clave tiene al menos 256 bits (32 bytes) para HS256
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "JWT secret must be at least 256 bits (32 characters). "
                    + "Set jwt.secret in application.properties or environment variable."
            );
        }
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String generateAccessToken(Long userId, String email, String role) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(accessTokenExpirationMinutes * 60);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    @Override
    public String generateRefreshToken(Long userId, String email, String role, String tokenId) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(refreshTokenExpirationDays * 24 * 60 * 60);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role)
                .id(tokenId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    @Override
    public Map<String, Object> validateToken(String token) throws InvalidTokenException {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Verificar si es un refresh token revocado
            String tokenId = claims.getId();
            if (tokenId != null && revokedRefreshTokens.containsKey(tokenId)) {
                throw new InvalidTokenException("Refresh token has been revoked");
            }

            Map<String, Object> result = new java.util.HashMap<>();
            result.put("userId", Long.valueOf(claims.getSubject()));
            result.put("email", claims.get("email", String.class));
            result.put("role", claims.get("role", String.class));
            result.put("tokenId", tokenId);
            result.put("issuedAt", claims.getIssuedAt());
            result.put("expiresAt", claims.getExpiration());
            return result;
        } catch (ExpiredJwtException e) {
            throw new InvalidTokenException("Token has expired", e);
        } catch (JwtException e) {
            throw new InvalidTokenException("Invalid token: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> extractClaims(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("userId", Long.valueOf(claims.getSubject()));
        result.put("email", claims.get("email", String.class));
        result.put("role", claims.get("role", String.class));
        result.put("tokenId", claims.getId());
        result.put("issuedAt", claims.getIssuedAt());
        result.put("expiresAt", claims.getExpiration());
        return result;
    }

    /**
     * Revoca un token de refresco (para logout o rotación de tokens).
     * En producción, esto debería persistirse en Redis o BD con TTL.
     *
     * @param tokenId ID del token a revocar
     */
    public void revokeRefreshToken(String tokenId) {
        if (tokenId != null) {
            revokedRefreshTokens.put(tokenId, Instant.now().plusSeconds(refreshTokenExpirationDays * 24 * 60 * 60));
        }
    }

    /**
     * Limpia tokens revocados expirados (para evitar fuga de memoria).
     * En producción, esto se manejaría automáticamente por TTL en Redis.
     */
    public void cleanupRevokedTokens() {
        Instant now = Instant.now();
        revokedRefreshTokens.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }
}