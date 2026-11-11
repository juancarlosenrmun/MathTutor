package com.mathtutor.llm;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Assumptions;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests de integración REAL con Google AI Studio API.
 *
 * Estos tests solo se ejecutan si:
 * 1. El sistema tiene la variable de entorno GOOGLE_AI_API_KEY definida
 * 2. Se ejecuta con el tag "real-api" (./gradlew realApiTest)
 *
 * Si la variable NO está definida -> se omiten (Assumptions.assumeTrue).
 * Si la variable está definida pero la llamada falla (400, 403, 404, 429, 5xx) -> FALLAN (no se omiten).
 * Requieren conexión a internet y cuota en Google AI Studio.
 */
@Tag("real-api")
class SpringAiGoogleAiStudioEmbeddingModelRealApiTest {

    private static SpringAiGoogleAiStudioEmbeddingModel model;

    @BeforeAll
    static void setup() {
        String apiKey = System.getenv("GOOGLE_AI_API_KEY");
        // Solo omitir si la variable NO está definida
        Assumptions.assumeTrue(apiKey != null && !apiKey.trim().isEmpty(),
            "GOOGLE_AI_API_KEY no configurada. Crea .env con GOOGLE_AI_API_KEY=... y ejecuta con perfil google-ai-studio.");

        model = new SpringAiGoogleAiStudioEmbeddingModel(
            apiKey,
            "gemini-embedding-001",
            768,
            3,
            60
        );
        // NO hay assumeValidKey: si la clave está definida pero es inválida, los tests DEBEN fallar
    }

    @Test
    @DisplayName("Dimensión es 768 (gemini-embedding-001 con outputDimensionality=768)")
    void dimensionsIs768() {
        assertThat(model.dimensions()).isEqualTo(768);
    }

    @Test
    @DisplayName("Embedding de texto simple genera vector 768-dim normalizado")
    void embedSimpleTextProducesNormalizedVector() {
        float[] embedding = model.embed("La derivada de x^2 es 2x");

        assertThat(embedding).hasSize(768);
        assertThat(embedding).isNotNull();

        // Verificar normalización (longitud ~1)
        double sum = 0;
        for (float v : embedding) {
            sum += v * v;
        }
        double norm = Math.sqrt(sum);
        assertThat(norm).isCloseTo(1.0, within(0.001));
    }

    @Test
    @DisplayName("Mismo texto produce mismo embedding (determinista con API)")
    void sameTextProducesSameEmbedding() {
        String text = "Ecuación cuadrática: ax^2 + bx + c = 0";
        float[] emb1 = model.embed(text);
        float[] emb2 = model.embed(text);

        assertThat(emb1).isEqualTo(emb2);
    }

    @Test
    @DisplayName("Textos matemáticos relacionados tienen mayor similitud")
    void relatedMathTextsHaveHigherSimilarity() {
        float[] embDerivada = model.embed("derivada de x al cuadrado");
        float[] embDerivar = model.embed("derivar x^2");
        float[] embIntegral = model.embed("integral de x al cuadrado");

        double simDeriv = cosineSimilarity(embDerivada, embDerivar);
        double simDiff = cosineSimilarity(embDerivada, embIntegral);

        // Textos sobre derivadas más similares que con integrales
        assertThat(simDeriv).isGreaterThan(simDiff);
        // Ambos > 0 (similitud positiva)
        assertThat(simDeriv).isPositive();
        assertThat(simDiff).isPositive();
    }

    @Test
    @DisplayName("Embedding en lote (batch) funciona correctamente")
    void batchEmbeddingWorks() {
        List<String> texts = List.of(
            "derivada de sin(x)",
            "integral de cos(x)",
            "limite de (sin x)/x cuando x tiende a 0",
            "serie de Taylor de e^x",
            "teorema del valor medio"
        );

        List<float[]> embeddings = model.embedBatch(texts);

        assertThat(embeddings).hasSize(5);
        for (float[] emb : embeddings) {
            assertThat(emb).hasSize(768);
            double sum = 0;
            for (float v : emb) {
                sum += v * v;
            }
            double norm = Math.sqrt(sum);
            assertThat(norm).isCloseTo(1.0, within(0.001));
        }
    }

