package com.mathtutor.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Verifica los constructores reales; no duplica la validación en el test. */
class SpringAiGoogleAiStudioFailFastTest {
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void chatRejectsMissingApiKey(String key) {
        assertThatThrownBy(() -> new SpringAiGoogleAiStudioChatModel(key, "model", 0.7, 100, 0, 5, new ObjectMapper()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("API key");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void embeddingRejectsMissingApiKey(String key) {
        assertThatThrownBy(() -> new SpringAiGoogleAiStudioEmbeddingModel(key, "model", 768, 0, 5))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("API key");
    }

    @Test
    void invalidDimensionAndQuotaFailBeforeRequests() {
        assertThatThrownBy(() -> new SpringAiGoogleAiStudioEmbeddingModel("key", "model", 0, 0, 5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SpringAiGoogleAiStudioEmbeddingModel("key", "model", 3073, 0, 5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SpringAiGoogleAiStudioEmbeddingModel("key", "model", 768, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SpringAiGoogleAiStudioChatModel("key", "model", Double.NaN, 100, 0, 5, new ObjectMapper()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}