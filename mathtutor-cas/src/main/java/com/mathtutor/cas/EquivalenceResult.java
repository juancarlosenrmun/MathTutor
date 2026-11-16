package com.mathtutor.cas;

/** Resultado conservador de la verificación de equivalencia matemática. */
public enum EquivalenceResult {
    /** Identidad del AST o diferencia simbólica simplificada exactamente a cero. */
    EQUIVALENTE,

    /** Un contraejemplo numérico demuestra una diferencia mayor que la tolerancia. */
    NO_EQUIVALENTE,

    /** La simplificación y los puntos disponibles no permiten una conclusión. */
    INDETERMINADO;

    /** Solo una prueba positiva de equivalencia se convierte en true. */
    public boolean toBoolean() {
        return this == EQUIVALENTE;
    }

    public boolean isDefinitive() {
        return this != INDETERMINADO;
    }
}