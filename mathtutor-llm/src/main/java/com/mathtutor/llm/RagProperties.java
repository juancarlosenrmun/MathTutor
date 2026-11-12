package com.mathtutor.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Propiedades de configuración para RAG (Retrieval-Augmented Generation).
 * Validadas al arranque: falla rápido si la propiedad no está configurada o está fuera de rango.
 *
 * Propiedades:
 * - tutor.rag.similarity-threshold: Umbral de similitud coseno (0.0 a 1.0).
 *   Mayor = más estricto (solo resultados muy similares).
 *   Valor recomendado: 0.7 (equivale a distancia coseno < 0.3).
 */
@Configuration
@ConfigurationProperties(prefix = "tutor.rag")
@Validated
public class RagProperties {

    /**
     * Umbral de similitud coseno para búsqueda vectorial.
     * Rango: 0.0 (aceptar todo) a 1.0 (solo idénticos).
     * Recomendado: 0.7 (distancia coseno < 0.3).
     * OBLIGATORIO: debe estar configurado en application.yml o variable de entorno.
     */
    @NotNull(message = "tutor.rag.similarity-threshold es obligatorio. Configúrelo en application.yml o variable de entorno TUTOR_RAG_SIMILARITY_THRESHOLD")
    @DecimalMin(value = "0.0", inclusive = false, message = "tutor.rag.similarity-threshold debe ser > 0.0")
    @DecimalMax(value = "1.0", inclusive = true, message = "tutor.rag.similarity-threshold debe ser ≤ 1.0")
    private Double similarityThreshold;

    public Double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public void setSimilarityThreshold(Double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }

    /**
     * Convierte umbral de similitud a distancia coseno (1 - similitud).
     * pgvector usa distancia coseno: 0 = idénticos, 2 = opuestos.
     * Para similitud S, distancia = 1 - S.
     * Ejemplo: similitud 0.7 → distancia 0.3.
     */
    public double toCosineDistance() {
        if (similarityThreshold == null) {
            throw new IllegalStateException("tutor.rag.similarity-threshold no configurado");
        }
        return 1.0 - similarityThreshold;
    }
}