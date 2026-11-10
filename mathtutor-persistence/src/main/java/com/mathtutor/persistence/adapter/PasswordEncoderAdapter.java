package com.mathtutor.persistence.adapter;

import com.mathtutor.common.port.PasswordEncoderPort;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import org.springframework.stereotype.Component;

/**
 * Implementación de PasswordEncoderPort usando Argon2id.
 * Parámetros seguros según OWASP: memoria=64MB, iteraciones=3, paralelismo=4.
 */
@Component
public class PasswordEncoderAdapter implements PasswordEncoderPort {

    private static final Argon2 ARGON2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);
    private static final int MEMORY_KIB = 65536; // 64 MB
    private static final int ITERATIONS = 3;
    private static final int PARALLELISM = 4;

    @Override
    public String encode(CharSequence rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        if (rawPassword.length() == 0) {
            throw new IllegalArgumentException("Password cannot be empty");
        }

        return ARGON2.hash(ITERATIONS, MEMORY_KIB, PARALLELISM, rawPassword.toString());
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null || encodedPassword.isEmpty()) {
            return false;
        }

        try {
            return ARGON2.verify(encodedPassword, rawPassword.toString());
        } catch (IllegalArgumentException e) {
            // Hash inválido o formato incorrecto
            return false;
        }
    }
}