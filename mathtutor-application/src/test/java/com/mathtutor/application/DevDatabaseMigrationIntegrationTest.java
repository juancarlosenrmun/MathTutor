package com.mathtutor.application;

import com.mathtutor.common.port.CasEngine;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/** EN-01: una BD vacía se migra antes de validar las entidades, igual que en dev. */
@SpringBootTest(properties = "spring.config.import=")
@ActiveProfiles("dev")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DevDatabaseMigrationIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("jwt.secret", () -> "clave-de-prueba-local-mayor-de-treinta-y-dos-caracteres");
    }

    @Autowired
    Flyway flyway;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    CasEngine casEngine;

    @Test
    void startsWithAllExistingMappingsValidatedAfterFlyway() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("8");
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pg_extension WHERE extname = 'vector'", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM information_schema.tables "
                + "WHERE table_schema = 'public' AND table_name IN "
                + "('users','refresh_tokens','audit_logs','password_reset_tokens','modules','topics',"
                + "'exercises','canonical_steps','typical_errors','key_concepts','note_chunks',"
                + "'tutor_sessions','exercise_runs','turns','consent_records','usage_quotas')", Integer.class))
                .isEqualTo(16);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM topics", Integer.class)).isEqualTo(8);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM exercises", Integer.class)).isEqualTo(6);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM canonical_steps", Integer.class)).isGreaterThanOrEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM typical_errors", Integer.class)).isGreaterThanOrEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(DISTINCT m.code) FROM exercises e JOIN modules m ON m.id=e.module_id", Integer.class)).isEqualTo(3);
    }

    @Test
    void wiresTheCasModuleInTheCompositionRoot() {
        assertThat(casEngine.equivalent("x+x", "2*x")).isTrue();
    }

    @Test
    void exerciseBankHasStableIdsTopicsSolutionsAndSeedHints() {
        assertThat(jdbc.queryForList("SELECT external_id FROM exercises ORDER BY external_id", String.class))
                .containsExactly("3D-PROC-001", "3D-TEO-001", "CAL-PROC-001", "CAL-TEO-001", "FUN-INT-001", "FUN-TEO-001");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM exercises WHERE topic_id IS NOT NULL "
                + "AND module_id IS NOT NULL AND reference_solution_latex <> ''", Integer.class)).isEqualTo(6);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM exercises WHERE type = 'TEORICO'", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM canonical_steps", Integer.class)).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM typical_errors WHERE seed_hint <> ''", Integer.class)).isEqualTo(2);
    }

    @Test
    void rerunningMigrationsDoesNotDuplicateExerciseBank() {
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM exercises", Integer.class)).isEqualTo(6);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM canonical_steps", Integer.class)).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM typical_errors", Integer.class)).isEqualTo(2);
    }
}
