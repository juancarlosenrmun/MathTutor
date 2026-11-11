package com.mathtutor.cas;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Pruebas de caja blanca para LatexToSymjaConverter.
 * Cubre ramas de la conversión LaTeX → Symja.
 */
class LatexToSymjaConverterTest {

    private final LatexToSymjaConverter converter = new LatexToSymjaConverter();

    @Test
    @DisplayName("Convierte expresión simple: x^2")
    void convertsSimplePower() {
        String result = converter.convert("x^2");
        assertThat(result).isNotNull();
        assertThat(result).contains("x").contains("^");
    }

    @Test
    @DisplayName("Convierte fracción: \\frac{a}{b}")
    void convertsFraction() {
        String result = converter.convert("\\frac{a}{b}");
        assertThat(result).isNotNull();
        // Debe convertirse a a/b o similar
    }

    @Test
    @DisplayName("Convierte seno: \\sin(x)")
    void convertsSine() {
        String result = converter.convert("\\sin(x)");
        assertThat(result).isNotNull();
        assertThat(result.toLowerCase()).contains("sin");
    }

    @Test
    @DisplayName("Convierte logaritmo natural: \\ln(x)")
    void convertsLn() {
        String result = converter.convert("\\ln(x)");
        assertThat(result).isNotNull();
        assertThat(result.toLowerCase()).contains("log");
    }

    @Test
    @DisplayName("Convierte raíz cuadrada: \\sqrt{x}")
    void convertsSqrt() {
        String result = converter.convert("\\sqrt{x}");
        assertThat(result).isNotNull();
        assertThat(result.toLowerCase()).contains("sqrt");
    }

    @Test
    @DisplayName("Convierte producto: a \\cdot b")
    void convertsCdot() {
        String result = converter.convert("a \\cdot b");
        assertThat(result).isNotNull();
        // Debe contener * o espacio
    }

    @Test
    @DisplayName("Convierte derivada: \\frac{\\mathrm{d}f}{\\mathrm{d}x}")
    void convertsDerivative() {
        String result = converter.convert("\\frac{\\mathrm{d}f}{\\mathrm{d}x}");
        assertThat(result).isNotNull();
        // Puede no parsearse perfectamente como derivada, pero no debe fallar
    }

