package com.mathtutor.cas;

import org.matheclipse.core.eval.ExprEvaluator;
import org.matheclipse.core.form.tex.TeXParser;
import org.matheclipse.core.interfaces.IExpr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

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
 *    - Mitigación: FullSimplify; el muestreo numérico solo descarta equivalencias
 * 2. Integrales definidas con límites complejos pueden fallar
 * 3. Algunas funciones especiales (Bessel, Gamma, etc.) requieren Hipparchus
 * 4. Parseo LaTeX estricto: requiere sintaxis LaTeX correcta y balanceada
 */
@Component
public class LatexToSymjaConverter {

    private static final Logger log = LoggerFactory.getLogger(LatexToSymjaConverter.class);
    private static final Pattern POWERED_FUNCTION = Pattern.compile(
            "\\\\(?:sin|cos|tan|cot|sec|csc|ln|log|exp)\\(([^()]*)\\)(?=\\s*\\^)");


    /**
     * Convierte una expresión LaTeX a formato Symja (AST evaluable).
     *
     * @param latex Expresión en formato LaTeX
     * @return Expresión Symja parseada, o null si falla el parseo
     */
    public IExpr parseLatex(String latex) {
        return parseLatex(latex, new ExprEvaluator());
    }

    private IExpr parseLatex(String latex, ExprEvaluator evaluator) {
        if (latex == null || latex.trim().isEmpty()) {
            return null;
        }

        // Symja necesita agrupar la función para no convertir sin(x)^2 en sin(x^2).
        // La salida ASCII del CAS escribe x^12. TeX sin llaves consumiría solo el primer dígito.
        String groupedExponents = latex.trim().replaceAll("\\^([+-]?\\d{2,})", "^{$1}");
        String cleanLatex = POWERED_FUNCTION.matcher(groupedExponents).replaceAll("($0)");

        try {
            // Symja puede parsear LaTeX directamente via TeXParser
            evaluator.parse("0"); // Activa el motor de esta operación en el hilo actual.
            IExpr expr = new TeXParser().parse(cleanLatex);
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
     * @deprecated Use {@link #areEquivalentThreeState(String, String)} for three-state result
     */
    @Deprecated
    public boolean areEquivalent(String latex1, String latex2) {
        return areEquivalentThreeState(latex1, latex2).toBoolean();
    }

    /**
     * Verifica si dos expresiones LaTeX son matemáticamente equivalentes.
     * Retorna resultado de tres estados para evitar falsos positivos.
     *
     * Estados:
     * - EQUIVALENTE: Simbólicamente idénticas, diferencia simplifica a 0, verificadas por simplificación completa
     * - NO_EQUIVALENTE: Diferencia numérica mayor que la tolerancia en algún punto
     * - INDETERMINADO: Solo evidencia numérica, dominios disjuntos, error de parseo/evaluación
     *
     * @param latex1 Primera expresión LaTeX
     * @param latex2 Segunda expresión LaTeX
     * @return Resultado de tres estados
     */
    public EquivalenceResult areEquivalentThreeState(String latex1, String latex2) {
        ExprEvaluator evaluator = new ExprEvaluator();
        IExpr expr1 = parseLatex(latex1, evaluator);
        IExpr expr2 = parseLatex(latex2, evaluator);

        if (expr1 == null || expr2 == null) {
            // No se puede parsear: indeterminado
            log.debug("Cannot parse one or both expressions: '{}' vs '{}'", latex1, latex2);
            return EquivalenceResult.INDETERMINADO;
        }

        try {
            // Optimización: si los ASTs son idénticos
            if (expr1.equals(expr2)) {
                return EquivalenceResult.EQUIVALENTE;
            }

            // Simbólicamente: simplificar (expr1 - expr2) y verificar si es 0
            IExpr diff = expr1.subtract(expr2);
            IExpr simplified = diff.normal(true);

            // Verificar si la simplificación es exactamente 0
            if (simplified.isZero()) {
                return EquivalenceResult.EQUIVALENTE;
            }

            // También intentar together() para combinar fracciones
            IExpr together = diff.together();
            if (together.isZero()) {
                return EquivalenceResult.EQUIVALENTE;
            }

            IExpr fullSimplified = evaluator.eval("FullSimplify(" + diff + ")");
            if (fullSimplified.isZero()) {
                return EquivalenceResult.EQUIVALENTE;
            }
            if (fullSimplified.isNumber()) {
                return EquivalenceResult.NO_EQUIVALENTE;
            }

            // Si no es exactamente 0, intentar evaluación numérica en varios puntos
            // para descartar diferencias cuando no hay prueba simbólica
            return evaluateNumericallyThreeState(expr1, expr2);

        } catch (Exception e) {
            log.debug("Error checking equivalence symbolically: {}", e.getMessage());
            // Fallback a evaluación numérica de tres estados
            return evaluateNumericallyThreeState(expr1, expr2);
        }
    }

    /**
     * Evalúa dos expresiones numéricamente en varios puntos con resultado de tres estados.
     * Solo permite refutar equivalencias; coincidir en los puntos no es una prueba.
     */
    private EquivalenceResult evaluateNumericallyThreeState(IExpr expr1, IExpr expr2) {
        double[] testPoints = {-2.0, -1.0, -0.5, 0.0, 0.5, 1.0, 2.0, 3.0, 5.0};
        for (double point : testPoints) {
            try {
                double first = evaluateAt(expr1, "x", point);
                double second = evaluateAt(expr2, "x", point);
                if (Math.abs(first - second) > 1e-9) {
                    return EquivalenceResult.NO_EQUIVALENTE;
                }
            } catch (RuntimeException ignored) {
                // Un punto fuera del dominio no aporta evidencia de equivalencia.
            }
        }
        // Coincidir en una muestra finita nunca demuestra una identidad simbólica.
        return EquivalenceResult.INDETERMINADO;
    }

    /** Evalúa sin asignar símbolos ni conservar estado entre peticiones. */
    double evaluateAt(IExpr expression, String variable, double value) {
        ExprEvaluator evaluator = new ExprEvaluator();
        IExpr localExpression = evaluator.parse(expression.toString());
        IExpr substituted = localExpression.subs(evaluator.parse(variable), evaluator.parse(Double.toString(value)));
        double result = substituted.evalf();
        if (!Double.isFinite(result)) {
            throw new IllegalArgumentException("La expresión no tiene un valor real finito en el punto");
        }
        return result;
    }
    /**
     * Calcula la derivada simbólica de una expresión LaTeX respecto a una variable.
     *
     * @param latex Expresión LaTeX
     * @param variable Variable respecto a la cual derivar (ej. "x")
     * @return Derivada en formato Symja string, o null si falla
     */
    public String derive(String latex, String variable) {
        if (!isValidVariable(variable)) {
            return null;
        }
        ExprEvaluator evaluator = new ExprEvaluator();
        IExpr expr = parseLatex(latex, evaluator);
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
        if (!isValidVariable(variable)) {
            return null;
        }
        ExprEvaluator evaluator = new ExprEvaluator();
        IExpr expr = parseLatex(latex, evaluator);
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
        if (!isValidVariable(variable)) {
            return null;
        }
        ExprEvaluator evaluator = new ExprEvaluator();
        IExpr expr = parseLatex(latex, evaluator);
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
        ExprEvaluator evaluator = new ExprEvaluator();
        IExpr expr = parseLatex(latex, evaluator);
        if (expr == null) {
            unsupported.add("No se pudo parsear como LaTeX válido para Symja");
        }

        return unsupported;
    }

    private boolean isValidVariable(String variable) {
        return variable != null && variable.matches("[A-Za-z][A-Za-z0-9]*");
    }
}
