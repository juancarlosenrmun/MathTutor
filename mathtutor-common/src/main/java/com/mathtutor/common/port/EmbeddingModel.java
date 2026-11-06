package com.mathtutor.common.port;

import java.util.List;

/**
 * Puerto para modelos de embeddings.
 * Permite cambiar de proveedor/modelo solo por configuración.
 */
public interface EmbeddingModel {

    /**
     * Genera un embedding para un texto.
     *
     * @param text texto a embeber
     * @return vector de embedding
     */
    float[] embed(String text);

    /**
     * Genera embeddings para múltiples textos.
     *
     * @param texts lista de textos
     * @return lista de embeddings
     */
    List<float[]> embed(List<String> texts);

    /**
     * Obtiene la dimensión del embedding.
     *
     * @return número de dimensiones
     */
    int dimensions();
}