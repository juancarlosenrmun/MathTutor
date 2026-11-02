package com.mathtutor.common.port;

import com.mathtutor.common.dto.GraphicSpec;

/**
 * Puerto para validar especificaciones gráficas.
 * Valida que la expresión es matemáticamente correcta usando CAS.
 */
public interface GraphicSpecValidator {

    /**
     * Valida una especificación gráfica.
     *
     * @param spec especificación a validar
     * @return resultado de la validación
     */
    ValidationResult validate(GraphicSpec spec);

    /**
     * Resultado de la validación.
     */
    record ValidationResult(boolean valid, String errorMessage) {
        public static ValidationResult ofValid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult ofInvalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }
    }
}