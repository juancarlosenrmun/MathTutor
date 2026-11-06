package com.mathtutor.llm.retriever;

import com.mathtutor.common.dto.NoteChunk;
import com.mathtutor.common.port.NoteChunkStore;
import com.mathtutor.common.port.RagRetriever;
import com.mathtutor.llm.DeterministicEmbeddingModel;
import com.mathtutor.llm.PgVectorRetriever;
import com.pgvector.PGvector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas de caja blanca para PgVectorRetriever.
 * Usa mocks para JdbcTemplate y NoteChunkStore.
 * Verifica lógica SQL, parámetros, mapeo y flujos de error.
 */
@ExtendWith(MockitoExtension.class)
class PgVectorRetrieverWhiteBoxTest {

    @Mock
    private NoteChunkStore noteChunkStore;

    @Mock
    private DeterministicEmbeddingModel embeddingModel;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private PgVectorRetriever retriever;

    @BeforeEach
    void setUp() {
        retriever = new PgVectorRetriever(noteChunkStore, embeddingModel, jdbcTemplate);
    }

    @Test
    @DisplayName("retrieve con query null retorna lista vacía")
    void retrieveWithNullQueryReturnsEmptyList() {
        List<NoteChunk> result = retriever.retrieve(null, "CALC", "DERIV");

        assertThat(result).isEmpty();
        verifyNoInteractions(jdbcTemplate, embeddingModel);
    }

    @Test
    @DisplayName("retrieve con query vacía retorna lista vacía")
    void retrieveWithEmptyQueryReturnsEmptyList() {
        List<NoteChunk> result = retriever.retrieve("   ", "CALC", "DERIV");

        assertThat(result).isEmpty();
        verifyNoInteractions(jdbcTemplate, embeddingModel);
    }

