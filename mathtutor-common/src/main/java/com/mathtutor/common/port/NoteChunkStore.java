package com.mathtutor.common.port;

import com.mathtutor.common.dto.NoteChunk;

import java.util.List;

/**
 * Puerto para almacenamiento y recuperación de fragmentos de notas (RAG).
 * Implementado por mathtutor-persistence, consumido por mathtutor-llm.
 */
public interface NoteChunkStore {

    /**
     * Guarda un fragmento de nota.
     */
    NoteChunk save(NoteChunk chunk);

    /**
     * Obtiene todos los fragmentos.
     */
    List<NoteChunk> findAll();

    /**
     * Busca fragmentos por código de módulo.
     */
    List<NoteChunk> findByModuleCode(String moduleCode);

    /**
     * Busca fragmentos por código de tema.
     */
    List<NoteChunk> findByTopicCode(String topicCode);

    /**
     * Busca fragmentos por código de módulo y tema.
     */
    List<NoteChunk> findByModuleCodeAndTopicCode(String moduleCode, String topicCode);

    /**
     * Busca fragmentos por código de módulo y tema con paginación.
     * @param offset número de elementos a saltar
     * @param limit número máximo de elementos a devolver
     */
    List<NoteChunk> findByModuleCodeAndTopicCode(String moduleCode, String topicCode, int offset, int limit);

    // Métodos de búsqueda vectorial - requieren PostgreSQL con pgvector
    // List<NoteChunk> findSimilarByModuleAndTopic(String moduleCode, String topicCode, float[] embedding, int limit);
    // List<NoteChunk> findSimilarByModule(String moduleCode, float[] embedding, int limit);
    // List<NoteChunk> findSimilarGlobal(float[] embedding, int limit);
}