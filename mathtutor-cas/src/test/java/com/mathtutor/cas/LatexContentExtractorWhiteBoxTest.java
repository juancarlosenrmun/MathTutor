package com.mathtutor.cas;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Pruebas de caja blanca para LatexContentExtractor.
 * Verifica lógica interna, casos frontera y manejo de errores.
 */
class LatexContentExtractorWhiteBoxTest {

    private LatexContentExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new LatexContentExtractor();
    }

    @Test
    @DisplayName("normalizeEnvironmentName: quita acentos correctamente")
    void normalizeEnvironmentNameRemovesAccents() {
        // Usar reflexión para probar método private
        assertThat(invokeNormalize("solución")).isEqualTo("solucion");
        assertThat(invokeNormalize("errorTípico")).isEqualTo("errorTipico");
        assertThat(invokeNormalize("conceptoClave")).isEqualTo("conceptoClave");
        assertThat(invokeNormalize("enunciado")).isEqualTo("enunciado");
    }

    @Test
    @DisplayName("extractFormulasFromEnvironment: extrae fórmulas de contenido de entorno")
    void extractFormulasFromEnvironmentExtractsFormulas() {
        String content = "Paso 1: $f(x) = x^2$. Paso 2: $$\\frac{d}{dx}x^2 = 2x$$";

        List<String> formulas = extractor.extractFormulasFromEnvironment(content);

        assertThat(formulas).hasSize(2);
        assertThat(formulas).containsExactlyInAnyOrder(
                "f(x) = x^2",
                "\\frac{d}{dx}x^2 = 2x"
        );
    }

    @Test
    @DisplayName("extractFormulasFromEnvironment: retorna lista vacía para null")
    void extractFormulasFromEnvironmentReturnsEmptyForNull() {
        List<String> formulas = extractor.extractFormulasFromEnvironment(null);

        assertThat(formulas).isEmpty();
    }

    @Test
    @DisplayName("extractFormulasFromEnvironment: retorna lista vacía para contenido sin fórmulas")
    void extractFormulasFromEnvironmentReturnsEmptyForNoFormulas() {
        List<String> formulas = extractor.extractFormulasFromEnvironment("Texto sin fórmulas");

        assertThat(formulas).isEmpty();
    }

    @Test
    @DisplayName("extract: maneja macros con espacios extra")
    void extractHandlesMacrosWithExtraSpaces() {
        String tex = """
                \\bloque{ Derivadas }
                \\tema{ Regla de la cadena }
                \\dificultad{ 2 }
                \\begin{ejercicio}Test\\end{ejercicio}
                """;

        LatexContentExtractor.ExtractionResult result = extractor.extract(tex);

        assertThat(result.getBloque()).isEqualTo("Derivadas");
        assertThat(result.getTema()).isEqualTo("Regla de la cadena");
        assertThat(result.getDificultad()).isEqualTo(2);
    }

    @Test
    @DisplayName("extract: maneja entornos con contenido multilínea")
    void extractHandlesMultilineEnvironmentContent() {
        String tex = """
                \\bloque{Test}
                \\tema{Test}
                \\dificultad{1}
                \\begin{solucion}
                Línea 1
                Línea 2
                Línea 3
                \\end{solucion}
                """;

        LatexContentExtractor.ExtractionResult result = extractor.extract(tex);

        assertThat(result.getEnvironments().get("solucion")).hasSize(1);
        assertThat(result.getEnvironments().get("solucion").get(0))
                .contains("Línea 1")
                .contains("Línea 2")
                .contains("Línea 3");
    }

    @Test
    @DisplayName("extract: maneja entornos anidados (toma el más externo)")
    void extractHandlesNestedEnvironments() {
        String tex = """
                \\bloque{Test}
                \\tema{Test}
                \\dificultad{1}
                \\begin{ejercicio}
                Enunciado \\begin{paso}paso interno\\end{paso} final
                \\end{ejercicio}
                """;

        LatexContentExtractor.ExtractionResult result = extractor.extract(tex);

        // El regex con DOTALL captura hasta el primer \\end{ejercicio}
        assertThat(result.getEnvironments()).containsKey("ejercicio");
        // El paso interno NO se extrae como entorno separado porque está dentro del ejercicio
    }

    @Test
    @DisplayName("extract: maneja múltiples archivos .tex concatenados (solo primera macro)")
    void extractTakesFirstMacroOccurrence() {
        String tex = """
                \\bloque{Primero}
                \\bloque{Segundo}
                \\tema{Tema}
                \\dificultad{1}
                \\begin{ejercicio}Test\\end{ejercicio}
                """;

        LatexContentExtractor.ExtractionResult result = extractor.extract(tex);

        // find() encuentra la primera ocurrencia
        assertThat(result.getBloque()).isEqualTo("Primero");
    }

    @Test
    @DisplayName("extract: no captura $ escapados como fórmulas")
    void extractDoesNotCaptureEscapedDollars() {
        String tex = """
                \\bloque{Test}
                \\tema{Test}
                \\dificultad{1}
                \\begin{ejercicio}
                Precio: \\$100 y \\$200. Fórmula: $x^2$
                \\end{ejercicio}
                """;

        LatexContentExtractor.ExtractionResult result = extractor.extract(tex);

        // Solo debe capturar x^2, no los precios con $ escapados
        assertThat(result.getFormulas()).containsExactly("x^2");
    }

    @Test
    @DisplayName("MUTATION TEST: regex sin lookbehind captura $ escapados como fórmulas (debe fallar si regex rota)")
    void mutationTestEscapedDollarsCaught() {
        // Este test está diseñado para FALLAR si el regex inline se rompe a \\$(.*?)\\$
        // (sin negative lookbehind). Verifica que el test SÍ detecta la mutación.
        String tex = """
                \\bloque{Test}
                \\tema{Test}
                \\dificultad{1}
                \\begin{ejercicio}
                Precio: \\$100 y \\$200. Fórmula: $x^2$
                \\end{ejercicio}
                """;

        LatexContentExtractor.ExtractionResult result = extractor.extract(tex);

        // Con regex CORRECTO: solo ["x^2"]
        // Con regex ROTO (sin lookbehind): captura ["100 y ", "x^2"] o similar
        // Este test DEBE pasar con implementación correcta
        assertThat(result.getFormulas()).containsExactly("x^2");
        // Verificación adicional: NO debe contener fragmentos de precios
        assertThat(result.getFormulas()).noneMatch(f -> f.contains("100") || f.contains("200") || f.contains("Precio"));
    }

    @Test
    @DisplayName("MUTATION TEST: regex sin lookbehind falla con múltiples $ escapados")
    void mutationTestMultipleEscapedDollars() {
        String tex = """
                \\bloque{Test}
                \\tema{Test}
                \\dificultad{1}
                \\begin{ejercicio}
                Costo: \\$50, impuesto: \\$5, total: \\$55. Fórmula: $y = x + 1$
                \\end{ejercicio}
                """;

        LatexContentExtractor.ExtractionResult result = extractor.extract(tex);

        // Con regex CORRECTO: solo ["y = x + 1"]
        // Con regex ROTO: capturaría fragmentos entre $ escapados
        assertThat(result.getFormulas()).containsExactly("y = x + 1");
        assertThat(result.getFormulas()).noneMatch(f -> f.contains("50") || f.contains("5") || f.contains("55"));
    }

    @Test
    @DisplayName("MUTATION TEST: regex sin lookbehind falla con $ escapado al final")
    void mutationTestEscapedDollarAtEnd() {
        String tex = """
                \\bloque{Test}
                \\tema{Test}
                \\dificultad{1}
                \\begin{ejercicio}
                Precio \\$100. Fórmula $z^2$
                \\end{ejercicio}
                """;

        LatexContentExtractor.ExtractionResult result = extractor.extract(tex);

        assertThat(result.getFormulas()).containsExactly("z^2");
        assertThat(result.getFormulas()).noneMatch(f -> f.contains("100") || f.contains("Precio"));
    }

    @Test
    @DisplayName("extract: no captura $$ como fórmula inline")
    void extractDoesNotCaptureDoubleDollarAsInline() {
        String tex = """
                \\bloque{Test}
                \\tema{Test}
                \\dificultad{1}
                \\begin{ejercicio}
                Display: $$x^2$$. Inline: $y^2$.
                \\end{ejercicio}
                """;

        LatexContentExtractor.ExtractionResult result = extractor.extract(tex);

        // Debe capturar ambos: el display y el inline
        assertThat(result.getFormulas()).hasSize(2);
        assertThat(result.getFormulas()).contains("x^2");
        assertThat(result.getFormulas()).contains("y^2");
    }

    @Test
    @DisplayName("extract: maneja fórmulas con llaves anidadas")
    void extractHandlesFormulasWithNestedBraces() {
        String tex = """
                \\bloque{Test}
                \\tema{Test}
                \\dificultad{1}
                \\begin{solucion}
                $$\\frac{\\mathrm{d}f}{\\mathrm{d}x} = \\lim_{h \\to 0} \\frac{f(x+h)-f(x)}{h}$$
                \\end{solucion}
                """;

        LatexContentExtractor.ExtractionResult result = extractor.extract(tex);

        assertThat(result.getFormulas()).hasSize(1);
        String formula = result.getFormulas().get(0);
        assertThat(formula).contains("\\frac");
        assertThat(formula).contains("\\mathrm{d}f");
        assertThat(formula).contains("\\lim");
    }

    @Test
    @DisplayName("extract: entornos con contenido vacío")
    void extractHandlesEmptyEnvironmentContent() {
        String tex = """
                \\bloque{Test}
                \\tema{Test}
                \\dificultad{1}
                \\begin{ejercicio}\\end{ejercicio}
                \\begin{solucion}  \\end{solucion}
                """;

        LatexContentExtractor.ExtractionResult result = extractor.extract(tex);

        assertThat(result.getEnvironments()).containsKey("ejercicio");
        assertThat(result.getEnvironments()).containsKey("solucion");
        assertThat(result.getEnvironments().get("ejercicio").get(0)).isEmpty();
        assertThat(result.getEnvironments().get("solucion").get(0)).isEmpty();
    }

    @Test
    @DisplayName("extract: múltiples entornos del mismo tipo acumulan contenido")
    void extractAccumulatesMultipleSameTypeEnvironments() {
        String tex = """
                \\bloque{Test}
                \\tema{Test}
                \\dificultad{1}
                \\begin{paso}Paso 1\\end{paso}
                \\begin{conceptoClave}Concepto 1\\end{conceptoClave}
                \\begin{paso}Paso 2\\end{paso}
                \\begin{conceptoClave}Concepto 2\\end{conceptoClave}
                """;

        LatexContentExtractor.ExtractionResult result = extractor.extract(tex);

        assertThat(result.getEnvironments().get("paso")).hasSize(2);
        assertThat(result.getEnvironments().get("conceptoClave")).hasSize(2);
    }

    @Test
    @DisplayName("extract: fórmulas en diferentes entornos se extraen globalmente")
    void extractFormulasFromAllEnvironments() {
        String tex = """
                \\bloque{Test}
                \\tema{Test}
                \\dificultad{1}
                \\begin{enunciado}
                Dada $f(x) = x^2$
                \\end{enunciado}
                \\begin{solucion}
                $$f'(x) = 2x$$
                \\end{solucion}
                """;

        LatexContentExtractor.ExtractionResult result = extractor.extract(tex);

        assertThat(result.getFormulas()).hasSize(2);
        assertThat(result.getFormulas()).contains("f(x) = x^2");
        assertThat(result.getFormulas()).contains("f'(x) = 2x");
    }

    // Helper para invocar método privado via reflexión
    private String invokeNormalize(String input) {
        try {
            var method = LatexContentExtractor.class.getDeclaredMethod("normalizeEnvironmentName", String.class);
            method.setAccessible(true);
            return (String) method.invoke(extractor, input);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}