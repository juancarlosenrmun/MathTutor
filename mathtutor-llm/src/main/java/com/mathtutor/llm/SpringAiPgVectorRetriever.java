package com.mathtutor.llm;

import com.mathtutor.common.dto.NoteChunk;
import com.mathtutor.common.port.RagRetriever;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementación de RagRetriever usando Spring AI VectorStore (PgVectorStore).
 * Usa la abstracción VectorStore de Spring AI para indexación y recuperación.
 *
 * Perfil: "spring-ai-pgvector" para activar solo cuando hay BD PostgreSQL con pgvector y Spring AI.
 * Para tests, usa SpringAiDeterministicEmbeddingModel y Testcontainers.
 *
 * Esta es la implementación que valida el requisito "Spring AI con pgvector" del spike EN-04.
 */
@Component
@Profile("spring-ai-pgvector")
public class SpringAiPgVectorRetriever implements RagRetriever {

    private final VectorStore vectorStore;
    private final FilterExpressionBuilder filterBuilder;

    @Autowired
    public SpringAiPgVectorRetriever(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        this.filterBuilder = new FilterExpressionBuilder();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NoteChunk> retrieve(String query, String module, String topic) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        // Construir filtros por módulo y tema usando Spring AI Filter DSL
        Filter.Expression filter = null;
        FilterExpressionBuilder.Op combinedOp = null;

        if (module != null && !module.trim().isEmpty()) {
            combinedOp = filterBuilder.eq("module_code", module);
        }

        if (topic != null && !topic.trim().isEmpty()) {
            FilterExpressionBuilder.Op topicOp = filterBuilder.eq("topic_code", topic);
            combinedOp = (combinedOp == null) ? topicOp : filterBuilder.and(combinedOp, topicOp);
        }

        if (combinedOp != null) {
            filter = combinedOp.build();
        }

        // Búsqueda por similitud con filtro opcional usando SearchRequest
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(10)
                .filterExpression(filter)
                .build();

        List<Document> results = vectorStore.similaritySearch(searchRequest);

        // Convertir Documents de Spring AI a NoteChunk del dominio
        return results.stream()
                .map(this::documentToNoteChunk)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void indexAtStartup() {
        // Spring AI PgVectorStore maneja la indexación automáticamente
        // al agregar documentos. Este método se mantiene por compatibilidad
        // con la interfaz RagRetriever, pero la indexación real ocurre
        // vía VectorStore.add(documents).
    }

    /**
     * Índice manual de documentos (para tests que insertan datos directamente en BD).
     * Convierte NoteChunk del dominio a Document de Spring AI y los añade al VectorStore.
     */
    public void indexDocuments(List<com.mathtutor.persistence.entity.NoteChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }

        List<Document> documents = chunks.stream()
                .map(this::noteChunkToDocument)
                .collect(Collectors.toList());

        vectorStore.add(documents);
    }

    private NoteChunk documentToNoteChunk(Document doc) {
        Map<String, Object> metadata = doc.getMetadata();
        return new NoteChunk(
                // id no está en metadata por defecto, se usa 0L
                0L,
                doc.getText(),
                null, // embedding no se devuelve en búsqueda
                (String) metadata.getOrDefault("type", "UNKNOWN"),
                (String) metadata.getOrDefault("source_file", ""),
                (String) metadata.getOrDefault("module_code", ""),
                (String) metadata.getOrDefault("topic_code", ""),
                (String) metadata.getOrDefault("version", "1")
        );
    }

    private Document noteChunkToDocument(com.mathtutor.persistence.entity.NoteChunk chunk) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("id", chunk.getId() != null ? chunk.getId() : 0L);
        metadata.put("type", chunk.getType());
        metadata.put("source_file", chunk.getSourceFile());
        metadata.put("module_code", chunk.getModuleCode());
        metadata.put("topic_code", chunk.getTopicCode());
        metadata.put("version", chunk.getVersion());

        return Document.builder()
                .text(chunk.getContent())
                .metadata(metadata)
                .build();
    }
}