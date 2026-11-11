package com.mathtutor.cas;

import org.matheclipse.core.eval.ExprEvaluator;
import org.matheclipse.core.eval.exception.SymjaMathException;
import org.matheclipse.core.form.tex.TeXParser;
import org.matheclipse.core.interfaces.IExpr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Conversor de LaTeX a Symja usando el parser nativo de Symja.
 *
 * Licencia: Symja (matheclipse-core 3.2.0) usa LGPL-3.0.
 * - Requiere que las modificaciones al código de Symja se distribuyan bajo LGPL-3.0
 * - Permite enlazar dinámicamente (uso como biblioteca) sin obligación de abrir código propio
 * - Este módulo usa Symja como biblioteca externa (vía Maven Central), sin modificaciones
 *
 * Symja proporciona:
 * - Parseo LaTeX nativo via TeXParser.parse() / TeXParser.convert()
 * - Simplificación simbólica completa via IExpr.normal(), together(), cancel()
 * - Derivadas simbólicas via IExpr.diff()
 * - Integrales simbólicas via ExprEvaluator.eval("Integrate(...)")
 * - Evaluación numérica de alta precisión (apfloat) via IExpr.evalf()
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
public class LatexToSymjaConverter {

    private static final Logger log = LoggerFactory.getLogger(LatexToSymjaConverter.class);

    private final TeXParser texParser = new TeXParser();
    private final ExprEvaluator evaluator = new ExprEvaluator();

    /**
     * Convierte una expresión LaTeX a formato Symja (AST evaluable).
     *
     * @param latex Expresión en formato LaTeX
     * @return Expresión Symja parseada, o null si falla el parseo
     */
    public IExpr parseLatex(String latex) {
        if (latex == null || latex.trim().isEmpty()) {
            return null;
        }

        String cleanLatex = latex.trim();

        try {
            // Symja puede parsear LaTeX directamente via TeXParser
            IExpr expr = texParser.parse(cleanLatex);
            log.debug("LaTeX -> Symja AST: '{}' -> {}", cleanLatex, expr);
            return expr;
        } catch (Exception e) {
            log.debug("Error parsing LaTeX '{}': {}", cleanLatex, e.getMessage());
            return null;
        }
    }

    /**
     * Convierte LaTeX a string Symja (para debugging/logging).
     */
    public String convertToSymja(String latex) {
        IExpr expr = parseLatex(latex);
        return expr != null ? expr.toString() : null;
    }

    /**
     * Alias para compatibilidad con tests.
     */
    public String convert(String latex) {
        return convertToSymja(latex);
    }

    /**
     * Verifica si dos expresiones LaTeX son matemáticamente equivalentes.
     * Usa simplificación simbólica de la diferencia (a - b = 0).
     *
     * @param latex1 Primera expresión LaTeX
     * @param latex2 Segunda expresión LaTeX
     * @return true si son equivalentes, false si no lo son o hay error
     */
    public boolean areEquivalent(String latex1, String latex2) {
        IExpr expr1 = parseLatex(latex1);
        IExpr expr2 = parseLatex(latex2);

        if (expr1 == null || expr2 == null) {
            // Fallback: comparación string si no se puede parsear
            return latex1 != null && latex1.equals(latex2);
        }

        try {
            // Optimización: si los ASTs son idénticos
            if (expr1.equals(expr2)) {
                return true;
            }

            // Simbólicamente: simplificar (expr1 - expr2) y verificar si es 0
            IExpr diff = expr1.subtract(expr2);
            IExpr simplified = diff.normal(true);

            // Verificar si la simplificación es exactamente 0
            if (simplified.isZero()) {
                return true;
            }

            // También intentar together() para combinar fracciones
            IExpr together = diff.together();
            if (together.isZero()) {
                return true;
            }

            // Si no es exactamente 0, intentar evaluación numérica en varios puntos
            // como respaldo para casos donde la simplificación no llega a 0
            return evaluateNumerically(expr1, expr2);

        } catch (Exception e) {
            log.debug("Error checking equivalence symbolically: {}", e.getMessage());
            // Fallback a evaluación numérica
            return evaluateNumerically(expr1, expr2);
        }
    }

