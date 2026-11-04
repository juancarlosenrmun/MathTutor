package com.mathtutor.common.port;

/**
 * Puerto para codificación de contraseñas (Argon2id/BCrypt).
 * Permite cambiar la implementación sin afectar al dominio.
 */
public interface PasswordEncoderPort {

    /**
     * Codifica una contraseña en texto plano.
     *
     * @param rawPassword contraseña en texto plano
     * @return hash de la contraseña
     */
    String encode(CharSequence rawPassword);

    /**
     * Verifica si una contraseña en texto plano coincide con el hash.
     *
     * @param rawPassword     contraseña en texto plano
     * @param encodedPassword hash almacenado
     * @return true si coinciden
     */
    boolean matches(CharSequence rawPassword, String encodedPassword);
}