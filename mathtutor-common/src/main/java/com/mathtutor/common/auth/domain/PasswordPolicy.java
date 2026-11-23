package com.mathtutor.common.auth.domain;

import java.util.Optional;

/** @brief Aplica los mismos requisitos de contraseña al registro y a la recuperación. */
public final class PasswordPolicy {
    private PasswordPolicy() {}

    /**
     * @param password Contraseña propuesta.
     * @return Motivo de rechazo o vacío si cumple la política.
     */
    public static Optional<String> violation(String password) {
        if (password == null) return Optional.of("Contraseña es obligatoria");
        if (password.length() < 12) return Optional.of("La contraseña debe tener al menos 12 caracteres");
        if (!password.matches(".*[A-Z].*")) return Optional.of("La contraseña debe contener al menos una mayúscula");
        if (!password.matches(".*[a-z].*")) return Optional.of("La contraseña debe contener al menos una minúscula");
        if (!password.matches(".*\\d.*")) return Optional.of("La contraseña debe contener al menos un dígito");
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            return Optional.of("La contraseña debe contener al menos un carácter especial");
        }
        return Optional.empty();
    }
}
