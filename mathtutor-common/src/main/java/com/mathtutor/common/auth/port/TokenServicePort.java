package com.mathtutor.common.auth.port;

import java.util.Map;
import java.util.Optional;

/**
 * Puerto para servicio de tokens JWT.
 * Permite generar, validar y extraer claims de tokens sin acoplar a implementación específica.
 */
public interface TokenServicePort {

    /**
     * Genera un access token de corta duración (ej. 15 min).
     *
     * @param userId  ID del usuario
     * @param email   email del usuario
     * @param role    rol del usuario
     * @return access token JWT
     */
    String generateAccessToken(Long userId, String email, String role);

    /**
     * Genera un refresh token de larga duración (ej. 7 días).
     *
     * @param userId  ID del usuario
     * @param email   email del usuario
     * @param role    rol del usuario
     * @param tokenId ID único del token (para rotación/revocación)
     * @return refresh token JWT
     */
    String generateRefreshToken(Long userId, String email, String role, String tokenId);

    /**
     * Valida un token y retorna sus claims si es válido.
     *
     * @param token token a validar
     * @return claims del token
     * @throws InvalidTokenException si el token es inválido o expirado
     */
    Map<String, Object> validateToken(String token) throws InvalidTokenException;

    /**
     * Extrae claims de un token sin validar firma (para debugging/refresh).
     *
     * @param token token del que extraer claims
     * @return claims del token
     */
    Map<String, Object> extractClaims(String token);

    /**
     * Almacena un refresh token en la base de datos (hash + metadatos).
     * Genera un familyId nuevo para tracking de rotación.
     *
     * @param userId     ID del usuario
     * @param email      email del usuario
     * @param role       rol del usuario
     * @param rawToken   token de refresco en claro (se hasheará con Argon2id)
     * @return refresh token JWT (el mismo que se pasó como rawToken)
     */
    String storeRefreshToken(Long userId, String email, String role, String rawToken);

    /**
     * Almacena un refresh token en la base de datos con un familyId específico (para rotación).
     *
     * @param userId       ID del usuario
     * @param email        email del usuario
     * @param role         rol del usuario
     * @param rawToken     token de refresco en claro
     * @param familyId     ID de familia existente (para mantener la cadena de rotación)
     * @return refresh token JWT
     */
    String storeRefreshTokenWithFamily(Long userId, String email, String role, String rawToken, String familyId);

    /**
     * Valida un refresh token contra la base de datos.
     *
     * @param rawToken token de refresco en claro
     * @return resultado de validación con metadatos (userId, email, role, familyId, tokenId)
     */
    Optional<TokenValidationResult> validateRefreshToken(String rawToken);

    /**
     * Revoca un refresh token individual (logout o rotación normal).
     *
     * @param tokenId ID del token a revocar (jti del JWT)
     */
    void revokeRefreshToken(String tokenId);

    /**
     * Revoca toda una familia de tokens (detección de reuso - ataque de replay).
     *
     * @param familyId ID de la familia a revocar
     */
    void revokeTokenFamily(String familyId);

    /**
     * Limpia tokens de refresco expirados de la base de datos.
     */
    void cleanupExpiredTokens();

    /**
     * Verifica si un refresh token existe en BD pero está revocado (indicando reuso).
     *
     * @param rawToken token de refresco en claro
     * @return true si el token existe y está revocado
     */
    boolean isRefreshTokenRevoked(String rawToken);

    /**
     * Obtiene el familyId de un refresh token revocado (para revocar la familia completa).
     *
     * @param rawToken token de refresco en claro
     * @return familyId si el token existe y está revocado, null en caso contrario
     */
    String getFamilyIdForRevokedToken(String rawToken);

    /**
     * Resultado de validación de refresh token.
     */
    record TokenValidationResult(
            Long userId,
            String email,
            String role,
            String familyId,
            String tokenId
    ) {}

    /**
     * Excepción para tokens inválidos.
     */
    class InvalidTokenException extends Exception {
        public InvalidTokenException(String message) {
            super(message);
        }

        public InvalidTokenException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}