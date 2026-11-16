package com.mathtutor.llm;

import java.util.List;

/**
 * EmbeddingModel determinista LENTO para tests de RNF-03.
 * Simula la latencia de una llamada real a LLM (Google AI Studio, OpenAI, etc.)
 * añadiendo un retraso configurable antes de generar el embedding.
 *
 * Usado exclusivamente en tests con perfil "deterministic" para validar
 * que no hay transacciones JDBC abiertas durante la generación de embeddings.
 */
public class SlowDeterministicEmbeddingModel extends DeterministicEmbeddingModel {

    // Retraso en milisegundos para simular latencia de red + procesamiento LLM
    // 2000ms = 2 segundos, realista para llamadas a API externas
    private static final int SIMULATED_LLM_LATENCY_MS = 2000;

    @Override
    public float[] embed(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new float[dimensions()];
        }

        // SIMULAR LATENCIA DE LLM REAL
        try {
            Thread.sleep(SIMULATED_LLM_LATENCY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Embedding generation interrupted", e);
        }

        // Delegar al modelo determinista estándar (padre)
        return super.embed(text);
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        // Para batch, aplicar retraso una sola vez (simula una llamada batch)
        try {
            Thread.sleep(SIMULATED_LLM_LATENCY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Batch embedding generation interrupted", e);
        }

        return texts.stream()
                .map(this::embed)
                .toList();
    }
}