package com.mathtutor.llm;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.util.Assert;

import java.util.List;
import java.util.ArrayList;

/**
 * Adaptador de Spring AI EmbeddingModel que usa OllamaEmbeddingModel de Spring AI.
 * Permite usar embeddings locales con Ollama (nomic-embed-text, 768 dimensiones)
 * como proveedor real para RAG semántico.
 * Perfil: "ollama" para activar solo cuando Ollama está disponible.
 */
public class SpringAiOllamaEmbeddingModel implements EmbeddingModel {

    private final OllamaEmbeddingModel delegate;

    public SpringAiOllamaEmbeddingModel(OllamaEmbeddingModel delegate) {
        Assert.notNull(delegate, "OllamaEmbeddingModel delegate must not be null");
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