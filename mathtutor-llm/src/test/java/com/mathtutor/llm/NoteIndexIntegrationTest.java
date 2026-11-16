package com.mathtutor.llm;

import com.mathtutor.common.dto.NoteChunk;
import org.junit.jupiter.api.*;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.utility.DockerImageName;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** UC-14: caja negra sobre pgvector, idempotencia, sustitución y rollback del índice. */
@Testcontainers
class NoteIndexIntegrationTest {
    @Container static final PostgreSQLContainer<?> DB = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));
    JdbcTemplate jdbc;
    EmbeddingModel embedding;
    PgVectorNoteIndex index;
    @BeforeEach void setup() throws Exception {
        var data = new DriverManagerDataSource(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword());
        jdbc = new JdbcTemplate(data);
        embedding = mock(EmbeddingModel.class);
        when(embedding.dimensions()).thenReturn(3);
        var vector = PgVectorStore.builder(jdbc, embedding).dimensions(3).initializeSchema(true).build();
        vector.afterPropertiesSet();
        jdbc.update("DELETE FROM vector_store");
        index = new PgVectorNoteIndex(jdbc, embedding, new DataSourceTransactionManager(data), new ObjectMapper());
    }
    NoteChunk chunk(String version) {
        return new NoteChunk(null, "Contenido " + version, null, "OTRO", "tema.tex", "funciones", "dominio", version);
    }
    @Test void indexesMetadataAndSkipsAnUnchangedSnapshot() {
        when(embedding.embed(anyList())).thenAnswer(call -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return List.of(new float[]{1, 0, 0});
        });
        index.synchronize(List.of(chunk("v1")));
        index.synchronize(List.of(chunk("v1")));
        verify(embedding).embed(anyList());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM vector_store", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT metadata->>'module_code' FROM vector_store", String.class)).isEqualTo("funciones");
    }
    @Test void replacesChangedAndDeletedSourcesAndKeepsUnmanagedDocuments() {
        when(embedding.embed(anyList())).thenReturn(List.of(new float[]{1, 0, 0}));
        index.synchronize(List.of(chunk("v1")));
        jdbc.update("INSERT INTO vector_store(id,content,metadata,embedding) VALUES (gen_random_uuid(),'ajeno','{}','[1,0,0]')");
        index.synchronize(List.of(chunk("v2")));
        assertThat(jdbc.queryForList("SELECT content FROM vector_store", String.class)).containsExactlyInAnyOrder("ajeno", "Contenido v2");
        index.synchronize(List.of());
        assertThat(jdbc.queryForList("SELECT content FROM vector_store", String.class)).containsExactly("ajeno");
    }
    @Test void failedEmbeddingAndFailedWritesKeepThePreviousVersion() {
        when(embedding.embed(anyList())).thenReturn(List.of(new float[]{1, 0, 0}));
        index.synchronize(List.of(chunk("v1")));
        when(embedding.embed(anyList())).thenThrow(new IllegalStateException("sin proveedor"));
        assertThatThrownBy(() -> index.synchronize(List.of(chunk("v2")))).isInstanceOf(IllegalStateException.class);
        doReturn(List.of(new float[]{1, 0})).when(embedding).embed(anyList());
        assertThatThrownBy(() -> index.synchronize(List.of(chunk("v2")))).isInstanceOf(RuntimeException.class);
        assertThat(jdbc.queryForList("SELECT content FROM vector_store", String.class)).containsExactly("Contenido v1");
    }
}
