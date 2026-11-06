package com.mathtutor.llm;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingOptionsBuilder;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.document.Document;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Adaptador de Spring AI EmbeddingModel que usa DeterministicEmbeddingModel interno.
 * Permite usar la API de Spring AI (VectorStore, etc.) con embeddings deterministas para tests.
 * En producción se reemplazaría por OpenAiEmbeddingModel, OllamaEmbeddingModel, etc.
 */
public class SpringAiDeterministicEmbeddingModel implements EmbeddingModel {

    private final DeterministicEmbeddingModel delegate;

    public SpringAiDeterministicEmbeddingModel(DeterministicEmbeddingModel delegate) {
        Assert.notNull(delegate, "DeterministicEmbeddingModel must not be null");
        this.delegate = delegate;
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
        return delegate.embed(text);
    }

    @Override
    public int dimensions() {
        return delegate.dimensions();
    }
}