    @Test
    @DisplayName("retrieve genera embedding y construye SQL con parámetros correctos")
    void retrieveBuildsSqlWithCorrectParameters() {
        // Given
        String query = "derivada de x^2";
        float[] queryEmbedding = new float[384];
        Arrays.fill(queryEmbedding, 0.1f);
        when(embeddingModel.embed(query)).thenReturn(queryEmbedding);

        NoteChunk chunk = new NoteChunk(1L, "Contenido", null, "TEOREMA",
            "test.tex", "CALC", "DERIV", "1");
        when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
            .thenReturn(List.of(chunk));

        // When
        List<NoteChunk> result = retriever.retrieve(query, "CALC", "DERIV");

        // Then
        assertThat(result).hasSize(1);

        // Verificar que se generó el embedding
        verify(embeddingModel).embed(query);

        // Verificar SQL y parámetros
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> paramsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), paramsCaptor.capture(), any(RowMapper.class));

        String sql = sqlCaptor.getValue();
        Object[] params = paramsCaptor.getValue();

        assertThat(sql).contains("embedding <=> ?::vector AS distance");
        assertThat(sql).contains("module_code = ?");
        assertThat(sql).contains("topic_code = ?");
        assertThat(sql).contains("embedding <=> ?::vector < 0.3");
        assertThat(sql).contains("ORDER BY distance ASC LIMIT 10");

        assertThat(params).hasSize(4);
        assertThat(params[0]).isInstanceOf(PGvector.class);
        assertThat(params[1]).isEqualTo("CALC");
        assertThat(params[2]).isEqualTo("DERIV");
        assertThat(params[3]).isInstanceOf(PGvector.class);
    }

    @Test
    @DisplayName("retrieve sin filtro de módulo ni tema")
    void retrieveWithoutModuleOrTopicFilters() {
        // Given
        float[] embedding = new float[384];
        Arrays.fill(embedding, 0.1f);
        when(embeddingModel.embed(anyString())).thenReturn(embedding);

        NoteChunk chunk = new NoteChunk(1L, "Contenido", null, "TEOREMA",
            "test.tex", "CALC", "DERIV", "1");
        when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
            .thenReturn(List.of(chunk));

        // When
        List<NoteChunk> result = retriever.retrieve("query", null, null);

        // Then
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> paramsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), paramsCaptor.capture(), any(RowMapper.class));

        String sql = sqlCaptor.getValue();
        Object[] params = paramsCaptor.getValue();

        assertThat(sql).doesNotContain("module_code = ?");
        assertThat(sql).doesNotContain("topic_code = ?");
        assertThat(params).hasSize(2); // query embedding (distance) + query embedding (threshold)
    }

    @Test
    @DisplayName("retrieve solo con filtro de módulo")
    void retrieveWithOnlyModuleFilter() {
        float[] embedding = new float[384];
        Arrays.fill(embedding, 0.1f);
        when(embeddingModel.embed(anyString())).thenReturn(embedding);

        NoteChunk chunk = new NoteChunk(1L, "Contenido", null, "TEOREMA",
            "test.tex", "CALC", "DERIV", "1");
        when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
            .thenReturn(List.of(chunk));

        List<NoteChunk> result = retriever.retrieve("query", "CALC", null);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> paramsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), paramsCaptor.capture(), any(RowMapper.class));

        String sql = sqlCaptor.getValue();
        Object[] params = paramsCaptor.getValue();

        assertThat(sql).contains("module_code = ?");
        assertThat(sql).doesNotContain("topic_code = ?");
        assertThat(params).hasSize(3);
        assertThat(params[0]).isInstanceOf(PGvector.class);
        assertThat(params[1]).isEqualTo("CALC");
        assertThat(params[2]).isInstanceOf(PGvector.class);
    }

    @Test
    @DisplayName("retrieve solo con filtro de tema")
    void retrieveWithOnlyTopicFilter() {
        float[] embedding = new float[384];
        Arrays.fill(embedding, 0.1f);
        when(embeddingModel.embed(anyString())).thenReturn(embedding);

        NoteChunk chunk = new NoteChunk(1L, "Contenido", null, "TEOREMA",
            "test.tex", "CALC", "DERIV", "1");
        when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
            .thenReturn(List.of(chunk));

        List<NoteChunk> result = retriever.retrieve("query", null, "DERIV");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> paramsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), paramsCaptor.capture(), any(RowMapper.class));

        String sql = sqlCaptor.getValue();
        Object[] params = paramsCaptor.getValue();

        assertThat(sql).doesNotContain("module_code = ?");
        assertThat(sql).contains("topic_code = ?");
        assertThat(params).hasSize(3);
        assertThat(params[0]).isInstanceOf(PGvector.class);
        assertThat(params[1]).isEqualTo("DERIV");
        assertThat(params[2]).isInstanceOf(PGvector.class);
    }

    @Test
    @DisplayName("retrieve mapea correctamente los campos del ResultSet")
    void retrieveMapsResultSetCorrectly() throws SQLException {
        float[] embedding = new float[384];
        Arrays.fill(embedding, 0.1f);
        when(embeddingModel.embed(anyString())).thenReturn(embedding);

        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(42L);
        when(rs.getString("content")).thenReturn("Derivada de sen(x)");
        when(rs.getString("type")).thenReturn("TEOREMA");
        when(rs.getString("source_file")).thenReturn("deriv.tex");
        when(rs.getString("module_code")).thenReturn("CALC");
        when(rs.getString("topic_code")).thenReturn("DERIV");
        when(rs.getString("version")).thenReturn("v2");

        // El query devuelve una fila
        when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
            .thenAnswer(invocation -> {
                RowMapper<NoteChunk> mapper = invocation.getArgument(2);
                return List.of(mapper.mapRow(rs, 0));
            });

        // When
        List<NoteChunk> result = retriever.retrieve("derivada", "CALC", "DERIV");

        // Then
        assertThat(result).hasSize(1);
        NoteChunk chunk = result.get(0);
        assertThat(chunk.id()).isEqualTo(42L);
        assertThat(chunk.content()).isEqualTo("Derivada de sen(x)");
        assertThat(chunk.type()).isEqualTo("TEOREMA");
        assertThat(chunk.sourceFile()).isEqualTo("deriv.tex");
        assertThat(chunk.moduleCode()).isEqualTo("CALC");
        assertThat(chunk.topicCode()).isEqualTo("DERIV");
        assertThat(chunk.version()).isEqualTo("v2");
        assertThat(chunk.embedding()).isNull(); // No se devuelve en búsqueda
    }

    @Test
    @DisplayName("retrieve retorna lista vacía cuando JDBC no encuentra resultados")
    void retrieveReturnsEmptyListWhenNoResults() {
        float[] embedding = new float[384];
        Arrays.fill(embedding, 0.1f);
        when(embeddingModel.embed(anyString())).thenReturn(embedding);
        when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
            .thenReturn(Collections.emptyList());

        List<NoteChunk> result = retriever.retrieve("query", "CALC", "DERIV");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("indexAtStartup no hace nada si no hay chunks")
    void indexAtStartupDoesNothingWhenNoChunks() {
        when(noteChunkStore.findAll()).thenReturn(Collections.emptyList());

        retriever.indexAtStartup();

        verify(noteChunkStore).findAll();
        verifyNoMoreInteractions(noteChunkStore, embeddingModel);
    }

    @Test
    @DisplayName("indexAtStartup genera embeddings solo para chunks sin embedding")
    void indexAtStartupGeneratesEmbeddingsOnlyForChunksWithoutEmbedding() {
        // Given - usando DTOs en lugar de entidades JPA
        NoteChunk chunk1 = new NoteChunk(1L, "Contenido 1", new float[0], "TEOREMA", "test.tex", "CALC", "DERIV", "1"); // Sin embedding
        NoteChunk chunk2 = new NoteChunk(2L, "Contenido 2", new float[]{0.1f, 0.2f}, "TEOREMA", "test.tex", "CALC", "DERIV", "1"); // Con embedding
        NoteChunk chunk3 = new NoteChunk(3L, "Contenido 3", null, "TEOREMA", "test.tex", "CALC", "DERIV", "1"); // Sin embedding (null)

        when(noteChunkStore.findAll()).thenReturn(List.of(chunk1, chunk2, chunk3));

        float[] emb1 = new float[384];
        Arrays.fill(emb1, 0.1f);
        float[] emb3 = new float[384];
        Arrays.fill(emb3, 0.3f);
        when(embeddingModel.embed("Contenido 1")).thenReturn(emb1);
        when(embeddingModel.embed("Contenido 3")).thenReturn(emb3);

        // When
        retriever.indexAtStartup();

        // Then
        verify(embeddingModel).embed("Contenido 1");
        verify(embeddingModel).embed("Contenido 3");
        verify(embeddingModel, never()).embed("Contenido 2");

        // Verificar que se guardaron los chunks con nuevos embeddings (usando DTO)
        ArgumentCaptor<NoteChunk> saveCaptor = ArgumentCaptor.forClass(NoteChunk.class);
        verify(noteChunkStore, times(2)).save(saveCaptor.capture());

        List<NoteChunk> saved = saveCaptor.getAllValues();
        assertThat(saved).hasSize(2);
        // chunk1 y chunk3 deben haberse guardado
        assertThat(saved.get(0).content()).isIn("Contenido 1", "Contenido 3");
        assertThat(saved.get(1).content()).isIn("Contenido 1", "Contenido 3");
    }

    @Test
    @DisplayName("indexAtStartup llama a embeddingModel.embed con el contenido correcto")
    void indexAtStartupCallsEmbedWithCorrectContent() {
        NoteChunk chunk = new NoteChunk(1L, "Texto para embedder", new float[0], "TEOREMA", "test.tex", "CALC", "DERIV", "1");
        when(noteChunkStore.findAll()).thenReturn(List.of(chunk));

        float[] embedding = new float[384];
        Arrays.fill(embedding, 0.5f);
        when(embeddingModel.embed("Texto para embedder")).thenReturn(embedding);

        retriever.indexAtStartup();

        verify(embeddingModel).embed("Texto para embedder");

        ArgumentCaptor<NoteChunk> saveCaptor = ArgumentCaptor.forClass(NoteChunk.class);
        verify(noteChunkStore).save(saveCaptor.capture());
        // getEmbedding() devuelve float[] (384 floats)
        assertThat(saveCaptor.getValue().embedding()).isNotNull();
        assertThat(saveCaptor.getValue().embedding()).hasSize(384);
    }
}