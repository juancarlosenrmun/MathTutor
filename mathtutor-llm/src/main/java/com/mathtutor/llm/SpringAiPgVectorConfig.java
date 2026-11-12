package com.mathtutor.llm;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;

/**
 * Configuración de Spring AI PgVectorStore para tests de integración.
 * Perfiles disponibles:
 * - "spring-ai-pgvector" + "ollama": usa OllamaEmbeddingModel local (nomic-embed-text, 768 dim)
 * - "spring-ai-pgvector" + "openai": usa OpenAiEmbeddingModel (requiere API key en OPENAI_API_KEY)
 * - "spring-ai-pgvector" + "google-ai-studio": usa Google AI Studio (Gemini text-embedding-004, 768 dim)
 * - "spring-ai-pgvector" (solo): usa DeterministicEmbeddingModel para tests sin claves API (384 dim)
 * - "spring-ai-pgvector" + "deterministic": usa SlowDeterministicEmbeddingModel para tests RNF-03 (2s delay)
 */
@Configuration
@Profile("spring-ai-pgvector")
public class SpringAiPgVectorConfig {

    @Bean
    @Profile("ollama")
    @Primary
    public EmbeddingModel ollamaEmbeddingModel(OllamaEmbeddingModel ollamaModel) {
        return new SpringAiOllamaEmbeddingModel(ollamaModel);
    }

    @Bean
    @Profile("openai")
    @Primary
    public EmbeddingModel openAiEmbeddingModel(
            @Value("${spring.ai.openai.api-key:}") String apiKey,
            @Value("${spring.ai.openai.embedding.model:text-embedding-3-small}") String model) {
        Assert.hasText(apiKey, "OpenAI API key must be set via spring.ai.openai.api-key or OPENAI_API_KEY env var");
        // OpenAiApi constructor in Spring AI 1.0.0 requires only the API key
        OpenAiApi openAiApi = OpenAiApi.builder().apiKey(apiKey).build();
        return new OpenAiEmbeddingModel(openAiApi);
    }

    @Bean
    @Profile("google-ai-studio")
    @Primary
    public EmbeddingModel googleAiStudioEmbeddingModel(
            @Value("${spring.ai.google.ai-studio.api-key:}") String apiKey,
            @Value("${spring.ai.google.ai-studio.embedding-model:text-embedding-004}") String embeddingModel,
            @Value("${spring.ai.google.ai-studio.embedding-dimensions:768}") int dimensions,
            @Value("${spring.ai.google.ai-studio.max-retries:3}") int maxRetries,
            @Value("${spring.ai.google.ai-studio.max-requests-per-minute:60}") int maxRequestsPerMinute) {
        return new SpringAiGoogleAiStudioEmbeddingModel(apiKey, embeddingModel, dimensions, maxRetries, maxRequestsPerMinute);
    }

    @Bean
    @Profile("!ollama & !openai & !google-ai-studio & !deterministic")
    public DeterministicEmbeddingModel deterministicEmbeddingModel() {
        return new DeterministicEmbeddingModel();
    }

    @Bean
    @Profile("!ollama & !openai & !google-ai-studio & !deterministic")
    public EmbeddingModel springAiEmbeddingModel(DeterministicEmbeddingModel delegate) {
        return new SpringAiDeterministicEmbeddingModel(delegate);
    }

    /**
     * Perfil "deterministic": usa DeterministicEmbeddingModel envuelto con retraso
     * para simular latencia de LLM real (tests RNF-03).
     */
    @Bean
    @Profile("deterministic")
    @Primary
    public SlowDeterministicEmbeddingModel slowDeterministicEmbeddingModel() {
        return new SlowDeterministicEmbeddingModel();
    }

    @Bean
    @Profile("deterministic")
    public EmbeddingModel springAiSlowEmbeddingModel(SlowDeterministicEmbeddingModel delegate) {
        return new SpringAiDeterministicEmbeddingModel(delegate);
    }

    @Bean
    public VectorStore vectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        // PgVectorStore builder requiere JdbcTemplate y EmbeddingModel
        // Las dimensiones se detectan automáticamente del embeddingModel
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .initializeSchema(true) // Crea tablas/índices si no existen
                .build();
    }
}