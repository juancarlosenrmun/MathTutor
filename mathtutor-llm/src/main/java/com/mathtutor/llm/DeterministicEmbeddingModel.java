package com.mathtutor.llm;

import com.mathtutor.common.port.EmbeddingModel;
import java.util.List;

/**
 * EmbeddingModel determinista para tests.
 * Genera embeddings basados en hash del texto, sin llamadas a LLM reales.
 * Útil para tests de integración con pgvector sin claves API.
 *
 * Nota: Para el spike EN-04, se usa una implementación propia ya que
 * Spring AI no está disponible en Maven Central para Spring Boot 3.2.x.
 * En producción, se migrará a Spring AI EmbeddingModel.
 */
public class DeterministicEmbeddingModel implements EmbeddingModel {

    private static final int DIMENSIONS = 384; // Dimensión estándar para modelos pequeños

    /**
     * Genera un embedding determinista basado en el hash del texto.
     * El mismo texto siempre produce el mismo embedding.
     *
     * @param text Texto a embeber
     * @return Vector de embedding de 384 dimensiones
     */
    public float[] embed(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new float[DIMENSIONS];
        }

        // Usar hash del texto para generar valores deterministas
        int hash = text.hashCode();
        float[] embedding = new float[DIMENSIONS];

        // Generar valores pseudo-aleatorios pero deterministas basados en el hash
        // Usar un generador simple basado en el hash
        long seed = hash & 0xFFFFFFFFL;
        for (int i = 0; i < DIMENSIONS; i++) {
            // Algoritmo simple: xorshift
            seed ^= (seed << 13);
            seed ^= (seed >>> 7);
            seed ^= (seed << 17);
            // Normalizar a [-1, 1]
            embedding[i] = ((seed & 0xFFFFFFFFL) / 4294967296.0f) * 2.0f - 1.0f;
        }

        // Normalizar el vector a longitud 1 (importante para similitud coseno)
        normalize(embedding);

        return embedding;
    }

    /**
     * Genera embeddings para múltiples textos.
     *
     * @param texts Lista de textos
     * @return Lista de embeddings
     */
    public List<float[]> embed(List<String> texts) {
        return texts.stream()
                .map(this::embed)
                .toList();
    }

    /**
     * Normaliza un vector a longitud unitaria.
     */
    private void normalize(float[] vector) {
        double sum = 0;
        for (float v : vector) {
            sum += v * v;
        }
        double norm = Math.sqrt(sum);
        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] = (float) (vector[i] / norm);
            }
        }
    }

    public int dimensions() {
        return DIMENSIONS;
    }
}