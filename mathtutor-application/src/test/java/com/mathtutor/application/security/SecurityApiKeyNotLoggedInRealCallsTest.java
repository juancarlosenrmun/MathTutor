package com.mathtutor.application.security;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Test de seguridad que captura logs REALES durante una llamada HTTP REAL a Google AI Studio
 * y verifica que NO aparece el valor de GOOGLE_AI_API_KEY.
 *
 * Usa Logback ListAppender para interceptar logs en tiempo real.
 * Tag: "security" para ejecución selectiva.
 *
 * DOS MODOS DE EJECUCIÓN:
 * 1. Con GOOGLE_AI_API_KEY real (desde .env): ./gradlew :mathtutor-application:realApiTest
 *    - Ejecuta realApiCall_doesNotLogApiKey con key REAL
 *    - Verifica que la key real no aparece en logs
 * 2. Sin GOOGLE_AI_API_KEY (fake keys): ./gradlew test --tests SecurityApiKeyNotLoggedInRealCallsTest
 *    - Usa FAKE keys inyectadas via @DynamicPropertySource
 *    - Verifica el detector de keys FUNCIONA (testDetectorCatchesLoggedKey)
 *    - Verifica que fake keys tampoco aparecen en logs
 */
@SpringBootTest(classes = com.mathtutor.application.MathTutorApplication.class)
@ActiveProfiles({"google-ai-studio"})
@Tag("security")
class SecurityApiKeyNotLoggedInRealCallsTest {

    // Fake API key para tests sin key real - formato Google AI Studio (AIza...)
    private static final String FAKE_API_KEY = "AIzaSyFAKE_KEY_FOR_TESTING_ONLY_12345678901234";

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String realApiKey = System.getenv("GOOGLE_AI_API_KEY");
        boolean hasRealKey = realApiKey != null && !realApiKey.trim().isEmpty();

