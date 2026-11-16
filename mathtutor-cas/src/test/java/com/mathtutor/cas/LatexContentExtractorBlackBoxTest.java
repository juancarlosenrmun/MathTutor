package com.mathtutor.cas;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Pruebas de caja negra para LatexContentExtractor.
 * Entrada: contenido .tex → Salida: ExtractionResult con macros, entornos, fórmulas.
 * Verifica comportamiento observable sin conocer implementación interna.
 */
class LatexContentExtractorBlackBoxTest {

    private final LatexContentExtractor extractor = new LatexContentExtractor();

    @Test
    @DisplayName("Extrae macros básicas: bloque, tema, dificultad")
    void extractsBasicMacros() {
        String tex = """
                \\bloque{Derivadas}
                \\tema{Regla de la cadena}
                \\dificultad{2}

                \\begin{ejercicio}
                Derivar f(x) = sin(x^2)
                \\end{ejercicio}
                """;

        LatexContentExtractor.ExtractionResult result = extractor.extract(tex);

        assertThat(result.getBloque()).isEqualTo("Derivadas");
        assertThat(result.getTema()).isEqualTo("Regla de la cadena");
        assertThat(result.getDificultad()).isEqualTo(2);
        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    @DisplayName("Extrae entorno ejercicio")
    void extractsEjercicioEnvironment() {
        String tex = """
                \\bloque{Derivadas}
                \\tema{Regla de la cadena}
                \\dificultad{2}

                \\begin{ejercicio}
                Derive la función f(x) = sin(x^2)
                \\end{ejercicio}
                """;

        LatexContentExtractor.ExtractionResult result = extractor.extract(tex);

        assertThat(result.getEnvironments()).containsKey("ejercicio");
        assertThat(result.getEnvironments().get("ejercicio")).hasSize(1);
        assertThat(result.getEnvironments().get("ejercicio").get(0))
                .contains("Derive la función");
    }

    @Test
    @DisplayName("Extrae entorno enunciado")
    void extractsEnunciadoEnvironment() {
        String tex = """
                \\bloque{Integrales}
                \\tema{Integral definida}
                \\dificultad{3}

                \\begin{enunciado}
                Calcule la integral definida de x^2 desde 0 hasta 1
                \\end{enunciado}
                """;

        LatexContentExtractor.ExtractionResult result = extractor.extract(tex);

        assertThat(result.getEnvironments()).containsKey("enunciado");
        assertThat(result.getEnvironments().get("enunciado")).hasSize(1);
        assertThat(result.getEnvironments().get("enunciado").get(0))
                .contains("integral definida");
    }

    @Test
    @DisplayName("Extrae entorno solucion (con y sin acento)")
    void extractsSolucionEnvironment() {
        String tex1 = """
                \\bloque{Test}
                \\tema{Test}
                \\dificultad{1}
                \\begin{solucion}
                La respuesta es 1/3
                \\end{solucion}
                """;

        String tex2 = """
                \\bloque{Test}
                \\tema{Test}
                \\dificultad{1}
                \\begin{solución}
                La respuesta es 1/3
                \\end{solución}
                """;

        LatexContentExtractor.ExtractionResult result1 = extractor.extract(tex1);
        LatexContentExtractor.ExtractionResult result2 = extractor.extract(tex2);

        assertThat(result1.getEnvironments()).containsKey("solucion");
        assertThat(result2.getEnvironments()).containsKey("solucion");
    }

    @Test
    @DisplayName("Extrae entorno paso")
    void extractsPasoEnvironment() {
        String tex = """
                \\bloque{Derivadas}
                \\tema{Regla de la cadena}
                \\dificultad{2}

                \\begin{paso}
                Identificar función externa e interna: f(u) = sin(u), u = x^2
                \\end{paso}
                \\begin{paso}
                Derivar externa: f'(u) = cos(u)
                \\end{paso}
                \\begin{paso}
                Derivar interna: u' = 2x
                \\end{paso}
                """;

        LatexContentExtractor.ExtractionResult result = extractor.extract(tex);

        assertThat(result.getEnvironments()).containsKey("paso");
        assertThat(result.getEnvironments().get("paso")).hasSize(3);
        assertThat(result.getEnvironments().get("paso").get(0)).contains("externa e interna");
        assertThat(result.getEnvironments().get("paso").get(1)).contains("cos(u)");
        assertThat(result.getEnvironments().get("paso").get(2)).contains("2x");
    }

    @Test
    @DisplayName("Extrae entorno errorTipico (con y sin acento)")
    void extractsErrorTipicoEnvironment() {
        String tex1 = """
                \\bloque{Test}
                \\tema{Test}
                \\dificultad{1}
                \\begin{errorTipico}
                Olvidar multiplicar por la derivada de la función interna
                \\end{errorTipico}
                """;

        String tex2 = """
                \\bloque{Test}
                \\tema{Test}
                \\dificultad{1}
                \\begin{errorTípico}
                Confundir regla de la cadena con regla del producto
                \\end{errorTípico}
                """;

        LatexContentExtractor.ExtractionResult result1 = extractor.extract(tex1);
        LatexContentExtractor.ExtractionResult result2 = extractor.extract(tex2);

        assertThat(result1.getEnvironments()).containsKey("errorTipico");
        assertThat(result2.getEnvironments()).containsKey("errorTipico");
    }

    @Test
    @DisplayName("Extrae entorno conceptoClave")
    void extractsConceptoClaveEnvironment() {
        String tex = """
                \\bloque{Derivadas}
                \\tema{Regla de la cadena}
                \\dificultad{2}

                \\begin{conceptoClave}
                La regla de la cadena: (f∘g)'(x) = f'(g(x))·g'(x)
                \\end{conceptoClave}
                """;

        LatexContentExtractor.ExtractionResult result = extractor.extract(tex);

        assertThat(result.getEnvironments()).containsKey("conceptoClave");
        assertThat(result.getEnvironments().get("conceptoClave")).hasSize(1);
        assertThat(result.getEnvironments().get("conceptoClave").get(0))
                .contains("regla de la cadena");
    }

    @Test
    @DisplayName("Extrae fórmulas inline $...$")
    void extractsInlineFormulas() {
        String tex = """
                \\bloque{Test}
                \\tema{Test}
                \\dificultad{1}
                \\begin{ejercicio}
                Derivar $f(x) = x^2$ y $g(x) = \\sin(x)$
                \\end{ejercicio}
                """;

        LatexContentExtractor.ExtractionResult result = extractor.extract(tex);

        assertThat(result.getFormulas()).containsExactlyInAnyOrder(
                "f(x) = x^2",
                "g(x) = \\sin(x)"
        );
    }

    @Test
    @DisplayName("Extrae fórmulas display $$...$$")
    void extractsDisplayFormulas() {
        String tex = """
                \\bloque{Test}
                \\tema{Test}
                \\dificultad{1}
                \\begin{solucion}
                $$\\int_0^1 x^2 dx = \\frac{1}{3}$$
                $$\\frac{d}{dx} \\sin(x) = \\cos(x)$$
                \\end{solucion}
                """;

        LatexContentExtractor.ExtractionResult result = extractor.extract(tex);

        assertThat(result.getFormulas()).containsExactlyInAnyOrder(
                "\\int_0^1 x^2 dx = \\frac{1}{3}",
                "\\frac{d}{dx} \\sin(x) = \\cos(x)"
        );
    }

    @Test
    @DisplayName("Extrae fórmulas mixtas inline y display")
    void extractsMixedFormulas() {
        String tex = """
                \\bloque{Test}
                \\tema{Test}
                \\dificultad{1}
                \\begin{enunciado}
                Dada $f(x) = x^2$, calcule $$\\int_0^1 f(x) dx$$
                \\end{enunciado}
                """;

        LatexContentExtractor.ExtractionResult result = extractor.extract(tex);

        assertThat(result.getFormulas()).hasSize(2);
        assertThat(result.getFormulas()).contains("f(x) = x^2");
        assertThat(result.getFormulas()).contains("\\int_0^1 f(x) dx");
    }

    @Test
    @DisplayName("Retorna error si falta macro bloque")
    void errorsWhenBloqueMissing() {
        String tex = """
                \\tema{Test}
                \\dificultad{1}
                \\begin{ejercicio}Test\\end{ejercicio}
                """;

        LatexContentExtractor.ExtractionResult result = extractor.extract(tex);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("bloque"));
    }

