package com.mathtutor.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad JPA para fragmento de nota (RAG) con pgvector.
 * Usa byte[] para compatibilidad con H2 y PostgreSQL.
 */
@Entity
@Table(name = "note_chunks", indexes = {
    @Index(name = "idx_note_chunk_module_topic", columnList = "module_code, topic_code"),
    @Index(name = "idx_note_chunk_embedding", columnList = "embedding")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoteChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "embedding", columnDefinition = "BLOB")
    private byte[] embedding;

    @Column(name = "chunk_type", length = 30)
    private String type; // DEFINICION, TEOREMA, EJEMPLO, EJERCICIO, OTRO

    @Column(name = "source_file", length = 500)
    private String sourceFile;

    @Column(name = "module_code", length = 50)
    private String moduleCode;

    @Column(name = "topic_code", length = 50)
    private String topicCode;

    @Column(length = 20)
    private String version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id")
    private Module module;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    /**
     * Convierte el array de bytes a array de floats para pgvector.
     */
    public float[] getEmbeddingAsFloatArray() {
        if (embedding == null || embedding.length == 0) {
            return new float[0];
        }
        // Assuming 4 bytes per float
        int floatCount = embedding.length / 4;
        float[] result = new float[floatCount];
        for (int i = 0; i < floatCount; i++) {
            result[i] = Float.intBitsToFloat(
                ((embedding[i * 4] & 0xFF) << 24) |
                ((embedding[i * 4 + 1] & 0xFF) << 16) |
                ((embedding[i * 4 + 2] & 0xFF) << 8) |
                (embedding[i * 4 + 3] & 0xFF)
            );
        }
        return result;
    }

    /**
     * Establece el embedding desde un array de floats.
     */
    public void setEmbeddingFromFloatArray(float[] floats) {
        if (floats == null) {
            this.embedding = new byte[0];
            return;
        }
        this.embedding = new byte[floats.length * 4];
        for (int i = 0; i < floats.length; i++) {
            int bits = Float.floatToIntBits(floats[i]);
            this.embedding[i * 4] = (byte) (bits >> 24);
            this.embedding[i * 4 + 1] = (byte) (bits >> 16);
            this.embedding[i * 4 + 2] = (byte) (bits >> 8);
            this.embedding[i * 4 + 3] = (byte) bits;
        }
    }
}