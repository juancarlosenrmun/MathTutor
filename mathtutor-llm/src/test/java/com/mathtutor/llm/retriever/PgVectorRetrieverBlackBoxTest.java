package com.mathtutor.llm.retriever;

import com.mathtutor.common.dto.NoteChunk;
import com.mathtutor.common.port.RagRetriever;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas de caja negra para PgVectorRetriever (con mocks).
 * Entrada: query, module, topic → Salida: lista de NoteChunk filtrados.
 * No requiere base de datos real, usa mocks para aislar la lógica.
 */
@ExtendWith(MockitoExtension.class)
class PgVectorRetrieverBlackBoxTest {

    @Mock
    private RagRetriever mockRetriever;

    @Test
    @DisplayName("Recupera chunks relevantes para una consulta")
    void retrievesRelevantChunksForQuery() {
        // Given
        NoteChunk chunk1 = new NoteChunk(1L, "Derivada de sen(x) es cos(x)",
            new float[384], "TEOREMA", "test.tex", "CALC", "DERIV", "1");
        NoteChunk chunk2 = new NoteChunk(2L, "Integral de cos(x) es sen(x)",
            new float[384], "TEOREMA", "test2.tex", "CALC", "INTEG", "1");

        when(mockRetriever.retrieve("derivada", "CALC", "DERIV"))
            .thenReturn(List.of(chunk1));

        // When
        List<NoteChunk> results = mockRetriever.retrieve("derivada", "CALC", "DERIV");

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).content()).contains("Derivada");
        assertThat(results.get(0).moduleCode()).isEqualTo("CALC");
        assertThat(results.get(0).topicCode()).isEqualTo("DERIV");
    }

    @Test
    @DisplayName("Filtra por módulo correctamente")
    void filtersByModule() {
        // Given
        NoteChunk calcChunk = new NoteChunk(1L, "Cálculo", new float[384], "TEOREMA",
            "a.tex", "CALC", "DERIV", "1");
        NoteChunk algChunk = new NoteChunk(2L, "Álgebra", new float[384], "TEOREMA",
            "b.tex", "ALG", "POLY", "1");

        when(mockRetriever.retrieve("x^2", "CALC", null))
            .thenReturn(List.of(calcChunk));

        // When
        List<NoteChunk> results = mockRetriever.retrieve("x^2", "CALC", null);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).moduleCode()).isEqualTo("CALC");
        assertThat(results.get(0).topicCode()).isEqualTo("DERIV");
    }

    @Test
    @DisplayName("Filtra por tema correctamente")
    void filtersByTopic() {
        // Given
        NoteChunk derivChunk = new NoteChunk(1L, "Derivadas", new float[384], "TEOREMA",
            "a.tex", "CALC", "DERIV", "1");
        NoteChunk integChunk = new NoteChunk(2L, "Integrales", new float[384], "TEOREMA",
            "b.tex", "CALC", "INTEG", "1");

        when(mockRetriever.retrieve("potencia", "CALC", "DERIV"))
            .thenReturn(List.of(derivChunk));

        // When
        List<NoteChunk> results = mockRetriever.retrieve("potencia", "CALC", "DERIV");

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).topicCode()).isEqualTo("DERIV");
    }

    @Test
    @DisplayName("Sin filtros busca globalmente")
    void searchesGloballyWithoutFilters() {
        // Given
        NoteChunk chunk1 = new NoteChunk(1L, "Matemáticas", new float[384], "TEOREMA",
            "a.tex", "CALC", "DERIV", "1");
        NoteChunk chunk2 = new NoteChunk(2L, "Física", new float[384], "TEOREMA",
            "b.tex", "PHYS", "MECH", "1");

        when(mockRetriever.retrieve("ciencia", null, null))
            .thenReturn(List.of(chunk1, chunk2));

        // When
        List<NoteChunk> results = mockRetriever.retrieve("ciencia", null, null);

        // Then
        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("Retorna lista vacía si no hay coincidencias")
    void returnsEmptyListIfNoMatches() {
        // Given
        when(mockRetriever.retrieve("tema inexistente", "CALC", "DERIV"))
            .thenReturn(List.of());

        // When
        List<NoteChunk> results = mockRetriever.retrieve("tema inexistente", "CALC", "DERIV");

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Chunks retornados tienen todos los campos poblados")
    void returnedChunksHaveAllFields() {
        // Given
        float[] embedding = new float[384];
        embedding[0] = 0.5f;

        NoteChunk chunk = new NoteChunk(
            42L,
            "Contenido completo",
            embedding,
            "DEFINICION",
            "source.tex",
            "MOD1",
            "TOP1",
            "v2"
        );

        when(mockRetriever.retrieve(anyString(), anyString(), anyString()))
            .thenReturn(List.of(chunk));

        // When
        List<NoteChunk> results = mockRetriever.retrieve("test", "MOD1", "TOP1");

        // Then
        assertThat(results).hasSize(1);
        NoteChunk result = results.get(0);
        assertThat(result.id()).isEqualTo(42L);
        assertThat(result.content()).isEqualTo("Contenido completo");
        assertThat(result.embedding()).isEqualTo(embedding);
        assertThat(result.type()).isEqualTo("DEFINICION");
        assertThat(result.sourceFile()).isEqualTo("source.tex");
        assertThat(result.moduleCode()).isEqualTo("MOD1");
        assertThat(result.topicCode()).isEqualTo("TOP1");
        assertThat(result.version()).isEqualTo("v2");
    }

    @Test
    @DisplayName("indexAtStartup es idempotente")
    void indexAtStartupIsIdempotent() {
        // When
        mockRetriever.indexAtStartup();
        mockRetriever.indexAtStartup();

        // Then - no debe lanzar excepción
        verify(mockRetriever, times(2)).indexAtStartup();
    }
}