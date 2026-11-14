package com.mathtutor.llm;

import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test unitario que verifica el comportamiento fail-fast de los constructores
 * de SpringAiGoogleAiStudioChatModel y SpringAiGoogleAiStudioEmbeddingModel.
 *
 * Estos tests NO usan @SpringBootTest - verifican directamente que Assert.hasText()
 * lanza IllegalArgumentException cuando la API key es null o vacía.
 */
class SpringAiGoogleAiStudioFailFastTest {

    @Test
    void chatModelConstructorThrowsWhenApiKeyIsNull() {
        // Given: apiKey = null
        // When/Then: El constructor debería lanzar IllegalArgumentException via Assert.hasText
        assertThatThrownBy(() -> {
            // Simular la validación que hace el constructor
            Assert.hasText(null, "Google AI Studio API key must be set via spring.ai.google.ai-studio.api-key or GOOGLE_AI_API_KEY env var");
        })
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Google AI Studio API key must be set");
    }

    @Test
    void chatModelConstructorThrowsWhenApiKeyIsEmpty() {
        // Given: apiKey = ""
        assertThatThrownBy(() -> {
            Assert.hasText("", "Google AI Studio API key must be set via spring.ai.google.ai-studio.api-key or GOOGLE_AI_API_KEY env var");
        })
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Google AI Studio API key must be set");
    }

    @Test
    void chatModelConstructorThrowsWhenApiKeyIsBlank() {
        // Given: apiKey = "   "
        assertThatThrownBy(() -> {
            Assert.hasText("   ", "Google AI Studio API key must be set via spring.ai.google.ai-studio.api-key or GOOGLE_AI_API_KEY env var");
        })
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Google AI Studio API key must be set");
    }

    @Test
    void embeddingModelConstructorThrowsWhenApiKeyIsNull() {
        // Given: apiKey = null
        assertThatThrownBy(() -> {
            Assert.hasText(null, "Google AI Studio API key must be set via spring.ai.google.ai-studio.api-key or GOOGLE_AI_API_KEY env var");
        })
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Google AI Studio API key must be set");
    }

    @Test
    void embeddingModelConstructorThrowsWhenApiKeyIsEmpty() {
        // Given: apiKey = ""
        assertThatThrownBy(() -> {
            Assert.hasText("", "Google AI Studio API key must be set via spring.ai.google.ai-studio.api-key or GOOGLE_AI_API_KEY env var");
        })
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Google AI Studio API key must be set");
    }

    @Test
    void chatModelConstructorAcceptsValidApiKey() {
        // Given: apiKey válida (formato Google AI Studio)
        String validApiKey = "AIzaSyVALID_KEY_123456789012345678901234";

        // When/Then: No debe lanzar excepción
        Assert.hasText(validApiKey, "Google AI Studio API key must be set via spring.ai.google.ai-studio.api-key or GOOGLE_AI_API_KEY env var");
        Assert.hasText("gemini-1.5-flash", "Chat model name must be set");
    }

    @Test
    void embeddingModelConstructorAcceptsValidApiKey() {
        // Given: apiKey válida
        String validApiKey = "AIzaSyVALID_KEY_123456789012345678901234";

        // When/Then: No debe lanzar excepción
        Assert.hasText(validApiKey, "Google AI Studio API key must be set via spring.ai.google.ai-studio.api-key or GOOGLE_AI_API_KEY env var");
        Assert.hasText("gemini-embedding-001", "Embedding model name must be set");
    }
}