        // Use H2 in-memory database for isolated tests (avoids Testcontainers/Docker issues on Windows)
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:mathtutor_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.H2Dialect");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.profiles.active", () -> "google-ai-studio");
        registry.add("spring.ai.vectorstore.pgvector.initialize-schema", () -> "false");
        registry.add("spring.ai.vectorstore.type", () -> "none");

        // Google AI Studio properties - usar key real si existe, si no fake key
        String apiKeyToUse = hasRealKey ? realApiKey : FAKE_API_KEY;
        registry.add("spring.ai.google.ai-studio.api-key", () -> apiKeyToUse);
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

    @Autowired
    private EmbeddingModel embeddingModel;

    private ListAppender<ILoggingEvent> listAppender;
    private Logger rootLogger;

    @BeforeEach
    void setUp() {
        // Configurar ListAppender para capturar TODOS los logs
        listAppender = new ListAppender<>();
        listAppender.start();

        rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        rootLogger.detachAppender(listAppender);
        listAppender.stop();
    }

    /**
     * Test 1: Llamada REAL con GOOGLE_AI_API_KEY real (desde .env)
     * Se ejecuta SOLO cuando hay key real en el entorno.
     * Tag: "real-api" para ejecución selectiva via :mathtutor-application:realApiTest
     */
    @Test
    @Tag("real-api")
    void realApiCall_doesNotLogApiKey() {
        // Given: GOOGLE_AI_API_KEY real en entorno
        String realApiKey = System.getenv("GOOGLE_AI_API_KEY");
        assumeThat(realApiKey).as("GOOGLE_AI_API_KEY must be set for this test (run with :mathtutor-application:realApiTest)").isNotNull().isNotEmpty();

        // When: Llamada REAL a embedding (genera logs de HTTP reales)
        String testText = "test embedding for security verification";
        float[] embedding = embeddingModel.embed(testText);

        // Then: Verificar que el embedding se generó
        assertThat(embedding).isNotNull();
        assertThat(embedding.length).isEqualTo(768);

        // And: Verificar que NO aparece la API key real en NINGÚN log
        String allLogs = listAppender.list.stream()
            .map(ILoggingEvent::getFormattedMessage)
            .reduce("", (a, b) -> a + "\n" + b);

        // Buscar la key real exacta
        assertThat(allLogs)
            .as("El valor de GOOGLE_AI_API_KEY real no debe aparecer en logs")
            .doesNotContain(realApiKey);

        // También buscar patrones comunes de keys de Google (AIza... y AQ...)
        assertThat(allLogs)
            .as("No debe aparecer patrón AIza... en logs")
            .doesNotMatch("(?s).*AIza[0-9A-Za-z_-]{35}.*");

        assertThat(allLogs)
            .as("No debe aparecer patrón AQ.... en logs")
            .doesNotMatch("(?s).*AQ\\.[0-9A-Za-z_-]+.*");

        // Verificar que la llamada HTTP se realizó (logs de DEBUG/TRACE de HTTP)
        // Pero sin credenciales
        System.out.println("=== LOGS CAPTURADOS (real key test) ===");
        listAppender.list.forEach(e -> System.out.println("[" + e.getLevel() + "] " + e.getLoggerName() + ": " + e.getFormattedMessage()));
        System.out.println("=== FIN LOGS ===");
    }

    /**
     * Test 2: Llamada que genera error 4xx (simulado con fake key)
     * Documenta el código de estado HTTP recibido.
     * Se ejecuta SIN key real (usa FAKE_API_KEY inyectada).
     */
    @Test
    @Tag("external-api")
    void realApiCall_with4xxError_doesNotLogApiKey() {
        // Given: Usamos la FAKE_API_KEY inyectada via @DynamicPropertySource
        // Esta key NO es válida y generará un error 401/403 de Google AI Studio
        String fakeKey = FAKE_API_KEY;

        // When: Intentar llamada que fallará con 4xx (401 Unauthorized o 403 Forbidden)
        String testText = "test error handling with invalid key";
        int httpStatusCaptured = -1;

        try {
            embeddingModel.embed(testText);
        } catch (Exception e) {
            // Esperado: falla por key inválida (401/403), rate limit, red, etc.
            String errorMsg = e.getMessage();
            System.out.println("Error en llamada (esperado con fake key): " + errorMsg);

            // Intentar extraer código de estado HTTP del mensaje de error
            // RestClientResponseException tiene getRawStatusCode()
            if (e instanceof org.springframework.web.client.RestClientResponseException rcre) {
                httpStatusCaptured = rcre.getRawStatusCode();
                System.out.println("Código de estado HTTP capturado: " + httpStatusCaptured);
            } else if (e.getCause() instanceof org.springframework.web.client.RestClientResponseException rcre2) {
                httpStatusCaptured = rcre2.getRawStatusCode();
                System.out.println("Código de estado HTTP capturado (cause): " + httpStatusCaptured);
            }
        }

        // Then: Verificar logs - la fake key NO debe aparecer
        String allLogs = listAppender.list.stream()
            .map(ILoggingEvent::getFormattedMessage)
            .reduce("", (a, b) -> a + "\n" + b);

        // Verificar que la fake key no aparece en logs
        assertThat(allLogs)
            .as("La fake key no debe aparecer en logs aunque falle la llamada")
            .doesNotContain(fakeKey);

        // Tampoco debe aparecer patrón AIza... (la fake key empieza con AIza)
        assertThat(allLogs)
            .as("No debe aparecer patrón AIza... en logs")
            .doesNotMatch("(?s).*AIza[0-9A-Za-z_-]{35}.*");

        // Documentar el código de estado HTTP
        System.out.println("=== LOGS ERROR 4xx (fake key test) ===");
        System.out.println("Código de estado HTTP documentado: " + (httpStatusCaptured == -1 ? "No capturado (excepción genérica)" : httpStatusCaptured));
        listAppender.list.forEach(e -> System.out.println("[" + e.getLevel() + "] " + e.getLoggerName() + ": " + e.getFormattedMessage()));
        System.out.println("=== FIN LOGS ERROR ===");
    }

    /**
     * Test 3: VERIFICAR QUE EL DETECTOR FUNCIONA
     * Este test DELIBERADAMENTE loguea la fake key y verifica que el detector la atrapa.
     * DEMUESTRA que los asserts de los tests 1 y 2 SÍ detectarían una fuga.
     * Se ejecuta SIN key real (usa FAKE_API_KEY inyectada).
     * Tag: "detector-demo" - excluido del test run regular, solo ejecución manual.
     */
    @Test
    @Tag("detector-demo")
    void testDetectorCatchesLoggedKey() {
        // Given: FAKE_API_KEY inyectada via @DynamicPropertySource
        String fakeKey = FAKE_API_KEY;

        // When: DELIBERADAMENTE logueamos la fake key (simulando una fuga)
        // Esto es SOLO para probar que el detector funciona
        rootLogger.info("DEBUG - API Key (SIMULATED LEAK): {}", fakeKey);

        // Then: El detector (los asserts) DEBE fallar
        String allLogs = listAppender.list.stream()
            .map(ILoggingEvent::getFormattedMessage)
            .reduce("", (a, b) -> a + "\n" + b);

        // ESTE ASSERT DEBE FALLAR porque logueamos la key deliberadamente
        // Esto demuestra que los tests 1 y 2 SÍ detectarían una fuga real
        assertThat(allLogs)
            .as("DETECTOR TEST: Este test DEBE FALLAR porque logueamos la key deliberadamente. "
                + "Si pasa, el detector no funciona. Si falla, el detector FUNCIONA correctamente.")
            .doesNotContain(fakeKey);
    }

    /**
     * Test 4: Verificar que fake key tampoco aparece en logs normales
     * (sin logueo deliberado). Se ejecuta SIN key real.
     */
    @Test
    @Tag("external-api")
    void fakeKeyNotLoggedInNormalOperation() {
        // Given: FAKE_API_KEY inyectada via @DynamicPropertySource
        String fakeKey = FAKE_API_KEY;

        // When: Llamada normal (fallará por key inválida, pero no logueamos deliberadamente)
        String testText = "test normal operation with fake key";
        try {
            embeddingModel.embed(testText);
        } catch (Exception e) {
            // Esperado: falla por key inválida
            System.out.println("Error esperado en operación normal: " + e.getMessage());
        }

        // Then: Verificar que la fake key no aparece en logs de operación normal
        String allLogs = listAppender.list.stream()
            .map(ILoggingEvent::getFormattedMessage)
            .reduce("", (a, b) -> a + "\n" + b);

        assertThat(allLogs)
            .as("La fake key no debe aparecer en logs de operación normal")
            .doesNotContain(fakeKey);

        assertThat(allLogs)
            .as("No debe aparecer patrón AIza... en logs de operación normal")
            .doesNotMatch("(?s).*AIza[0-9A-Za-z_-]{35}.*");

        System.out.println("=== LOGS OPERACIÓN NORMAL (fake key) ===");
        listAppender.list.forEach(e -> System.out.println("[" + e.getLevel() + "] " + e.getLoggerName() + ": " + e.getFormattedMessage()));
        System.out.println("=== FIN LOGS ===");
    }
}
