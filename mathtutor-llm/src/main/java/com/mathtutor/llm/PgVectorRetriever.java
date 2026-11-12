package com.mathtutor.llm;

import com.mathtutor.common.dto.NoteChunk;
import com.mathtutor.common.port.NoteChunkStore;
import com.mathtutor.common.port.RagRetriever;
import com.pgvector.PGvector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de RagRetriever usando pgvector directo (JDBC).
 * Indexa y recupera fragmentos de notas con filtro por módulo/tema y umbral de similitud.
 *
 * Perfil: "pgvector" para activar solo cuando hay BD PostgreSQL con pgvector.
 * Para tests, usar DeterministicEmbeddingModel y Testcontainers.
 *
 * Nota: Para el spike EN-04, se usa pgvector JDBC directo ya que
 * Spring AI no está disponible en Maven Central para Spring Boot 3.2.x.
 * En producción, se migrará a Spring AI VectorStore.
 */
@Component
@Profile("pgvector")
public class PgVectorRetriever implements RagRetriever {

    private final NoteChunkStore noteChunkStore;
    private final DeterministicEmbeddingModel embeddingModel;
    private final JdbcTemplate jdbcTemplate;
    private final RagProperties ragProperties;

    @Autowired
    public PgVectorRetriever(NoteChunkStore noteChunkStore,
                              DeterministicEmbeddingModel embeddingModel,
                              JdbcTemplate jdbcTemplate,
                              RagProperties ragProperties) {
        this.noteChunkStore = noteChunkStore;
        this.embeddingModel = embeddingModel;
        this.jdbcTemplate = jdbcTemplate;
        this.ragProperties = ragProperties;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NoteChunk> retrieve(String query, String module, String topic) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        // Generar embedding de la consulta
        float[] queryEmbedding = embeddingModel.embed(query);

        // Construir SQL con filtro por módulo y tema y búsqueda por similitud
        StringBuilder sql = new StringBuilder(
                "SELECT id, content, type, source_file, module_code, topic_code, version, " +
                "embedding <=> ?::vector AS distance " +
                "FROM note_chunks WHERE 1=1"
        );

        List<Object> params = new ArrayList<>();
        params.add(new PGvector(queryEmbedding));

        if (module != null && !module.trim().isEmpty()) {
            sql.append(" AND module_code = ?");
            params.add(module);
        }

        if (topic != null && !topic.trim().isEmpty()) {
            sql.append(" AND topic_code = ?");
            params.add(topic);
        }

        // Umbral de similitud desde configuración (validada al arrancar)
        // pgvector usa distancia coseno: 1 - similitud
        double cosineDistance = ragProperties.toCosineDistance();
        sql.append(" AND embedding <=> ?::vector < ?");
        params.add(new PGvector(queryEmbedding));
        params.add(cosineDistance);

        sql.append(" ORDER BY distance ASC LIMIT 10");

        return jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) -> mapRowToNoteChunk(rs));
    }

    @Override
    @Transactional
    public void indexAtStartup() {
        // Obtener todos los chunks de la BD que no tienen embedding
        List<NoteChunk> chunks = noteChunkStore.findAll();

        if (chunks.isEmpty()) {
            return;
        }

        // Generar embeddings y actualizar en BD
        for (NoteChunk chunk : chunks) {
            if (chunk.embedding() == null || chunk.embedding().length == 0) {
                float[] embedding = embeddingModel.embed(chunk.content());
                NoteChunk updated = new NoteChunk(
                    chunk.id(),
                    chunk.content(),
                    embedding,
                    chunk.type(),
                    chunk.sourceFile(),
                    chunk.moduleCode(),
                    chunk.topicCode(),
                    chunk.version()
                );
                noteChunkStore.save(updated);
            }
        }
    }

    private NoteChunk mapRowToNoteChunk(ResultSet rs) throws java.sql.SQLException {
        return new NoteChunk(
                rs.getLong("id"),
                rs.getString("content"),
                null, // embedding no se devuelve en búsqueda
                rs.getString("type"),
                rs.getString("source_file"),
                rs.getString("module_code"),
                rs.getString("topic_code"),
                rs.getString("version")
        );
    }
}