    /**
     * Evalúa dos expresiones numéricamente en varios puntos.
     * Rechaza diferencias constantes (ej: x vs x+1e-10) y dominios disjuntos.
     */
    private boolean evaluateNumerically(IExpr expr1, IExpr expr2) {
        try {
            double[] testPoints = {-2.0, -1.0, -0.5, 0.0, 0.5, 1.0, 2.0, 3.0, 5.0};
            boolean anyValidEvaluation = false;
            double diffSum = 0.0;
            int validCount = 0;

            for (double x : testPoints) {
                try {
                    // Sustituir x por el valor de prueba
                    IExpr xExpr = evaluator.defineVariable("x", x);
                    IExpr sub1 = expr1.subs(xExpr, evaluator.parse(String.valueOf(x)));
                    IExpr sub2 = expr2.subs(xExpr, evaluator.parse(String.valueOf(x)));

                    double val1 = sub1.evalf();
                    double val2 = sub2.evalf();

                    if (Double.isNaN(val1) || Double.isNaN(val2) || Double.isInfinite(val1) || Double.isInfinite(val2)) {
                        continue;
                    }

                    anyValidEvaluation = true;
                    double diff = val1 - val2;
                    diffSum += diff;
                    validCount++;

                    // Diferencia absoluta > tolerancia en CUALQUIER punto → no equivalente
                    if (Math.abs(diff) > 1e-9) {
                        return false;
                    }
                } catch (Exception e) {
                    // Punto no evaluable (ej: sqrt(-1)) → saltar, no cuenta como válido
                    continue;
                }
            }

            // Debe haber al menos 2 puntos de evaluación válida para considerar equivalente
            // Esto evita falsos positivos con dominios disjuntos (ej: sqrt(x) vs sqrt(-x) solo x=0)
            if (validCount < 2) {
                return false;
            }

            // Rechazar diferencia constante no nula (ej: x vs x + 1e-10 en todos los puntos)
            // Si la diferencia es constante y ≠ 0, no son equivalentes
            double avgDiff = diffSum / validCount;
            if (Math.abs(avgDiff) > 1e-12) {
                // Verificar si la diferencia es constante en todos los puntos válidos
                // (si la varianza de las diferencias es ~0)
                // Para simplificar: si avgDiff ≠ 0, no son equivalentes
                return false;
            }

            return true;

        } catch (Exception e) {
            log.debug("Error in numeric evaluation: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Calcula la derivada simbólica de una expresión LaTeX respecto a una variable.
     *
     * @param latex Expresión LaTeX
     * @param variable Variable respecto a la cual derivar (ej. "x")
     * @return Derivada en formato Symja string, o null si falla
     */
    public String derive(String latex, String variable) {
        IExpr expr = parseLatex(latex);
        if (expr == null) {
            return null;
        }

        try {
            IExpr var = evaluator.defineVariable(variable);
            IExpr derivative = expr.diff(var);
            log.debug("Derivative of '{}' wrt '{}': {}", latex, variable, derivative);
            return derivative.toString();
        } catch (Exception e) {
            log.debug("Error computing derivative: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Calcula la integral simbólica (indefinida) de una expresión LaTeX.
     *
     * @param latex Expresión LaTeX
     * @param variable Variable de integración (ej. "x")
     * @return Integral en formato Symja string, o null si falla
     */
    public String integrate(String latex, String variable) {
        IExpr expr = parseLatex(latex);
        if (expr == null) {
            return null;
        }

        try {
            // Symja usa Integrate(expr, var) para integrales indefinidas
            String integrateCmd = "Integrate(" + expr.toString() + ", " + variable + ")";
            IExpr integral = evaluator.eval(integrateCmd);
            log.debug("Integral of '{}' wrt '{}': {}", latex, variable, integral);
            return integral.toString();
        } catch (Exception e) {
            log.debug("Error computing integral: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Calcula la integral definida de una expresión LaTeX.
     *
     * @param latex Expresión LaTeX
     * @param variable Variable de integración
     * @param lower Límite inferior
     * @param upper Límite superior
     * @return Valor numérico de la integral definida, o null si falla
     */
    public Double integrateDefinite(String latex, String variable, double lower, double upper) {
        IExpr expr = parseLatex(latex);
        if (expr == null) {
            return null;
        }

        try {
            // Symja usa Integrate(expr, {var, lower, upper}) para integrales definidas
            String integrateCmd = String.format("Integrate(%s, {%s, %s, %s})",
                    expr.toString(), variable, lower, upper);
            IExpr integral = evaluator.eval(integrateCmd);
            log.debug("Definite integral of '{}' from {} to {}: {}", latex, lower, upper, integral);
            return integral.evalf();
        } catch (Exception e) {
            log.debug("Error computing definite integral: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Obtiene las construcciones no soportadas en una expresión LaTeX.
     * Symja soporta la mayoría de construcciones matemáticas estándar.
     */
    public List<String> getUnsupportedConstructions(String latex) {
        List<String> unsupported = new ArrayList<>();
        if (latex == null) {
            return unsupported;
        }

        // Symja tiene soporte amplio; solo reportar lo que falle al parsear
        IExpr expr = parseLatex(latex);
        if (expr == null) {
            unsupported.add("No se pudo parsear como LaTeX válido para Symja");
        }

        return unsupported;
    }
}