package com.mathtutor.auth.adapter;

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
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementación de TokenServicePort usando JJWT (Java JWT).
 * Genera y valida tokens JWT para autenticación stateless.
 * Los refresh tokens se almacenan en BD con rotación y detección de reuso vía RefreshTokenAdapter.
 */
@Component
public class TokenServiceAdapter implements TokenServicePort {

    private static final com.fasterxml.jackson.databind.ObjectMapper JSON = new com.fasterxml.jackson.databind.ObjectMapper();

    // En producción, esto debería venir de configuración externa (Spring Vault, env vars)
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration-minutes:15}")
    private long accessTokenExpirationMinutes = 15;

    @Value("${jwt.refresh-token-expiration-days:7}")
    private long refreshTokenExpirationDays = 7;

    private SecretKey secretKey;
    private final RefreshTokenAdapter refreshTokenAdapter;

    public TokenServiceAdapter(RefreshTokenAdapter refreshTokenAdapter) {
        this.refreshTokenAdapter = refreshTokenAdapter;
    }

    @PostConstruct
    public void init() {
        // Validar que la clave tiene al menos 256 bits (32 bytes) para HS256
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                    "JWT secret not configured. Set 'jwt.secret' environment variable (JWT_SECRET). "
                    + "Must be at least 256 bits (32 chars) for HS256."
            );
        }
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "JWT secret must be at least 256 bits (32 characters). "
                    + "Current length: " + jwtSecret.getBytes(StandardCharsets.UTF_8).length + " bytes."
            );
        }
        if (accessTokenExpirationMinutes <= 0 || accessTokenExpirationMinutes > Integer.MAX_VALUE / 60
                || refreshTokenExpirationDays <= 0 || refreshTokenExpirationDays > Integer.MAX_VALUE / 86400) {
            throw new IllegalStateException("Las duraciones de los tokens deben ser positivas y estar acotadas");
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

            // Un refresh token nunca autoriza peticiones de recursos.
            String tokenId = claims.getId();
            if (tokenId != null) {
                throw new InvalidTokenException("Refresh token cannot be used as an access token");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("userId", Long.valueOf(claims.getSubject()));
            result.put("email", claims.get("email", String.class));
            result.put("role", claims.get("role", String.class));
            result.put("tokenId", tokenId);
            result.put("issuedAt", claims.getIssuedAt());
            result.put("expiresAt", claims.getExpiration());
            return result;
        } catch (ExpiredJwtException e) {
            throw new InvalidTokenException("Token has expired", e);
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("Invalid token: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> extractClaims(String token) {
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT format");
        }
        try {
            Map<String, Object> payload = JSON.readValue(
                    Base64.getUrlDecoder().decode(parts[1]),
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            Map<String, Object> result = new HashMap<>();
            if (payload.get("sub") instanceof String subject) {
                result.put("userId", Long.valueOf(subject));
            }
            result.put("email", payload.get("email"));
            result.put("role", payload.get("role"));
            result.put("tokenId", payload.get("jti"));
            if (payload.get("iat") instanceof Number issuedAt) {
                result.put("issuedAt", Date.from(Instant.ofEpochSecond(issuedAt.longValue())));
            }
            if (payload.get("exp") instanceof Number expiresAt) {
                result.put("expiresAt", Date.from(Instant.ofEpochSecond(expiresAt.longValue())));
            }
            return result;
        } catch (java.io.IOException | IllegalArgumentException invalid) {
            throw new IllegalArgumentException("Invalid JWT payload", invalid);
        }
    }

    @Override
    public String storeRefreshToken(Long userId, String email, String role, String rawToken) {
        // Generar tokenId único para este refresh token
        String tokenId = UUID.randomUUID().toString();
        // Generar el JWT refresh token
        String refreshToken = generateRefreshToken(userId, email, role, tokenId);
        // Almacenar en BD (hash + metadatos) - returns the refresh token
        return refreshTokenAdapter.storeRefreshToken(userId, email, role, refreshToken);
    }

    @Override
    public String storeRefreshTokenWithFamily(Long userId, String email, String role, String rawToken, String familyId) {
        // Generar tokenId único para este refresh token
        String tokenId = UUID.randomUUID().toString();
        // Generar el JWT refresh token
        String refreshToken = generateRefreshToken(userId, email, role, tokenId);
        // Almacenar en BD con el familyId específico - returns the refresh token
        return refreshTokenAdapter.storeRefreshTokenWithFamily(userId, email, role, refreshToken, familyId);
    }

    /** Rota y consume el refresh bajo el bloqueo transaccional del adaptador. */
    public Optional<TokenRotation> rotateRefreshToken(String rawToken) {
        return refreshTokenAdapter.rotateRefreshToken(rawToken, user -> generateRefreshToken(
                user.userId(), user.email(), user.role(), UUID.randomUUID().toString()))
                .map(rotation -> {
                    var user = rotation.user();
                    return new TokenRotation(generateAccessToken(user.userId(), user.email(), user.role()),
                            rotation.refreshToken(), user.email(), user.role());
                });
    }

    public void revokeRefreshSession(String rawToken) {
        refreshTokenAdapter.revokeRefreshSession(rawToken);
    }

    public record TokenRotation(String accessToken, String refreshToken, String email, String role) {}

    @Override
    public Optional<TokenValidationResult> validateRefreshToken(String rawToken) {
        return refreshTokenAdapter.validateRefreshToken(rawToken);
    }

    @Override
    public void revokeRefreshToken(String tokenId) {
        refreshTokenAdapter.revokeRefreshToken(tokenId);
    }

    @Override
    public void revokeTokenFamily(String familyId) {
        refreshTokenAdapter.revokeTokenFamily(familyId);
    }

    @Override
    public void cleanupExpiredTokens() {
        refreshTokenAdapter.cleanupExpiredTokens();
    }

    @Override
    public boolean isRefreshTokenRevoked(String rawToken) {
        return refreshTokenAdapter.isRefreshTokenRevoked(rawToken);
    }

    @Override
    public String getFamilyIdForRevokedToken(String rawToken) {
        return refreshTokenAdapter.getFamilyIdForRevokedToken(rawToken);
    }
}