package com.mathtutor.llm;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Adaptador Gemini: tareas de consulta/documento, timeout y caché acotada defensiva. */
@Component
@Profile("google-ai-studio")
public class SpringAiGoogleAiStudioEmbeddingModel implements EmbeddingModel {
    private final RestClient restClient;
    private final int dimensions;
    private final GoogleAiStudioRateLimiter rateLimiter;
    private final Map<String, float[]> cache = Collections.synchronizedMap(new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, float[]> eldest) { return size() > 256; }
    });

    @Autowired
    public SpringAiGoogleAiStudioEmbeddingModel(
            @Value("${spring.ai.google.ai-studio.api-key:}") String apiKey,
            @Value("${spring.ai.google.ai-studio.embedding-model:gemini-embedding-001}") String embeddingModel,
            @Value("${spring.ai.google.ai-studio.embedding-dimensions:768}") int dimensions,
            @Value("${spring.ai.google.ai-studio.max-retries:0}") int maxRetries,
            @Value("${spring.ai.google.ai-studio.max-requests-per-minute:5}") int maxRequestsPerMinute) {
        this(apiKey, embeddingModel, dimensions, maxRetries, maxRequestsPerMinute,
                createClient(apiKey, embeddingModel));
    }

    SpringAiGoogleAiStudioEmbeddingModel(String apiKey, String model, int dimensions,
            int maxRetries, int maxRequestsPerMinute, RestClient restClient) {
        Assert.hasText(apiKey, "Google AI Studio API key must be set");
        Assert.hasText(model, "Debe configurarse el modelo de embeddings");
        Assert.isTrue(dimensions > 0 && dimensions <= 3072, "La dimensión debe estar entre 1 y 3072");
        Assert.isTrue(maxRetries >= 0, "El número de reintentos no puede ser negativo");
        this.restClient = restClient.mutate().defaultHeader("x-goog-api-key", apiKey).build();
        this.dimensions = dimensions;
        this.rateLimiter = new GoogleAiStudioRateLimiter(maxRequestsPerMinute);
    }

    private static RestClient createClient(String apiKey, String model) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(5000);
        return RestClient.builder().baseUrl("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":embedContent")
                .defaultHeader("x-goog-api-key", apiKey == null ? "" : apiKey)
                .requestFactory(factory).build();
    }

    @Override
    public float[] embed(Document document) { return embed(document.getText(), "RETRIEVAL_DOCUMENT"); }

    @Override
    public List<float[]> embed(List<Document> documents, org.springframework.ai.embedding.EmbeddingOptions options,
            org.springframework.ai.embedding.BatchingStrategy batchingStrategy) {
        Assert.notNull(documents, "Los documentos no pueden ser nulos");
        List<float[]> vectors = new ArrayList<>(documents.size());
        for (List<Document> batch : batchingStrategy.batch(documents))
            for (Document document : batch) vectors.add(embed(document));
        return vectors;
    }

    @Override
    public float[] embed(String text) { return embedQuery(text); }

    public float[] embedQuery(String text) { return embed(text, "RETRIEVAL_QUERY"); }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> results = new ArrayList<>();
        for (int i = 0; i < request.getInstructions().size(); i++)
            results.add(new Embedding(embed(request.getInstructions().get(i)), i));
        return new EmbeddingResponse(results);
    }

    private float[] embed(String text, String taskType) {
        if (text == null || text.isBlank()) return new float[dimensions];
        String cacheKey = taskType + ":" + text;
        float[] cached = cache.get(cacheKey);
        if (cached != null) return cached.clone();
        Assert.state(!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive(),
                "No se permite llamar a Google AI Studio dentro de una transacción");
        rateLimiter.acquire();
        try {
            Map<?, ?> response = restClient.post().body(Map.of(
                    "content", Map.of("parts", List.of(Map.of("text", text))),
                    "taskType", taskType, "outputDimensionality", dimensions))
                    .retrieve().body(Map.class);
            if (response == null || !(response.get("embedding") instanceof Map<?, ?> embedding)
                    || !(embedding.get("values") instanceof List<?> values))
                throw new IllegalStateException("Respuesta de embedding ausente");
            if (values.size() != dimensions) throw new IllegalStateException("La dimensión del embedding no coincide con la configurada");
            float[] vector = new float[dimensions];
            double sum = 0;
            for (int i = 0; i < dimensions; i++) {
                if (!(values.get(i) instanceof Number number) || !Float.isFinite(number.floatValue()))
                    throw new IllegalStateException("El vector contiene valores no finitos");
                vector[i] = number.floatValue();
                sum += (double) vector[i] * vector[i];
            }
            if (sum == 0) throw new IllegalStateException("El vector no puede ser nulo");
            double norm = Math.sqrt(sum);
            for (int i = 0; i < vector.length; i++) vector[i] = (float) (vector[i] / norm);
            cache.put(cacheKey, vector.clone());
            return vector;
        } catch (org.springframework.web.client.RestClientException e) {
            // No propagar el cuerpo del proveedor: puede contener contenido sensible.
            String status = e instanceof org.springframework.web.client.RestClientResponseException http
                    ? " (HTTP " + http.getStatusCode().value() + ")" : "";
            throw new IllegalStateException("Error de Google AI Studio al generar el vector" + status);
        }
    }

    public List<float[]> embedBatch(List<String> texts) { return embedBatch(texts, "RETRIEVAL_DOCUMENT"); }
    public List<float[]> embedBatchQueries(List<String> texts) { return embedBatch(texts, "RETRIEVAL_QUERY"); }
    private List<float[]> embedBatch(List<String> texts, String taskType) {
        if (texts == null || texts.isEmpty()) return List.of();
        return texts.stream().map(text -> embed(text, taskType)).toList();
    }

    @Override
    public int dimensions() { return dimensions; }
}
