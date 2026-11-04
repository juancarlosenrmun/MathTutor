package com.mathtutor.common.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de caja blanca para {@link NoteChunk}.
 * Cobertura: campos, equals/hashCode, toString, array embedding.
 */
class NoteChunkTest {

    @Test
    void shouldCreateNoteChunkWithAllFields() {
        float[] embedding = new float[]{0.1f, 0.2f, 0.3f};
        NoteChunk chunk = new NoteChunk(
            1L,
            "Contenido de la nota",
            embedding,
            "DEFINICION",
            "apuntes/calculo.md",
            "MOD1",
            "TEMA1",
            "v1.0"
        );

        assertThat(chunk.id()).isEqualTo(1L);
        assertThat(chunk.content()).isEqualTo("Contenido de la nota");
        assertThat(chunk.embedding()).isEqualTo(embedding);
        assertThat(chunk.type()).isEqualTo("DEFINICION");
        assertThat(chunk.sourceFile()).isEqualTo("apuntes/calculo.md");
        assertThat(chunk.moduleCode()).isEqualTo("MOD1");
        assertThat(chunk.topicCode()).isEqualTo("TEMA1");
        assertThat(chunk.version()).isEqualTo("v1.0");
    }

    @Test
    void shouldHandleNullEmbedding() {
        NoteChunk chunk = new NoteChunk(
            1L,
            "Contenido",
            null,
            "EJEMPLO",
            "file.md",
            "MOD1",
            "TEMA1",
            "v1.0"
        );

        assertThat(chunk.embedding()).isNull();
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        float[] emb = new float[]{0.1f, 0.2f};
        NoteChunk c1 = new NoteChunk(1L, "cont", emb, "DEF", "f.md", "M1", "T1", "v1");
        NoteChunk c2 = new NoteChunk(1L, "cont", emb, "DEF", "f.md", "M1", "T1", "v1");
        NoteChunk c3 = new NoteChunk(2L, "cont", emb, "DEF", "f.md", "M1", "T1", "v1");

        assertThat(c1).isEqualTo(c2);
        assertThat(c1.hashCode()).isEqualTo(c2.hashCode());
        assertThat(c1).isNotEqualTo(c3);
    }
}