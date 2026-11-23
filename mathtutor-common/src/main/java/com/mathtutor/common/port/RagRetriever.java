package com.mathtutor.common.port;

import com.mathtutor.common.dto.NoteChunk;

import java.util.List;

/**
 * Puerto para la recuperación de conocimiento (RAG).
 * Busca chunks de apuntes relevantes por módulo y tema.
 */
public interface RagRetriever {

    /**
     * Recupera chunks de notas relevantes para una consulta.
     *
     * @param query  consulta del estudiante
     * @param module código del módulo (opcional)
     * @param topic  código del tema (opcional)
     * @return lista de chunks relevantes ordenados por relevancia
     */
    List<NoteChunk> retrieve(String query, String module, String topic);

    /** @brief Recupera apuntes sin embeddings cuando el índice remoto no responde. */
    default List<NoteChunk> retrieveLocal(String query, String module, String topic) {
        return List.of();
    }

    /**
     * Índice los apuntes al arranque.
     */
    void indexAtStartup();
}
