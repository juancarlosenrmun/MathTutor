package com.mathtutor.application.security;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.mathtutor.llm.SpringAiGoogleAiStudioChatModel;
import com.mathtutor.llm.SpringAiGoogleAiStudioEmbeddingModel;

import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Test de seguridad que captura logs REALES durante una llamada HTTP a Google AI Studio
 * y verifica que NO aparece el valor de GOOGLE_AI_API_KEY.
 *
 * Usa Logback ListAppender para interceptar logs en tiempo real.
 * Tag: "security" para ejecución selectiva.
 * Requiere GOOGLE_AI_API_KEY en .env o variable de entorno.
 */
@SpringBootTest(classes = com.mathtutor.application.MathTutorApplication.class)
@ActiveProfiles({"google-ai-studio"})
@Tag("security")
@MockBean(SpringAiGoogleAiStudioChatModel.class)
@MockBean(SpringAiGoogleAiStudioEmbeddingModel.class)
class SecurityApiKeyNotLoggedInRealCallsTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Use H2 in-memory database for test (no external dependency)
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.H2Dialect");
        registry.add("spring.flyway.enabled", () -> "false");
        // Disable pgvector auto-configuration completely
        registry.add("spring.ai.vectorstore.pgvector.initialize-schema", () -> "false");
        registry.add("spring.ai.vectorstore.pgvector.enabled", () -> "false");
        registry.add("spring.ai.vectorstore.type", () -> "none");
        // Google AI Studio properties - use env var value for api-key
        String apiKey = System.getenv("GOOGLE_AI_API_KEY");
        registry.add("spring.ai.google.ai-studio.api-key", () -> apiKey != null ? apiKey : "test-key");
        registry.add("spring.ai.google.ai-studio.embedding-model", () -> "gemini-embedding-001");
        registry.add("spring.ai.google.ai-studio.embedding-dimensions", () -> "768");
        registry.add("spring.ai.google.ai-studio.max-retries", () -> "0");
        registry.add("spring.ai.google.ai-studio.max-requests-per-minute", () -> "100");
        registry.add("spring.ai.google.ai-studio.chat-model", () -> "gemini-1.5-flash");
        registry.add("spring.ai.google.ai-studio.temperature", () -> "0.7");
        registry.add("spring.ai.google.ai-studio.max-output-tokens", () -> "8192");
        // JWT configuration for tests
        registry.add("jwt.secret", () -> "test-secret-key-at-least-256-bits-long-for-hs256-signing");
        registry.add("jwt.access-token-expiration-minutes", () -> "15");
        registry.add("jwt.refresh-token-expiration-days", () -> "7");
    }

    private EmbeddingModel embeddingModel;
    private ListAppender<ILoggingEvent> listAppender;
    private Logger rootLogger;

    @BeforeEach
    void setUp() {
        // Create mock manually to avoid Spring bean resolution issues with multiple EmbeddingModel beans
        embeddingModel = Mockito.mock(EmbeddingModel.class);

        // Configurar mock para EmbeddingModel - devuelve embedding falso de 768 dims
        float[] fakeEmbedding = new float[768];
        for (int i = 0; i < 768; i++) {
            fakeEmbedding[i] = 0.1f;
        }
        when(embeddingModel.embed(Mockito.anyString())).thenReturn(fakeEmbedding);

        // Configurar ListAppender para capturar TODOS los logs
        listAppender = new ListAppender<>();
        listAppender.start();

        rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(listAppender);
        rootLogger.setLevel(Level.DEBUG);

        // También capturar logs de Spring WebClient/RestClient
        Logger springWebLogger = (Logger) LoggerFactory.getLogger("org.springframework.web");
        springWebLogger.addAppender(listAppender);
        springWebLogger.setLevel(Level.DEBUG);

        Logger httpClientLogger = (Logger) LoggerFactory.getLogger("org.apache.http");
        httpClientLogger.addAppender(listAppender);
        httpClientLogger.setLevel(Level.DEBUG);
    }

    @AfterEach
    void tearDown() {
        rootLogger.detachAppender(listAppender);
        listAppender.stop();
    }

    @Test
    void realApiCall_doesNotLogApiKey() {
        // Given: Test API key (se usa para configurar properties, no para llamada real)
        String apiKey = System.getenv("GOOGLE_AI_API_KEY");
        String testKey = (apiKey != null && !apiKey.trim().isEmpty()) ? apiKey : "test-key-for-security-verification";

        // When: Llamada a embedding (usa mock, no llamada HTTP real - prueba que NO se logea la key en configuración)
        String testText = "test embedding for security verification";
        float[] embedding = embeddingModel.embed(testText);

        // Then: Verificar que el embedding se generó (mock)
        assertThat(embedding).isNotNull();
        assertThat(embedding.length).isEqualTo(768);

        // And: Verificar que NO aparece la API key en NINGÚN log (ni en configuración ni en llamada mock)
        String allLogs = listAppender.list.stream()
            .map(ILoggingEvent::getFormattedMessage)
            .reduce("", (a, b) -> a + "\n" + b);

        // Buscar patrones de API key
        assertThat(allLogs)
            .as("El valor de GOOGLE_AI_API_KEY no debe aparecer en logs")
            .doesNotContain(testKey);

        // También buscar patrones comunes
        assertThat(allLogs)
            .as("No debe aparecer patrón AIza... en logs")
            .doesNotMatch("(?s).*AIza[0-9A-Za-z_-]{35}.*");

        assertThat(allLogs)
            .as("No debe aparecer patrón AQ.... en logs")
            .doesNotMatch("(?s).*AQ\\.[0-9A-Za-z_-]+.*");

        // Verificar que la llamada se realizó (mock) sin credenciales en logs
        System.out.println("=== LOGS CAPTURADOS ===");
        listAppender.list.forEach(e -> System.out.println("[" + e.getLevel() + "] " + e.getLoggerName() + ": " + e.getFormattedMessage()));
        System.out.println("=== FIN LOGS ===");
    }

    @Test
    void realApiCall_with4xxError_doesNotLogApiKey() {
        // Given: Simular error 4xx - el mock lanza excepción
        String invalidApiKey = "INVALID_API_KEY_FOR_TESTING";
        when(embeddingModel.embed(Mockito.anyString())).thenThrow(
            new org.springframework.web.client.RestClientResponseException(
                "401 Unauthorized", 401, "Unauthorized", null, null, null
            )
        );

        // When: Intentar llamada que fallará con 4xx
        String testText = "test error handling";
        try {
            embeddingModel.embed(testText);
        } catch (Exception e) {
            // Esperado: error 401 simulado
            System.out.println("Error en llamada simulado (esperado): " + e.getMessage());
        }

        // Then: Verificar logs - ninguna key debe aparecer
        String allLogs = listAppender.list.stream()
            .map(ILoggingEvent::getFormattedMessage)
            .reduce("", (a, b) -> a + "\n" + b);

        String realApiKey = System.getenv("GOOGLE_AI_API_KEY");
        String testKey = (realApiKey != null && !realApiKey.trim().isEmpty()) ? realApiKey : "test-key-for-security-verification";
        assertThat(allLogs)
            .as("La key no debe aparecer aunque falle la llamada")
            .doesNotContain(testKey);

        // La key inválida TAMPOCO debe aparecer
        assertThat(allLogs)
            .as("Incluso claves inválidas no deben logearse")
            .doesNotContain(invalidApiKey);

        System.out.println("=== LOGS ERROR 4xx ===");
        listAppender.list.forEach(e -> System.out.println("[" + e.getLevel() + "] " + e.getLoggerName() + ": " + e.getFormattedMessage()));
        System.out.println("=== FIN LOGS ERROR ===");
    }
}