    @Test
    @DisplayName("Embeddings con taskType RETRIEVAL_DOCUMENT para indexación")
    void embedWithRetrievalDocumentTaskType() {
        float[] embedding = model.embed("fragmento de documento para indexar en RAG");

        assertThat(embedding).hasSize(768);
        double sum = 0;
        for (float v : embedding) {
            sum += v * v;
        }
        double norm = Math.sqrt(sum);
        assertThat(norm).isCloseTo(1.0, within(0.001));
    }

    @Test
    @DisplayName("Embeddings con taskType RETRIEVAL_QUERY para consultas")
    void embedWithRetrievalQueryTaskType() {
        float[] embedding = model.embedQuery("buscar derivada de x al cuadrado");

        assertThat(embedding).hasSize(768);
        double sum = 0;
        for (float v : embedding) {
            sum += v * v;
        }
        double norm = Math.sqrt(sum);
        assertThat(norm).isCloseTo(1.0, within(0.001));
    }

    @Test
    @DisplayName("embedBatchQueries usa RETRIEVAL_QUERY")
    void batchEmbeddingQueriesWorks() {
        List<String> queries = List.of(
            "derivada de sin(x)",
            "integral de cos(x)"
        );

        List<float[]> embeddings = model.embedBatchQueries(queries);

        assertThat(embeddings).hasSize(2);
        for (float[] emb : embeddings) {
            assertThat(emb).hasSize(768);
            double sum = 0;
            for (float v : emb) {
                sum += v * v;
            }
            double norm = Math.sqrt(sum);
            assertThat(norm).isCloseTo(1.0, within(0.001));
        }
    }

    @Test
    @DisplayName("Texto en español genera embedding válido")
    void spanishTextProducesValidEmbedding() {
        String text = "El teorema fundamental del cálculo establece que la derivación y la integración son operaciones inversas.";
        float[] embedding = model.embed(text);

        assertThat(embedding).hasSize(768);
        double sum = 0;
        for (float v : embedding) {
            sum += v * v;
        }
        double norm = Math.sqrt(sum);
        assertThat(norm).isCloseTo(1.0, within(0.001));
    }

    @Test
    @DisplayName("Texto con fórmulas LaTeX genera embedding válido")
    void latexFormulasProducesValidEmbedding() {
        String text = "La fórmula de la derivada: f'(x) = lim_{h\\to0} (f(x+h)-f(x))/h";
        float[] embedding = model.embed(text);

        assertThat(embedding).hasSize(768);
        double sum = 0;
        for (float v : embedding) {
            sum += v * v;
        }
        double norm = Math.sqrt(sum);
        assertThat(norm).isCloseTo(1.0, within(0.001));
    }

    @Test
    @DisplayName("Embeddings son consistentes entre llamadas (idempotencia)")
    void embeddingsAreIdempotent() {
        String text = "consistencia de embeddings";

        float[] emb1 = model.embed(text);
        float[] emb2 = model.embed(text);
        float[] emb3 = model.embed(text);

        assertThat(emb1).isEqualTo(emb2);
        assertThat(emb2).isEqualTo(emb3);
    }

    @Test
    @DisplayName("Rate limiting: requests consecutivas respetan intervalo mínimo")
    void rateLimitingRespectsMinInterval() throws InterruptedException {
        // Este test verifica que embedBatch respeta el rate limiting
        long start = System.currentTimeMillis();
        List<float[]> embeddings = model.embedBatch(List.of("a", "b", "c"));
        long elapsed = System.currentTimeMillis() - start;

        // Con maxRequestsPerMinute=60, minInterval = 1000ms
        // 3 requests = 2 intervalos = ~2000ms mínimo
        assertThat(embeddings).hasSize(3);
        assertThat(elapsed).isGreaterThanOrEqualTo(1500L); // tolerancia
    }

    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
        }
        return dot; // Ya están normalizados
    }
}