package com.mathtutor.cas;

/**
 * Resultado de tres estados para la verificación de equivalencia matemática.
 *
 * <p>Estados posibles:
 * <ul>
 *   <li>{@code EQUIVALENTE}: Las expresiones son matemáticamente equivalentes (simbólicamente o numéricamente)</li>
 *   <li>{@code NO_EQUIVALENTE}: Las expresiones son definitivamente diferentes (diferencia > tolerancia o diferencia constante no nula)</li>
 *   <li>{@code INDETERMINADO}: No se puede determinar con certeza (dominios disjuntos, pocos puntos válidos, error en evaluación)</li>
 * </ul>
 *
 * <p>Este enfoque evita falsos positivos en la evaluación numérica:
 * - Diferencias constantes pequeñas (ej: x vs x+1e-10) → NO_EQUIVALENTE
 * - Dominios que solo se tocan en 1 punto (ej: sqrt(x) vs sqrt(-x)) → INDETERMINADO
 * - Errores de parseo/evaluación → INDETERMINADO
 */
public enum EquivalenceResult {
    /**
     * Las expresiones son matemáticamente equivalentes.
     * Verificado por simplificación simbólica (diferencia = 0) o evaluación numérica
     * en múltiples puntos con diferencia ≈ 0 y sin diferencia constante.
     */
    EQUIVALENTE,

    /**
     * Las expresiones son definitivamente NO equivalentes.
     * Diferencia > tolerancia en algún punto, o diferencia constante no nula detectada.
     */
    NO_EQUIVALENTE,

    /**
     * No se puede determinar la equivalencia con confianza.
     * Pocos puntos de evaluación válida (< 2), dominios disjuntos,
     * error en parseo, o evaluación numérica fallida.
     */
    INDETERMINADO;

    /**
     * Convierte a boolean para compatibilidad con código existente.
     * {@code EQUIVALENTE} → true, {@code NO_EQUIVALENTE} y {@code INDETERMINADO} → false.
     */
    public boolean toBoolean() {
        return this == EQUIVALENTE;
    }

    /**
     * Verifica si el resultado es definitivo (no INDETERMINADO).
     */
    public boolean isDefinitive() {
        return this != INDETERMINADO;
    }
}