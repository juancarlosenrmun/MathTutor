package com.mathtutor.cas;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Pruebas de caja negra para LatexToSymjaConverter.
 * Entrada: LaTeX → Salida esperada (comportamiento observable).
 * No testea implementación interna, solo contrato de entrada/salida.
 */
class LatexToSymjaConverterBlackBoxTest {

    private final LatexToSymjaConverter converter = new LatexToSymjaConverter();

    @ParameterizedTest(name = "{index}: {0} → contiene {1}")
    @CsvSource({
        "x^2, x",
        "\\sin(x), sin",
        "\\cos(x), cos",
        "\\ln(x), log",
        "\\sqrt{x}, sqrt",
        "a \\cdot b, a",
        "\\frac{1}{2}, 1",
        "\\frac{a}{b}, a",
        "x^{2}, x",
        "x^{n}, x",
        "e^{x}, e"
    })
    @DisplayName("Convierte funciones y operadores básicos")
    void convertsBasicFunctionsAndOperators(String latex, String expectedContains) {
        String result = converter.convert(latex);
        assertThat(result).as("LaTeX: %s", latex).isNotNull();
        assertThat(result.toLowerCase()).as("LaTeX: %s", latex).contains(expectedContains.toLowerCase());
    }

    @ParameterizedTest(name = "{index}: {0} ≡ {1}")
    @CsvSource({
        "x^2, x^2",
        "\\sin(x), \\sin(x)",
        "\\frac{1}{2}, 0.5",
        "\\sqrt{4}, 2",
        "(x+1)^2, x^2+2*x+1",
        "2*x+3*x, 5*x",
        "x^2+x^2, 2*x^2"
    })
    @DisplayName("Expresiones equivalentes son detectadas como equivalentes")
    void equivalentExpressionsAreDetected(String expr1, String expr2) {
        boolean result = converter.areEquivalent(expr1, expr2);
        assertThat(result).as("%s ≡ %s", expr1, expr2).isTrue();
    }

    @ParameterizedTest(name = "{index}: {0} ≢ {1}")
    @CsvSource({
        "x^2, x^3",
        "\\sin(x), \\cos(x)",
        "\\frac{1}{2}, \\frac{1}{3}",
        "x+1, x+2",
        "2*x, 3*x"
    })
    @DisplayName("Expresiones no equivalentes son detectadas como no equivalentes")
    void nonEquivalentExpressionsAreDetected(String expr1, String expr2) {
        boolean result = converter.areEquivalent(expr1, expr2);
        assertThat(result).as("%s ≢ %s", expr1, expr2).isFalse();
    }

    @Test
    @DisplayName("Expresión compleja: regla de la cadena")
    void complexExpressionChainRule() {
        String latex = "\\frac{\\mathrm{d}}{\\mathrm{d}x} \\sin(x^2)";
        String result = converter.convert(latex);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Expresión compleja: integral definida")
    void complexExpressionDefiniteIntegral() {
        String latex = "\\int_{0}^{1} x^2 \\mathrm{d}x";
        String result = converter.convert(latex);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Expresión compleja: fracción anidada")
    void complexExpressionNestedFraction() {
        String latex = "\\frac{\\frac{a}{b}}{c}";
        String result = converter.convert(latex);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Entrada inválida no lanza excepción")
    void invalidInputDoesNotThrow() {
        // Todas estas no deben lanzar excepción
        assertThatCode(() -> converter.convert(null)).doesNotThrowAnyException();
        assertThatCode(() -> converter.convert("")).doesNotThrowAnyException();
        assertThatCode(() -> converter.convert("\\frac{1}{")).doesNotThrowAnyException();
        assertThatCode(() -> converter.convert("\\unknown{command}")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("areEquivalent con entrada inválida retorna false")
    void areEquivalentWithInvalidInputReturnsFalse() {
        assertThat(converter.areEquivalent(null, "x")).isFalse();
        assertThat(converter.areEquivalent("x", null)).isFalse();
        assertThat(converter.areEquivalent("", "x")).isFalse();
        assertThat(converter.areEquivalent("\\invalid", "x")).isFalse();
    }
}