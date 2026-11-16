package com.mathtutor.llm;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmbeddingAdaptersTest {
    @Test
    void deterministicAdapterDelegatesAllOperations() {
        var delegate = new DeterministicEmbeddingModel();
        var adapter = new SpringAiDeterministicEmbeddingModel(delegate);
        assertThat(adapter.embed(new Document("texto"))).containsExactly(delegate.embed("texto"));
        var result = adapter.call(new EmbeddingRequest(List.of("uno", "dos"), null));
        assertThat(result.getResults()).hasSize(2);
        assertThat(result.getResults().get(1).getIndex()).isEqualTo(1);
        assertThat(adapter.dimensions()).isEqualTo(delegate.dimensions());
        assertThatThrownBy(() -> new SpringAiDeterministicEmbeddingModel(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ollamaAdapterDelegatesAllOperationsWithoutNetwork() {
        var delegate = mock(OllamaEmbeddingModel.class);
        when(delegate.embed(anyString())).thenReturn(new float[]{1, 0});
        when(delegate.dimensions()).thenReturn(2);
        var adapter = new SpringAiOllamaEmbeddingModel(delegate);
        assertThat(adapter.embed(new Document("texto"))).containsExactly(1, 0);
        assertThat(adapter.call(new EmbeddingRequest(List.of("uno", "dos"), null)).getResults()).hasSize(2);
        assertThat(adapter.dimensions()).isEqualTo(2);
        assertThatThrownBy(() -> new SpringAiOllamaEmbeddingModel(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ragRequiresThresholdAndConvertsIt() {
        var properties = new RagProperties();
        assertThatThrownBy(properties::toCosineDistance).isInstanceOf(IllegalStateException.class);
        properties.setSimilarityThreshold(0.7);
        assertThat(properties.toCosineDistance()).isCloseTo(0.3, within(0.000001));
    }
}
