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

    // En producción, esto debería venir de configuración externa (Spring Vault, env vars)
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration-minutes:15}")
    private long accessTokenExpirationMinutes;

    @Value("${jwt.refresh-token-expiration-days:7}")
    private long refreshTokenExpirationDays;

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

            // Verificar si es un refresh token revocado en BD
            String tokenId = claims.getId();
            if (tokenId != null) {
                // Para refresh tokens, delegar a RefreshTokenAdapter
                var validationResult = refreshTokenAdapter.validateRefreshToken(token);
                if (validationResult.isEmpty()) {
                    throw new InvalidTokenException("Refresh token has been revoked or is invalid");
                }
                // Token válido, continuar con claims normales
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
        } catch (JwtException e) {
            throw new InvalidTokenException("Invalid token: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> extractClaims(String token) {
        // Decode JWT payload (middle part) without verifying signature
        // JWT format: header.payload.signature
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT format");
        }

        String payload = parts[1];
        // Base64URL decode (add padding if needed)
        String padded = payload + "=".repeat((4 - payload.length() % 4) % 4);
        byte[] decoded = Base64.getUrlDecoder().decode(padded);
        String json = new String(decoded, StandardCharsets.UTF_8);

        // Parse JSON manually (simple approach for known claims)
        Map<String, Object> result = new HashMap<>();
        // Extract userId (subject)
        String subject = extractJsonValue(json, "sub");
        if (subject != null) {
            result.put("userId", Long.valueOf(subject));
        }
        result.put("email", extractJsonValue(json, "email"));
        result.put("role", extractJsonValue(json, "role"));
        result.put("tokenId", extractJsonValue(json, "jti"));
        // issuedAt and expiresAt are numeric timestamps
        String iat = extractJsonValue(json, "iat");
        if (iat != null) {
            result.put("issuedAt", Date.from(Instant.ofEpochSecond(Long.parseLong(iat))));
        }
        String exp = extractJsonValue(json, "exp");
        if (exp != null) {
            result.put("expiresAt", Date.from(Instant.ofEpochSecond(Long.parseLong(exp))));
        }
        return result;
    }

    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int start = json.indexOf(searchKey);
        if (start == -1) return null;
        start = json.indexOf(":", start);
        if (start == -1) return null;
        start++;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        if (start >= json.length()) return null;
        if (json.charAt(start) == '"') {
            start++;
            int end = json.indexOf("\"", start);
            if (end == -1) return null;
            return json.substring(start, end);
        } else {
            int end = start;
            while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
            return json.substring(start, end);
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