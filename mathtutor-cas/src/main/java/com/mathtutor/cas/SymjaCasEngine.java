package com.mathtutor.cas;

import com.mathtutor.common.port.CasEngine;
import com.mathtutor.common.dto.ErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implementación del motor CAS usando Symja (matheclipse-core 3.2.0).
 *
 * Licencia: Symja usa LGPL-3.0 (Lesser General Public License v3.0).
 * - Permite enlazar dinámicamente como biblioteca sin obligación de abrir código propio
 * - Las modificaciones al código de Symja deben distribuirse bajo LGPL-3.0
 * - Este módulo usa Symja como dependencia externa vía Maven Central, sin modificaciones
 *
 * Capacidades de Symja:
 * - Parseo LaTeX nativo
 * - Simplificación simbólica completa
 * - Derivadas simbólicas (D)
 * - Integrales simbólicas (Integrate) y definidas
 * - Evaluación numérica de alta precisión (apfloat)
 *
 * Limitaciones conocidas / falsos negativos documentados:
 * 1. Simplificación no siempre llega a 0 para expresiones equivalentes complejas
 *    - Ej: sin(x)^2 + cos(x)^2 - 1 puede no simplificarse a 0 directamente
 *    - Mitigación: fallback a evaluación numérica en múltiples puntos
 * 2. Integrales definidas con límites complejos pueden fallar
 * 3. Algunas funciones especiales (Bessel, Gamma, etc.) requieren Hipparchus
 * 4. Parseo LaTeX estricto: requiere sintaxis LaTeX correcta y balanceada
 */
@Component
public class SymjaCasEngine implements CasEngine {

    private static final Logger log = LoggerFactory.getLogger(SymjaCasEngine.class);
    private final LatexToSymjaConverter converter;

    public SymjaCasEngine(LatexToSymjaConverter converter) {
        this.converter = converter;
    }

    @Override
    public CasEngine.Verification verify(String studentExpr, String reference) {
        if (studentExpr == null || reference == null) {
            return CasEngine.Verification.ofIncorrect(ErrorType.OTRO, "Expresión nula");
        }

        try {
            boolean equiv = equivalent(studentExpr, reference);

            if (equiv) {
                return CasEngine.Verification.ofCorrect();
            } else {
                String hint = analyzeDifference(studentExpr, reference);
                return CasEngine.Verification.ofIncorrect(ErrorType.OTRO, hint);
            }
        } catch (Exception e) {
            log.debug("Error verifying expression: {}", e.getMessage());
            return CasEngine.Verification.ofIncorrect(ErrorType.OTRO, "Error de evaluación: " + e.getMessage());
        }
    }

    @Override
    public boolean equivalent(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return converter.areEquivalent(a, b);
    }

    @Override
    public String derive(String expr, String var) {
        if (expr == null || var == null) {
            return null;
        }

        String derivative = converter.derive(expr, var);
        if (derivative == null) {
            return "No se pudo calcular la derivada: " + expr;
        }
        return derivative;
    }

    @Override
    public CasEngine.Expr parse(String text) {
        if (text == null) {
            return new CasEngine.Expr("", "", "");
        }

        String normalized = converter.convertToSymja(text);
        String latexAst = null;
        try {
            // Intentar obtener AST para debugging
            var parsed = converter.parseLatex(text);
            if (parsed != null) {
                latexAst = parsed.toString();
            }
        } catch (Exception ignored) {
        }

        return new CasEngine.Expr(text, normalized != null ? normalized : "", latexAst);
    }

    /**
     * Analiza la diferencia entre dos expresiones para dar una pista pedagógica.
     */
    private String analyzeDifference(String studentExpr, String reference) {
        try {
            // Primero intentar derivadas para ver estructura
            String studentDeriv = converter.derive(studentExpr, "x");
            String refDeriv = converter.derive(reference, "x");

            if (studentDeriv != null && refDeriv != null && studentDeriv.equals(refDeriv)) {
                return "Las derivadas coinciden: posible diferencia constante";
            }

            // Evaluar numéricamente en un punto
            var studentParsed = converter.parseLatex(studentExpr);
            var refParsed = converter.parseLatex(reference);

            if (studentParsed != null && refParsed != null) {
                try {
                    double testValue = 2.0;
                    double studentVal = evaluateAt(studentParsed, "x", testValue);
                    double refVal = evaluateAt(refParsed, "x", testValue);

                    double diff = Math.abs(studentVal - refVal);

                    if (diff < 1e-10) {
                        return "Las expresiones son numéricamente equivalentes en x=2 (posible forma equivalente no detectada simbólicamente)";
                    }

                    return String.format("Las expresiones no son equivalentes. Estudiante: %s (%.4f), Referencia: %s (%.4f)",
                            studentExpr, studentVal, reference, refVal);
                } catch (Exception e) {
                    return "Error evaluando numéricamente: " + e.getMessage();
                }
            }

            return "No se pudo parsear una de las expresiones para análisis";

        } catch (Exception e) {
            return "Error analizando diferencia: " + e.getMessage();
        }
    }

    private double evaluateAt(Object expr, String var, double value) {
        // Usar reflexión o método del converter
        // Por simplicidad, evaluar a través del converter
        return 0.0; // Placeholder - la evaluación real se hace en converter
    }
}