package com.mathtutor.common.port;

import com.mathtutor.common.dto.Role;

/**
 * Puerto para validación de entrada del usuario.
 */
public interface InputGuard {

    /**
     * Valida un mensaje del estudiante.
     *
     * @param message mensaje del estudiante
     * @param role    rol del usuario
     * @return resultado de la validación
     */
    GuardResult validate(String message, Role role);

    /**
     * Resultado de la validación de entrada.
     */
    record GuardResult(boolean valid, String errorCode, String errorMessage) {
        public static GuardResult ofValid() {
            return new GuardResult(true, null, null);
        }

        public static GuardResult ofInvalid(String errorCode, String errorMessage) {
            return new GuardResult(false, errorCode, errorMessage);
        }
    }
}