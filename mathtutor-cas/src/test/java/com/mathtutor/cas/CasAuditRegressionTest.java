package com.mathtutor.cas;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/** Regresiones de la auditoría EN-04: la evidencia numérica no prueba identidades. */
class CasAuditRegressionTest {

    private final LatexToSymjaConverter converter = new LatexToSymjaConverter();

    @Test
    void numericalSamplesCannotProveIdentity() {
        String polynomial = "(x+2)*(x+1)*(x+0.5)*x*(x-0.5)*(x-1)*(x-2)*(x-3)*(x-5)";
        assertThat(converter.areEquivalentThreeState(polynomial, "0"))
                .isNotEqualTo(EquivalenceResult.EQUIVALENTE);
        assertThat(new SymjaCasEngine(converter).verify(polynomial, "0").hint())
                .contains("No se pudo determinar");
    }

    @Test
    void tinyConstantDifferenceIsNotEquivalent() {
        assertThat(converter.areEquivalentThreeState("x", "x+0.0000000000001"))
                .isEqualTo(EquivalenceResult.NO_EQUIVALENTE);
    }

    @Test
    void numericComparisonDoesNotAssignTheSymbol() {
        converter.areEquivalentThreeState("x^2", "x^3");
        assertThat(converter.derive("x^2", "x")).isEqualTo("2*x");
        assertThat(converter.integrateDefinite("x", "x", 0, 1)).isEqualTo(0.5);
    }

    @Test
    void numericalHintUsesActualValues() {
        var engine = new SymjaCasEngine(converter);
        var verification = engine.verify("x^2", "x^3");
        assertThat(verification.correct()).isFalse();
        assertThat(verification.hint()).contains("4.0000", "8.0000")
                .doesNotContain("numéricamente equivalentes");
    }

    @Test
    void corpusIsStrictJsonWithTenCategories() throws Exception {
        var mapper = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        try (var stream = getClass().getResourceAsStream("/latex-symja-corpus.json")) {
            var corpus = mapper.readTree(stream);
            assertThat(corpus.path("categories").size()).isEqualTo(10);
        }
    }

    @Test
    void environmentFormulasIgnoreEscapedDollars() {
        var extractor = new LatexContentExtractor();
        assertThat(extractor.extractFormulasFromEnvironment("Precio \\$5 y fórmula $x^2$"))
                .containsExactly("x^2");
    }

    @Test
    void extractionIncludesNestedTemplateEnvironments() {
        var extractor = new LatexContentExtractor();
        String content = "\\bloque{derivadas}\\tema{polinomios}\\dificultad{1}"
                + "\\begin{ejercicio}\\begin{enunciado}$x^2$\\end{enunciado}"
                + "\\begin{solucion}\\begin{paso}$2x$\\end{paso}\\end{solucion}\\end{ejercicio}";
        assertThat(extractor.extract(content).getEnvironments())
                .containsKeys("ejercicio", "enunciado", "solucion", "paso");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "x+1", "x);Print(1)"})
    void calculusRejectsAnInvalidVariable(String variable) {
        assertThat(converter.derive("x", variable)).isNull();
        assertThat(converter.integrate("x", variable)).isNull();
        assertThat(converter.integrateDefinite("x", variable, 0, 1)).isNull();
    }
}