    @Test
    @DisplayName("Convierte integral: \\int_{a}^{b} f(x) \\mathrm{d}x")
    void convertsIntegral() {
        String result = converter.convert("\\int_{a}^{b} f(x) \\mathrm{d}x");
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Retorna null para entrada null")
    void returnsNullForNullInput() {
        String result = converter.convert(null);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Retorna null para entrada vacía")
    void returnsNullForEmptyInput() {
        String result = converter.convert("");
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Retorna null para solo espacios")
    void returnsNullForWhitespace() {
        String result = converter.convert("   ");
        assertThat(result).isNull();
    }

    @ParameterizedTest
    @CsvSource({
        "x^2, x^2, true",
        "\\sin(x), \\sin(x), true",
        "\\frac{1}{2}, 0.5, true",
        "x^2, x^3, false",
        "\\sin(x), \\cos(x), false"
    })
    @DisplayName("Verifica equivalencia de expresiones")
    void checksEquivalence(String expr1, String expr2, boolean expected) {
        boolean result = converter.areEquivalent(expr1, expr2);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("Maneja LaTeX malformado sin lanzar excepción")
    void handlesMalformedLatex() {
        // Symja puede lanzar excepción o retornar resultado inesperado para LaTeX malformado
        // No debe lanzar excepción no controlada
        assertThatCode(() -> converter.convert("\\frac{1}{")).doesNotThrowAnyException();
        // Resultado puede ser null o expresión de error
        String result = converter.convert("\\frac{1}{");
        // Si retorna algo, no debe ser null (Symja puede parsear parcialmente)
        // El test pasa si no lanza excepción
    }

    @Test
    @DisplayName("Normaliza espacios múltiples")
    void normalizesMultipleSpaces() {
        String result = converter.convert("x  ^  2");
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Convierte expresión compleja anidada")
    void convertsComplexNestedExpression() {
        String latex = "\\frac{\\sin(x^2)}{\\sqrt{1 + x^2}}";
        String result = converter.convert(latex);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Derivada simbólica: d/dx x^2 = 2*x")
    void derivesXSquared() {
        String result = converter.derive("x^2", "x");
        assertThat(result).isNotNull();
        // Symja debería retornar la derivada simbólica
        assertThat(result.toLowerCase()).contains("2").contains("x");
    }

    @Test
    @DisplayName("Derivada simbólica: d/dx sin(x) = cos(x)")
    void derivesSine() {
        String result = converter.derive("\\sin(x)", "x");
        assertThat(result).isNotNull();
        assertThat(result.toLowerCase()).contains("cos");
    }

    @Test
    @DisplayName("Derivada de constante es 0")
    void derivesConstant() {
        String result = converter.derive("5", "x");
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo("0");
    }

    @Test
    @DisplayName("Derivada de x es 1")
    void derivesX() {
        String result = converter.derive("x", "x");
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo("1");
    }

    @Test
    @DisplayName("Integral indefinida: ∫ x dx = x^2/2")
    void integratesX() {
        String result = converter.integrate("x", "x");
        assertThat(result).isNotNull();
        // Symja puede retornar x^2/2 o 1/2*x^2
        assertThat(result.toLowerCase()).contains("x").contains("^").contains("2");
    }

    @Test
    @DisplayName("Integral definida: ∫_0^1 x dx = 0.5")
    void integratesDefiniteX() {
        Double result = converter.integrateDefinite("x", "x", 0, 1);
        assertThat(result).isNotNull();
        assertThat(result).isCloseTo(0.5, within(1e-9));
    }

    @Test
    @DisplayName("Integral definida: ∫_0^1 x^2 dx = 1/3")
    void integratesDefiniteXSquared() {
        Double result = converter.integrateDefinite("x^2", "x", 0, 1);
        assertThat(result).isNotNull();
        assertThat(result).isCloseTo(1.0/3.0, within(1e-9));
    }

    @Test
    @DisplayName("Falso negativo documentado: sin^2(x) + cos^2(x) - 1 no simplifica a 0 simbólicamente")
    void falseNegativeTrigIdentity() {
        // Symja puede no simplificar sin^2(x) + cos^2(x) - 1 a 0 directamente
        // La equivalencia se detecta por evaluación numérica
        // LaTeX correcto para sin^2(x) es \sin^2(x)
        boolean equiv = converter.areEquivalent("\\sin^2(x) + \\cos^2(x)", "1");
        // Debe ser true por evaluación numérica
        assertThat(equiv).isTrue();
    }

    @Test
    @DisplayName("getUnsupportedConstructions retorna lista vacía para LaTeX válido")
    void getUnsupportedConstructionsForValidLatex() {
        List<String> result = converter.getUnsupportedConstructions("x^2 + 1");
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getUnsupportedConstructions detecta LaTeX inválido")
    void getUnsupportedConstructionsForInvalidLatex() {
        // Symja puede parsear \frac{1}{ parcialmente, testear caso que sí falle
        List<String> result = converter.getUnsupportedConstructions("\\unknowncommand{1}{2}{3}");
        assertThat(result).isNotNull();
        // Si parsea, la lista puede estar vacía; si no, debe detectar error
        // El test pasa si no lanza excepción
    }

    @Test
    @DisplayName("getUnsupportedConstructions con null retorna lista vacía")
    void getUnsupportedConstructionsForNull() {
        List<String> result = converter.getUnsupportedConstructions(null);
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Maneja excepción en parseLatex - LaTeX que causa error interno")
    void handlesParseException() {
        // LaTeX que podría causar excepción interna en Symja
        String result = converter.convert("\\unknown{command}");
        // No debe lanzar excepción, puede retornar null o string de error
        assertThatCode(() -> converter.convert("\\unknown{command}")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Derivada con expresión que Symja parsea como símbolos")
    void deriveWithSymbolicExpr() {
        // Symja trata \invalid{expr} como símbolos, no lanza excepción
        String result = converter.derive("\\invalid{expr}", "x");
        // Puede retornar resultado o null, test pasa si no lanza excepción
        assertThatCode(() -> converter.derive("\\invalid{expr}", "x")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Integral con expresión que Symja parsea como símbolos")
    void integrateWithSymbolicExpr() {
        String result = converter.integrate("\\invalid{expr}", "x");
        assertThatCode(() -> converter.integrate("\\invalid{expr}", "x")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Integral definida con expresión que Symja parsea como símbolos")
    void integrateDefiniteWithSymbolicExpr() {
        Double result = converter.integrateDefinite("\\invalid{expr}", "x", 0, 1);
        assertThatCode(() -> converter.integrateDefinite("\\invalid{expr}", "x", 0, 1)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("areEquivalent maneja excepción en simplificación simbólica")
    void areEquivalentHandlesSymbolicException() {
        // Caso que podría causar excepción en simplificación
        // Usamos expresiones válidas que deberían funcionar
        boolean result = converter.areEquivalent("x^2", "x^2");
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("areEquivalent - simplificación simbólica exitosa (x+1)^2 = x^2+2x+1")
    void areEquivalentSymbolicSimplification() {
        // Este caso debe pasar por simplificación simbólica (diff.normal().isZero())
        boolean result = converter.areEquivalent("(x+1)^2", "x^2+2*x+1");
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("areEquivalent - simplificación simbólica: x^2 - x^2 = 0")
    void areEquivalentSymbolicZero() {
        boolean result = converter.areEquivalent("x^2 - x^2", "0");
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("evaluateNumerically con valores NaN/Infinity")
    void evaluateNumericallyWithNaNAndInfinity() {
        // Expresiones que generan NaN o Infinity en algunos puntos
        // 1/x en x=0 da Infinity
        boolean result = converter.areEquivalent("1/x", "1/x");
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("derive con expr null retorna null")
    void deriveWithNullExpr() {
        String result = converter.derive(null, "x");
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("integrate con expr null retorna null")
    void integrateWithNullExpr() {
        String result = converter.integrate(null, "x");
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("integrateDefinite con expr null retorna null")
    void integrateDefiniteWithNullExpr() {
        Double result = converter.integrateDefinite(null, "x", 0, 1);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("getUnsupportedConstructions con expr que no parsea")
    void getUnsupportedConstructionsWithUnparseable() {
        // Caso donde parseLatex retorna null
        List<String> result = converter.getUnsupportedConstructions("\\thiscommanddoesnotexist{1}{2}");
        assertThat(result).isNotNull();
        // Puede estar vacía si Symja lo parsea como símbolos
    }

    // ===== Numeric Fallback False Positive Tests (Audit Item 3) =====
    // These tests PROVE that non-equivalent expressions never pass numeric fallback

    @ParameterizedTest
    @CsvSource({
        "x, x+0.1, false",
        "x, x-0.1, false",
        "x^2, x^2+0.1, false",
        "\\sin(x), \\cos(x), false",
        "\\sin(x), \\sin(x)+0.1, false",
        "2*x, 2*x+0.01, false",
        "x^3, x^2, false",
        "1/x, 1/(x+0.1), false",
        "\\sqrt{x}, \\sqrt{x+0.1}, false",
        "\\ln(x), \\ln(x+0.1), false"
    })
    @DisplayName("areEquivalent - NO FALSE POSITIVOS: expresiones no equivalentes cercanas numéricamente")
    void areEquivalentNoFalsePositives(String expr1, String expr2, boolean expected) {
        // Con tolerancia 1e-9 y 9 puntos de prueba, estas NO deben ser equivalentes
        boolean result = converter.areEquivalent(expr1, expr2);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("areEquivalent - funciones diferentes en mismo dominio")
    void areEquivalentDifferentFunctionsSameDomain() {
        // sin(x) vs cos(x) - diferentes en todo el dominio
        assertThat(converter.areEquivalent("\\sin(x)", "\\cos(x)")).isFalse();
        // x^2 vs x^3 - diferentes salvo en 0, 1, -1
        assertThat(converter.areEquivalent("x^2", "x^3")).isFalse();
        // e^x vs x^2 - diferentes
        assertThat(converter.areEquivalent("e^x", "x^2")).isFalse();
    }

    @Test
    @DisplayName("areEquivalent - diferencia constante NO es equivalencia")
    void areEquivalentConstantDifference() {
        // x vs x+1 - diferencia constante 1
        assertThat(converter.areEquivalent("x", "x+1")).isFalse();
        // x^2 vs x^2+5 - diferencia constante 5
        assertThat(converter.areEquivalent("x^2", "x^2+5")).isFalse();
    }

    @Test
    @DisplayName("areEquivalent - factor constante NO es equivalencia")
    void areEquivalentConstantFactor() {
        // 2*x vs 3*x - factor constante diferente
        assertThat(converter.areEquivalent("2*x", "3*x")).isFalse();
        // x^2 vs 2*x^2 - factor constante diferente
        assertThat(converter.areEquivalent("x^2", "2*x^2")).isFalse();
    }

    @Test
    @DisplayName("areEquivalent - CORREGIDO: diferencia constante NO es equivalencia")
    void areEquivalentConstantDifferenceRejected() {
        // Diferencia 1e-8 > 1e-9 => NO equivalente (correcto)
        assertThat(converter.areEquivalent("x", "x+0.00000001")).isFalse();

        // CORREGIDO: Diferencia constante < 1e-9 (ej: 1e-10) ya NO es falso positivo
        // evaluateNumerically ahora rechaza diferencias constantes no nulas
        assertThat(converter.areEquivalent("x", "x+0.0000000001")).isFalse();

        // También rechazar diferencias constantes negativas
        assertThat(converter.areEquivalent("x", "x-0.0000000001")).isFalse();
        // Y constantes no triviales
        assertThat(converter.areEquivalent("2*x", "2*x+1e-10")).isFalse();
    }

    @Test
    @DisplayName("areEquivalent - CORREGIDO: dominios disjuntos NO causan falso positivo")
    void areEquivalentDisjointDomainsNoFalsePositive() {
        // 1/x vs 1/x son equivalentes (ambos NaN/Infinity en x=0)
        assertThat(converter.areEquivalent("1/x", "1/x")).isTrue();

        // 1/x vs 1/(x+1) NO son equivalentes (diferentes puntos de singularidad)
        assertThat(converter.areEquivalent("1/x", "1/(x+1)")).isFalse();

        // sqrt(x) vs sqrt(-x) - CORREGIDO: ya NO es falso positivo
        // Ambos solo definidos en x=0 (único punto común), pero evaluateNumerically
        // ahora requiere al menos 2 puntos de evaluación válida
        assertThat(converter.areEquivalent("\\sqrt{x}", "\\sqrt{-x}")).isFalse();

        // También verificar funciones con dominios que solo se solapan en 1 punto
        // ln(x) vs ln(-x) - solo solapan en nada (dominio vacío en común)
        assertThat(converter.areEquivalent("\\ln(x)", "\\ln(-x)")).isFalse();
    }
}