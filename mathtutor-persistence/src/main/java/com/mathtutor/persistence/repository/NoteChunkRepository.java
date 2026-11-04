package com.mathtutor.persistence.repository;

import com.mathtutor.persistence.entity.Module;
import com.mathtutor.persistence.entity.NoteChunk;
import com.mathtutor.persistence.entity.Topic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para entidad NoteChunk.
 * Las consultas de similitud vectorial requieren PostgreSQL con pgvector
 * y se implementan en el servicio correspondiente.
 */
@Repository
public interface NoteChunkRepository extends JpaRepository<NoteChunk, Long> {

    List<NoteChunk> findByModule(Module module);

    List<NoteChunk> findByTopic(Topic topic);

    List<NoteChunk> findByModuleCode(String moduleCode);

    List<NoteChunk> findByTopicCode(String topicCode);

    List<NoteChunk> findByModuleCodeAndTopicCode(String moduleCode, String topicCode);

    Page<NoteChunk> findByModuleCodeAndTopicCode(String moduleCode, String topicCode, Pageable pageable);

    // Las siguientes consultas de similitud vectorial requieren PostgreSQL con pgvector
    // y se implementarán en una implementación personalizada o servicio
    // List<NoteChunk> findSimilarByModuleAndTopic(String moduleCode, String topicCode, float[] embedding, int limit);
    // List<NoteChunk> findSimilarByModule(String moduleCode, float[] embedding, int limit);
    // List<NoteChunk> findSimilarGlobal(float[] embedding, int limit);
}