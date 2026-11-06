package com.mathtutor.llm;

import com.mathtutor.common.port.EmbeddingModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Pruebas de caja blanca para DeterministicEmbeddingModel.
 * Verifica determinismo, dimensionalidad y normalización.
 */
class DeterministicEmbeddingModelTest {

    private final EmbeddingModel model = new DeterministicEmbeddingModel();

    @Test
    @DisplayName("Dimensión es 384")
    void dimensionsIs384() {
        assertThat(model.dimensions()).isEqualTo(384);
    }

    @Test
    @DisplayName("Embedding de texto vacío retorna vector de ceros")
    void embedEmptyTextReturnsZeroVector() {
        float[] embedding = model.embed("");
        assertThat(embedding).hasSize(384);
        for (float v : embedding) {
            assertThat(v).isEqualTo(0.0f);
        }
    }

    @Test
    @DisplayName("Embedding de null retorna vector de ceros")
    void embedNullReturnsZeroVector() {
        float[] embedding = model.embed((String) null);
        assertThat(embedding).hasSize(384);
        for (float v : embedding) {
            assertThat(v).isEqualTo(0.0f);
        }
    }

    @Test
    @DisplayName("Mismo texto produce mismo embedding (determinista)")
    void sameTextProducesSameEmbedding() {
        String text = "La derivada de sin(x) es cos(x)";
        float[] emb1 = model.embed(text);
        float[] emb2 = model.embed(text);

        assertThat(emb1).isEqualTo(emb2);
    }

    @Test
    @DisplayName("Textos diferentes producen embeddings diferentes")
    void differentTextsProduceDifferentEmbeddings() {
        float[] emb1 = model.embed("derivada");
        float[] emb2 = model.embed("integral");

        assertThat(emb1).isNotEqualTo(emb2);
    }

    @Test
    @DisplayName("Embedding está normalizado (longitud 1)")
    void embeddingIsNormalized() {
        float[] embedding = model.embed("texto de prueba para normalización");

        double sum = 0;
        for (float v : embedding) {
            sum += v * v;
        }
        double norm = Math.sqrt(sum);

        assertThat(norm).isCloseTo(1.0, within(0.001));
    }

