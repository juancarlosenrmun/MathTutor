package com.mathtutor.persistence.mapper;

import com.mathtutor.common.dto.NoteChunk;
import com.mathtutor.persistence.entity.Module;
import com.mathtutor.persistence.entity.Topic;
import org.springframework.stereotype.Component;

/**
 * Mapper entre NoteChunk (JPA entity) y NoteChunk (DTO).
 */
@Component
public class NoteChunkMapper {

    /**
     * Convierte entidad JPA a DTO.
     */
    public com.mathtutor.common.dto.NoteChunk toDto(com.mathtutor.persistence.entity.NoteChunk entity) {
        if (entity == null) {
            return null;
        }
        return new com.mathtutor.common.dto.NoteChunk(
            entity.getId(),
            entity.getContent(),
            entity.getEmbeddingAsFloatArray(),
            entity.getType(),
            entity.getSourceFile(),
            entity.getModuleCode(),
            entity.getTopicCode(),
            entity.getVersion()
        );
    }

    /**
     * Convierte DTO a entidad JPA.
     */
    public com.mathtutor.persistence.entity.NoteChunk toEntity(com.mathtutor.common.dto.NoteChunk dto) {
        if (dto == null) {
            return null;
        }
        com.mathtutor.persistence.entity.NoteChunk entity = new com.mathtutor.persistence.entity.NoteChunk();
        entity.setId(dto.id());
        entity.setContent(dto.content());
        entity.setType(dto.type());
        entity.setSourceFile(dto.sourceFile());
        entity.setModuleCode(dto.moduleCode());
        entity.setTopicCode(dto.topicCode());
        entity.setVersion(dto.version());
        if (dto.embedding() != null) {
            entity.setEmbeddingFromFloatArray(dto.embedding());
        }
        // Note: module and topic relations need to be set separately if needed
        return entity;
    }
}