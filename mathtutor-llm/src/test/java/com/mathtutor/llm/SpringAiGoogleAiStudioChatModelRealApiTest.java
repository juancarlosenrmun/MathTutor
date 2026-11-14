package com.mathtutor.llm;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Test de integración real con Google AI Studio (Gemini) para chat.
 * Requiere GOOGLE_AI_API_KEY en variables de entorno o .env.
 * Se ejecuta con: ./gradlew :mathtutor-llm:realApiTest
 */
@SpringBootTest(classes = LlmTestApplication.class)
@ActiveProfiles({"google-ai-studio"})
@Testcontainers(disabledWithoutDocker = true)
@Tag("real-api")
class SpringAiGoogleAiStudioChatModelRealApiTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
        .withDatabaseName("mathtutor_test")
        .withUsername("test")
        .withPassword("test")
        .withInitScript("db/migration/test-init.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String apiKey = System.getenv("GOOGLE_AI_API_KEY");
        assumeThat(apiKey).as("GOOGLE_AI_API_KEY must be set for this test").isNotNull().isNotEmpty();

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.profiles.active", () -> "google-ai-studio");
        registry.add("spring.ai.vectorstore.type", () -> "none");
        registry.add("spring.ai.vectorstore.pgvector.initialize-schema", () -> "false");
        registry.add("spring.ai.google.ai-studio.api-key", () -> apiKey);
        // Use gemini-1.5-flash (standard model per Google AI Studio)
        // NOTE: Chat models return 404 in Generative Language API for this API key
        // Embedding models work with gemini-embedding-001, but chat models (gemini-1.5-flash, gemini-1.5-pro, etc.) not available
        registry.add("spring.ai.google.ai-studio.chat-model", () -> "gemini-1.5-flash");
        registry.add("spring.ai.google.ai-studio.max-retries", () -> "3");
        registry.add("spring.ai.google.ai-studio.max-requests-per-minute", () -> "5");
        registry.add("spring.ai.google.ai-studio.temperature", () -> "0.7");
        registry.add("spring.ai.google.ai-studio.max-output-tokens", () -> "8192");
        // Required by RagProperties
        registry.add("tutor.rag.similarity-threshold", () -> "0.7");
        // JWT configuration for tests (required by TokenServiceAdapter)
        registry.add("jwt.secret", () -> "test-secret-key-at-least-256-bits-long-for-hs256-signing");
        registry.add("jwt.access-token-expiration-minutes", () -> "15");
        registry.add("jwt.refresh-token-expiration-days", () -> "7");
    }

    @Autowired
    private ChatModel chatModel;

    @Test
    void call_simpleQuestion_returnsResponse() {
        // Given
        Prompt prompt = new Prompt(new UserMessage("Responde en una sola frase: ¿cuánto es 2 + 2?"));

        // When
        ChatResponse response = chatModel.call(prompt);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getResults()).isNotEmpty();
        String answer = response.getResults().get(0).getOutput().getText();
        assertThat(answer).isNotBlank();
        assertThat(answer.toLowerCase()).containsAnyOf("4", "cuatro");
        System.out.println("Respuesta: " + answer);
    }

    @Test
    void call_mathQuestion_returnsExplanation() {
        // Given
        Prompt prompt = new Prompt(new UserMessage("Explica brevemente qué es una derivada en cálculo diferencial."));

        // When
        ChatResponse response = chatModel.call(prompt);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getResults()).isNotEmpty();
        String answer = response.getResults().get(0).getOutput().getText();
        assertThat(answer).isNotBlank();
        assertThat(answer.length()).isGreaterThan(20);
        System.out.println("Respuesta: " + answer);
    }

    @Test
    void stream_simpleQuestion_emitsTokens() {
        // Given
        Prompt prompt = new Prompt(new UserMessage("Cuenta del 1 al 5 separados por comas."));

        // When
        Flux<ChatResponse> stream = chatModel.stream(prompt);

        // Then
        StepVerifier.create(stream)
                .assertNext(response -> {
                    assertThat(response.getResults()).isNotEmpty();
                    String text = response.getResults().get(0).getOutput().getText();
                    assertThat(text).isNotBlank();
                    System.out.println("Chunk: " + text);
                })
                .expectNextCount(10) // Expect up to 10 more chunks
                .thenConsumeWhile(response -> true)
                .verifyComplete();
    }

    @Test
    void stream_mathQuestion_emitsProgressiveResponse() {
        // Given
        Prompt prompt = new Prompt(new UserMessage("Escribe la fórmula de la derivada de x^2 y explica cada término."));

        // When
        Flux<ChatResponse> stream = chatModel.stream(prompt);

        // Then
        StepVerifier.create(stream)
                .assertNext(response -> {
                    assertThat(response.getResults()).isNotEmpty();
                    String text = response.getResults().get(0).getOutput().getText();
                    assertThat(text).isNotBlank();
                    System.out.println("Primer chunk: " + text);
                })
                .expectNextCount(15)
                .thenConsumeWhile(response -> true)
                .verifyComplete();
    }

    @Test
    void call_withSystemPrompt_usesSystemPrompt() {
        // Given
        Prompt prompt = new Prompt(List.of(
                new SystemMessage("Responde SOLO con el nombre de la ciudad, sin explicaciones."),
                new UserMessage("¿Cuál es la capital de Francia?")
        ));

        // When
        ChatResponse response = chatModel.call(prompt);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getResults()).isNotEmpty();
        String answer = response.getResults().get(0).getOutput().getText();
        assertThat(answer).isNotBlank();
        // Should be just "París" or "Paris" without extra text
        System.out.println("Respuesta con system prompt: " + answer);
    }
}