package com.mathtutor.common.auth.port;

import java.util.Map;

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