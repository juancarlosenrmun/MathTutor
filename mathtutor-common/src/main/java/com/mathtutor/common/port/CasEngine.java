package com.mathtutor.common.port;

import com.mathtutor.common.dto.ErrorType;

/**
 * Puerto para el motor de álgebra computacional (CAS).
 * Permite verificar expresiones, derivar, comprobar equivalencia.
 */
public interface CasEngine {

    /**
     * Verifica si la expresión del estudiante es equivalente a la de referencia.
     *
     * @param studentExpr expresión del estudiante
     * @param reference   expresión de referencia
     * @return resultado de la verificación
     */
    Verification verify(String studentExpr, String reference);

    /**
     * Comprueba si dos expresiones son matemáticamente equivalentes.
     *
     * @param a primera expresión
     * @param b segunda expresión
     * @return true si son equivalentes
     */
    boolean equivalent(String a, String b);

    /**
     * Deriva una expresión respecto a una variable.
     *
     * @param expr expresión a derivar
     * @param var  variable de derivación
     * @return expresión derivada
     */
    String derive(String expr, String var);

    /**
     * Parsea una expresión matemática.
     *
     * @param text texto de la expresión
     * @return expresión parseada
     */
    Expr parse(String text);

    /**
     * Resultado de la verificación CAS.
     */
    record Verification(boolean correct, ErrorType errorType, String hint) {
        public static Verification ofCorrect() {
            return new Verification(true, ErrorType.NINGUNO, null);
        }

        public static Verification ofIncorrect(ErrorType errorType, String hint) {
            return new Verification(false, errorType, hint);
        }
    }

    /**
     * Expresión parseada.
     */
    record Expr(String original, String normalized, String ast) {}
}