package com.mathtutor.llm;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Adaptador de Spring AI EmbeddingModel que usa Google AI Studio (Gemini) API directamente.
 * Modelo: gemini-embedding-001 (3072 dimensiones nativas, configurable via outputDimensionality a 768) - multilingüe.
 *
 * Perfil: "google-ai-studio" para activar cuando se tiene API key de Google AI Studio.
 *
 * API Reference: https://ai.google.dev/gemini-api/docs/embeddings
 */
@Component
@Profile("google-ai-studio")
public class SpringAiGoogleAiStudioEmbeddingModel implements EmbeddingModel {

    private static final int DEFAULT_DIMENSIONS = 768;
    private static final String DEFAULT_EMBEDDING_MODEL = "gemini-embedding-001";
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/";

    private final RestClient restClient;
    private final String apiKey;
    private final String embeddingModel;
    private final int dimensions;
    private final int maxRetries;
    private final int maxRequestsPerMinute;

    public SpringAiGoogleAiStudioEmbeddingModel(
            @Value("${spring.ai.google.ai-studio.api-key:}") String apiKey,
            @Value("${spring.ai.google.ai-studio.embedding-model:" + DEFAULT_EMBEDDING_MODEL + "}") String embeddingModel,
            @Value("${spring.ai.google.ai-studio.embedding-dimensions:" + DEFAULT_DIMENSIONS + "}") int dimensions,
            @Value("${spring.ai.google.ai-studio.max-retries:3}") int maxRetries,
            @Value("${spring.ai.google.ai-studio.max-requests-per-minute:60}") int maxRequestsPerMinute) {
        Assert.hasText(apiKey, "Google AI Studio API key must be set via spring.ai.google.ai-studio.api-key or GOOGLE_AI_API_KEY env var");
        Assert.hasText(embeddingModel, "Embedding model name must be set");
        this.apiKey = apiKey;
        this.embeddingModel = embeddingModel;
        this.dimensions = dimensions;
        this.maxRetries = maxRetries;
        this.maxRequestsPerMinute = maxRequestsPerMinute;
        // Use x-goog-api-key header as per Google AI Studio API documentation
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL + "models/" + embeddingModel + ":embedContent")
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("x-goog-api-key", apiKey)
                .build();
    }

    @Override
    public float[] embed(Document document) {
        return embed(document.getText());
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<String> instructions = request.getInstructions();
        List<Embedding> embeddings = new ArrayList<>();
        for (int i = 0; i < instructions.size(); i++) {
            String instruction = instructions.get(i);
            float[] embedding = embed(instruction);
            embeddings.add(new Embedding(embedding, i));
        }
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public float[] embed(String text) {
        return embed(text, "RETRIEVAL_DOCUMENT");
    }

    /**
     * Genera embedding para consulta (query) usando taskType RETRIEVAL_QUERY.
     * Útil para búsquedas de similitud donde el query debe compararse con documentos indexados con RETRIEVAL_DOCUMENT.
     */
    public float[] embedQuery(String text) {
        return embed(text, "RETRIEVAL_QUERY");
    }

    /**
     * Genera embedding con taskType configurable (RETRIEVAL_DOCUMENT para indexación, RETRIEVAL_QUERY para búsquedas).
     */
    private float[] embed(String text, String taskType) {
        if (text == null || text.trim().isEmpty()) {
            return new float[dimensions];
        }

        try {
            Map<String, Object> request = Map.of(
                    "content", Map.of("parts", List.of(Map.of("text", text))),
                    "taskType", taskType,
                    "outputDimensionality", dimensions
            );

            Map<String, Object> response = executeWithRetry(() -> restClient.post()
                    .uri(uriBuilder -> uriBuilder.build())
                    .body(request)
                    .retrieve()
                    .body(Map.class));

            @SuppressWarnings("unchecked")
            List<Number> embeddingValues = (List<Number>) ((Map<String, Object>) response.get("embedding")).get("values");

            float[] embedding = new float[embeddingValues.size()];
            for (int i = 0; i < embeddingValues.size(); i++) {
                embedding[i] = embeddingValues.get(i).floatValue();
            }

            // Validar dimensión esperada
            if (embedding.length != dimensions) {
                throw new IllegalStateException("Embedding dimension mismatch: expected " + dimensions + ", got " + embedding.length);
            }

            // Normalizar a longitud 1 para similitud coseno
            normalize(embedding);
            return embedding;

        } catch (RestClientResponseException e) {
            if (e.getRawStatusCode() == 429) {
                throw new IllegalStateException("Rate limited by Google AI Studio (429). Configure rate limiting or retry later.", e);
            }
            throw new IllegalStateException("Error calling Google AI Studio embedding API: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IllegalStateException("Error calling Google AI Studio embedding API: " + e.getMessage(), e);
        }
    }

    /**
     * Embeddings en lote para mejor rendimiento (máx. 250 textos por request según API).
     * Usa taskType RETRIEVAL_DOCUMENT para indexación.
     */
    public List<float[]> embedBatch(List<String> texts) {
        return embedBatch(texts, "RETRIEVAL_DOCUMENT");
    }

    /**
     * Embeddings en lote para consultas.
     * Usa taskType RETRIEVAL_QUERY.
     */
    public List<float[]> embedBatchQueries(List<String> texts) {
        return embedBatch(texts, "RETRIEVAL_QUERY");
    }

    private List<float[]> embedBatch(List<String> texts, String taskType) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        // Google AI Studio embedding API no soporta batch nativo en v1beta
        // Procesamos secuencialmente con rate limiting
        List<float[]> results = new ArrayList<>(texts.size());
        long minIntervalMs = 60_000L / Math.max(1, maxRequestsPerMinute);

        for (int i = 0; i < texts.size(); i++) {
            if (i > 0) {
                // Rate limiting simple entre requests
                try {
                    Thread.sleep(minIntervalMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted during rate limiting", e);
                }
            }
            results.add(embed(texts.get(i), taskType));
        }
        return results;
    }

    @Override
    public int dimensions() {
        return dimensions;
    }

    /**
     * Normaliza un vector a longitud unitaria.
     */
    private void normalize(float[] vector) {
        double sum = 0;
        for (float v : vector) {
            sum += v * v;
        }
        double norm = Math.sqrt(sum);
        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] = (float) (vector[i] / norm);
            }
        }
    }

    /**
     * Ejecuta una operación con reintentos exponenciales y jitter para 429/5xx.
     */
    private <T> T executeWithRetry(java.util.function.Supplier<T> operation) {
        int attempt = 0;
        while (true) {
            try {
                return operation.get();
            } catch (RestClientResponseException e) {
                int status = e.getRawStatusCode();
                boolean retryable = status == 429 || status >= 500;
                if (!retryable || attempt >= maxRetries) {
                    throw e;
                }
                attempt++;
                long baseDelay = (long) Math.pow(2, attempt) * 1000L; // 2s, 4s, 8s...
                long jitter = ThreadLocalRandom.current().nextLong(0, 1000);
                long delay = baseDelay + jitter;
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted during retry", ie);
                }
            }
        }
    }
}