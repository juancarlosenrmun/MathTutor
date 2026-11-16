package com.mathtutor.application;

import com.mathtutor.application.config.NoteIndexStartup;
import com.mathtutor.llm.DeterministicEmbeddingModel;
import com.mathtutor.llm.SpringAiDeterministicEmbeddingModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.utility.DockerImageName;
import static org.assertj.core.api.Assertions.*;

/** UC-14: arranque completo con recursos versionados y embeddings simulados. */
@SpringBootTest(properties = {"spring.config.import=", "spring.jpa.open-in-view=false"})
@ActiveProfiles({"dev", "notes-indexing"})
@Import(NoteStartupIntegrationTest.Models.class)
@Testcontainers @DirtiesContext
class NoteStartupIntegrationTest {
    @Container static final PostgreSQLContainer<?> DB = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", DB::getJdbcUrl);
        r.add("spring.datasource.username", DB::getUsername);
        r.add("spring.datasource.password", DB::getPassword);
        r.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        r.add("jwt.secret", () -> "clave-de-prueba-arranque-de-apuntes-sin-servicios-reales");
    }
    @TestConfiguration static class Models {
        @Bean org.springframework.ai.embedding.EmbeddingModel documentEmbedding() {
            return new SpringAiDeterministicEmbeddingModel(new DeterministicEmbeddingModel());
        }
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired NoteIndexStartup startup;
    @Test void startupHasAlreadyIndexedThreeModulesAndRestartIsIdempotent() throws Exception {
        var ids = jdbc.queryForList("SELECT id::text FROM vector_store ORDER BY id", String.class);
        assertThat(ids).hasSize(121);
        assertThat(jdbc.queryForList("SELECT metadata->>'module_code' FROM vector_store", String.class))
                .containsOnly("funciones", "derivadas-integrales", "metodos-3d");
        assertThat(jdbc.queryForObject("SELECT count(DISTINCT metadata->>'source_file') FROM vector_store", Integer.class)).isEqualTo(8);
        startup.run(null);
        assertThat(jdbc.queryForList("SELECT id::text FROM vector_store ORDER BY id", String.class)).isEqualTo(ids);
    }
}
