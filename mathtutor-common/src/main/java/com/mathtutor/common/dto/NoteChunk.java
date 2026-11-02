package com.mathtutor.common.dto;

/**
 * Fragmento de nota indexado para RAG.
 */
public record NoteChunk(
    Long id,
    String content,
    float[] embedding,
    String type,        // DEFINICION, TEOREMA, EJEMPLO, EJERCICIO, OTRO
    String sourceFile,
    String moduleCode,
    String topicCode,
    String version
) {}