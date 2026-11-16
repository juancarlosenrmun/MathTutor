package com.mathtutor.application.retriever;

import com.mathtutor.common.dto.NoteChunk;
import java.util.List;

/** Traducción de fixtures JPA al contrato compartido del módulo LLM. */
public final class NoteChunkTestData {
    private NoteChunkTestData() { }

    public static List<NoteChunk> toDto(List<com.mathtutor.persistence.entity.NoteChunk> chunks) {
        return chunks.stream().map(chunk -> new NoteChunk(chunk.getId(), chunk.getContent(),
                null, chunk.getType(), chunk.getSourceFile(), chunk.getModuleCode(),
                chunk.getTopicCode(), chunk.getVersion())).toList();
    }
}