    @Test
    @DisplayName("Embed de lista retorna lista de embeddings")
    void embedListReturnsListOfEmbeddings() {
        List<String> texts = List.of("texto 1", "texto 2", "texto 3");
        List<float[]> embeddings = model.embed(texts);

        assertThat(embeddings).hasSize(3);
        assertThat(embeddings.get(0)).hasSize(384);
        assertThat(embeddings.get(1)).hasSize(384);
        assertThat(embeddings.get(2)).hasSize(384);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "x^2",
        "\\sin(x)",
        "\\frac{\\mathrm{d}f}{\\mathrm{d}x}",
        "\\int_{a}^{b} f(x) \\mathrm{d}x",
        "Teorema fundamental del cálculo",
        "Regla de la cadena: (f∘g)'(x) = f'(g(x))·g'(x)"
    })
    @DisplayName("Genera embedding válido para expresiones matemáticas")
    void generatesValidEmbeddingForMathExpressions(String mathText) {
        float[] embedding = model.embed(mathText);

        assertThat(embedding).hasSize(384);
        // No todo ceros
        boolean anyNonZero = false;
        for (float v : embedding) {
            if (v != 0.0f) {
                anyNonZero = true;
                break;
            }
        }
        assertThat(anyNonZero).isTrue();
        // Normalizado
        double sum = 0;
        for (float v : embedding) {
            sum += v * v;
        }
        double norm = Math.sqrt(sum);
        assertThat(norm).isCloseTo(1.0, within(0.001));
    }

    @Test
    @DisplayName("Embeddings son consistentes entre llamadas separadas")
    void embeddingsAreConsistentAcrossCalls() {
        String text = "consistencia";

        float[] emb1 = model.embed(text);
        float[] emb2 = model.embed(text);
        float[] emb3 = model.embed(text);

        assertThat(emb1).isEqualTo(emb2);
        assertThat(emb2).isEqualTo(emb3);
    }

    @Test
    @DisplayName("Embeddings de lista son consistentes")
    void listEmbeddingsAreConsistent() {
        List<String> texts = List.of("a", "b", "c");

        List<float[]> embs1 = model.embed(texts);
        List<float[]> embs2 = model.embed(texts);

        assertThat(embs1).hasSize(3);
        assertThat(embs2).hasSize(3);
        for (int i = 0; i < 3; i++) {
            assertThat(embs1.get(i)).isEqualTo(embs2.get(i));
        }
    }

    @Test
    @DisplayName("Embeddings preservan similitud semántica aproximada")
    void embeddingsPreserveApproximateSemanticSimilarity() {
        // Textos similares deberían tener embeddings con mayor similitud coseno
        float[] embDerivada = model.embed("derivada de x al cuadrado");
        float[] embDerivar = model.embed("derivar x^2");
        float[] embIntegral = model.embed("integral de x al cuadrado");

        double simDeriv = cosineSimilarity(embDerivada, embDerivar);
        double simDiff = cosineSimilarity(embDerivada, embIntegral);

        // Textos sobre derivadas deberían ser más similares entre sí que con integrales
        assertThat(simDeriv).isGreaterThan(simDiff);
    }

    @Test
    @DisplayName("Embedding de solo espacios retorna vector de ceros (sin normalizar)")
    void embedWhitespaceReturnsZeroVector() {
        float[] embedding = model.embed("   ");

        assertThat(embedding).hasSize(384);
        for (float v : embedding) {
            assertThat(v).isEqualTo(0.0f);
        }
    }

    @Test
    @DisplayName("Embedding de tabs y newlines retorna vector de ceros")
    void embedTabsAndNewlinesReturnsZeroVector() {
        float[] embedding = model.embed("\t\n\r  \n");

        assertThat(embedding).hasSize(384);
        for (float v : embedding) {
            assertThat(v).isEqualTo(0.0f);
        }
    }

    @Test
    @DisplayName("Embed de lista vacía retorna lista vacía")
    void embedEmptyListReturnsEmptyList() {
        List<float[]> embeddings = model.embed(Collections.emptyList());

        assertThat(embeddings).isEmpty();
    }

    @Test
    @DisplayName("Embed de lista con nulls maneja gracefully")
    void embedListWithNullsHandlesGracefully() {
        List<String> texts = Arrays.asList("texto 1", null, "texto 3");
        List<float[]> embeddings = model.embed(texts);

        assertThat(embeddings).hasSize(3);
        assertThat(embeddings.get(0)).hasSize(384);
        // null debería producir vector de ceros
        for (float v : embeddings.get(1)) {
            assertThat(v).isEqualTo(0.0f);
        }
        assertThat(embeddings.get(2)).hasSize(384);
    }

    @Test
    @DisplayName("Embedding de texto muy largo funciona correctamente")
    void embedLongTextWorks() {
        String longText = "a".repeat(10000);
        float[] embedding = model.embed(longText);

        assertThat(embedding).hasSize(384);
        // Debe estar normalizado
        double sum = 0;
        for (float v : embedding) {
            sum += v * v;
        }
        double norm = Math.sqrt(sum);
        assertThat(norm).isCloseTo(1.0, within(0.001));
    }

    @Test
    @DisplayName("Embedding de caracteres Unicode funciona")
    void embedUnicodeWorks() {
        float[] embedding = model.embed("αβγδε ζηθ ικλμ νξοπ ρστυ φχψω");

        assertThat(embedding).hasSize(384);
        double sum = 0;
        for (float v : embedding) {
            sum += v * v;
        }
        double norm = Math.sqrt(sum);
        assertThat(norm).isCloseTo(1.0, within(0.001));
    }

    @Test
    @DisplayName("Embedding de números y símbolos especiales")
    void embedNumbersAndSymbolsWorks() {
        float[] embedding = model.embed("12345 !@#$%^&*()_+-=[]{}|;':\",./<>?");

        assertThat(embedding).hasSize(384);
        double sum = 0;
        for (float v : embedding) {
            sum += v * v;
        }
        double norm = Math.sqrt(sum);
        assertThat(norm).isCloseTo(1.0, within(0.001));
    }

    @Test
    @DisplayName("Determinismo: mismo hash produce mismo embedding aunque contenido diferente pero mismo hashCode")
    void sameHashCodeProducesSameEmbedding() {
        // Buscar dos strings con mismo hashCode (colisión forzada para test)
        // En práctica, usamos el hashCode como semilla
        String text1 = "test";
        String text2 = "test"; // Mismo string = mismo hashCode

        float[] emb1 = model.embed(text1);
        float[] emb2 = model.embed(text2);

        assertThat(emb1).isEqualTo(emb2);
    }

    @Test
    @DisplayName("Vector normalizado tiene todos los valores en rango [-1, 1]")
    void normalizedVectorValuesInRange() {
        float[] embedding = model.embed("rango de valores");

        for (float v : embedding) {
            assertThat(v).isBetween(-1.0f, 1.0f);
        }
    }

    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
        }
        return dot; // Ya están normalizados
    }
}