    @Test
    @DisplayName("Retorna error si falta macro tema")
    void errorsWhenTemaMissing() {
        String tex = """
                \\bloque{Test}
                \\dificultad{1}
                \\begin{ejercicio}Test\\end{ejercicio}
                """;

        LatexContentExtractor.ExtractionResult result = extractor.extract(tex);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("tema"));
    }

    @Test
    @DisplayName("Retorna error si falta macro dificultad")
    void errorsWhenDificultadMissing() {
        String tex = """
                \\bloque{Test}
                \\tema{Test}
                \\begin{ejercicio}Test\\end{ejercicio}
                """;

        LatexContentExtractor.ExtractionResult result = extractor.extract(tex);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("dificultad"));
    }

    @Test
    @DisplayName("Retorna error si dificultad no es número")
    void errorsWhenDificultadNotNumber() {
        String tex = """
                \\bloque{Test}
                \\tema{Test}
                \\dificultad{dos}
                \\begin{ejercicio}Test\\end{ejercicio}
                """;

        LatexContentExtractor.ExtractionResult result = extractor.extract(tex);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("dificultad") && e.contains("número"));
    }

    @Test
    @DisplayName("Retorna error si no hay entornos válidos")
    void errorsWhenNoValidEnvironments() {
        String tex = """
                \\bloque{Test}
                \\tema{Test}
                \\dificultad{1}
                \\begin{entornoInvalido}Test\\end{entornoInvalido}
                """;

        LatexContentExtractor.ExtractionResult result = extractor.extract(tex);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("entornos válidos"));
    }

    @Test
    @DisplayName("Maneja contenido nulo o vacío")
    void handlesNullOrEmptyContent() {
        LatexContentExtractor.ExtractionResult result1 = extractor.extract(null);
        LatexContentExtractor.ExtractionResult result2 = extractor.extract("");
        LatexContentExtractor.ExtractionResult result3 = extractor.extract("   ");

        assertThat(result1.hasErrors()).isTrue();
        assertThat(result2.hasErrors()).isTrue();
        assertThat(result3.hasErrors()).isTrue();
    }

    @Test
    @DisplayName("Ignora entornos no reconocidos")
    void ignoresUnrecognizedEnvironments() {
        String tex = """
                \\bloque{Test}
                \\tema{Test}
                \\dificultad{1}
                \\begin{ejercicio}Valid\\end{ejercicio}
                \\begin{otroEntorno}Ignored\\end{otroEntorno}
                \\begin{solucion}Valid\\end{solucion}
                """;

        LatexContentExtractor.ExtractionResult result = extractor.extract(tex);

        assertThat(result.getEnvironments()).containsKeys("ejercicio", "solucion");
        assertThat(result.getEnvironments()).doesNotContainKey("otroEntorno");
        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    @DisplayName("Extrae múltiples entornos del mismo tipo")
    void extractsMultipleSameTypeEnvironments() {
        String tex = """
                \\bloque{Test}
                \\tema{Test}
                \\dificultad{1}
                \\begin{paso}Paso 1\\end{paso}
                \\begin{paso}Paso 2\\end{paso}
                \\begin{paso}Paso 3\\end{paso}
                """;

        LatexContentExtractor.ExtractionResult result = extractor.extract(tex);

        assertThat(result.getEnvironments().get("paso")).hasSize(3);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "2", "3"})
    @DisplayName("Acepta valores de dificultad válidos 1, 2, 3")
    void acceptsValidDificultadValues(String diff) {
        String tex = String.format("""
                \\bloque{Test}
                \\tema{Test}
                \\dificultad{%s}
                \\begin{ejercicio}Test\\end{ejercicio}
                """, diff);

        LatexContentExtractor.ExtractionResult result = extractor.extract(tex);

        assertThat(result.getDificultad()).isEqualTo(Integer.parseInt(diff));
        assertThat(result.hasErrors()).isFalse();
    }
}