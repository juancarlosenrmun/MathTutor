package com.mathtutor.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mathtutor.common.dto.NoteChunk;
import com.mathtutor.common.port.NoteIndex;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** @brief Índice idempotente: embeddings sin JDBC y sustitución atómica en pgvector. */
@Component @Profile("notes-indexing") @DependsOn("vectorStore")
public class PgVectorNoteIndex implements NoteIndex {
    private final JdbcTemplate jdbc;
    private final EmbeddingModel embedding;
    private final TransactionTemplate transaction;
    private final ObjectMapper json;

    public PgVectorNoteIndex(JdbcTemplate jdbc, EmbeddingModel embedding,
                             PlatformTransactionManager manager, ObjectMapper json) {
        this.jdbc = jdbc; this.embedding = embedding; this.transaction = new TransactionTemplate(manager); this.json = json;
    }

    @Override @Transactional(propagation = Propagation.NEVER)
    public void synchronize(List<NoteChunk> chunks) {
        var ids = new ArrayList<UUID>();
        for (int i = 0; i < chunks.size(); i++) {
            var chunk = chunks.get(i);
            ids.add(UUID.nameUUIDFromBytes((chunk.sourceFile() + ":" + i + ":" + chunk.version())
                    .getBytes(StandardCharsets.UTF_8)));
        }
        var existing = jdbc.queryForList("SELECT id::text FROM vector_store WHERE metadata->>'managed_by' = 'UC-14'", String.class);
        if (new TreeSet<>(existing).equals(new TreeSet<>(ids.stream().map(UUID::toString).toList()))) return;
        // Ninguna transacción ni conexión sigue abierta mientras se llama al modelo.
        // El overload Document selecciona RETRIEVAL_DOCUMENT en proveedores asimétricos.
        var vectors = chunks.stream().map(chunk -> embedding.embed(new org.springframework.ai.document.Document(
                chunk.content(), Map.of("module_code", chunk.moduleCode(), "topic_code", chunk.topicCode())))).toList();
        var metadata = chunks.stream().map(this::metadata).toList();
        transaction.executeWithoutResult(status -> {
            jdbc.update("DELETE FROM vector_store WHERE metadata->>'managed_by' = 'UC-14'");
            for (int i = 0; i < chunks.size(); i++) {
                jdbc.update("INSERT INTO vector_store(id,content,metadata,embedding) VALUES (?, ?, ?::jsonb, ?::vector)",
                        ids.get(i), chunks.get(i).content(), metadata.get(i), Arrays.toString(vectors.get(i)));
            }
        });
    }

    private String metadata(NoteChunk chunk) {
        try {
            return json.writeValueAsString(Map.of("managed_by", "UC-14", "module_code", chunk.moduleCode(),
                    "topic_code", chunk.topicCode(), "type", chunk.type(), "source_file", chunk.sourceFile(), "version", chunk.version()));
        } catch (JsonProcessingException failure) {
            throw new IllegalArgumentException("Metadatos de apuntes no serializables", failure);
        }
    }
}
