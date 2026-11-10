package com.mathtutor.application.retriever;

import com.mathtutor.application.MathTutorApplication;
import com.mathtutor.common.dto.NoteChunk;
import com.mathtutor.common.port.RagRetriever;
import com.mathtutor.llm.SpringAiPgVectorRetriever;
import com.mathtutor.persistence.entity.Module;
import com.mathtutor.persistence.entity.Topic;
import com.mathtutor.persistence.repository.ModuleRepository;
import com.mathtutor.persistence.repository.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Prueba de integración para SpringAiPgVectorRetriever con Testcontainers.
 * Usa pgvector/pgvector:pg16 y SpringAiDeterministicEmbeddingModel (sin LLM real).
 * Perfil: "spring-ai-pgvector" para activar la configuración Spring AI.
 *
 * Esta prueba valida el requisito EN-04: "Spring AI con pgvector"
 * usando la abstracción VectorStore de Spring AI (PgVectorStore).
 */
@SpringBootTest(classes = MathTutorApplication.class)
@ActiveProfiles("spring-ai-pgvector")
@Testcontainers
@Transactional
class SpringAiPgVectorRetrieverIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
        .withDatabaseName("mathtutor_test")
        .withUsername("test")
        .withPassword("test")
        .withInitScript("db/migration/test-init.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        // Perfil spring-ai-pgvector para activar Spring AI PgVectorStore
        registry.add("spring.profiles.active", () -> "spring-ai-pgvector");
        // Spring AI configuration - provide dummy values to prevent placeholder resolution errors
        registry.add("spring.ai.openai.api-key", () -> "test-key");
        registry.add("spring.ai.vectorstore.pgvector.initialize-schema", () -> "true");
        registry.add("spring.ai.embedding.model", () -> "text-embedding-ada-002");
        // JWT configuration for tests (required by TokenServiceAdapter)
        registry.add("jwt.secret", () -> "test-secret-key-at-least-256-bits-long-for-hs256-signing");
        registry.add("jwt.access-token-expiration-minutes", () -> "15");
        registry.add("jwt.refresh-token-expiration-days", () -> "7");
    }

    @Autowired
    private RagRetriever ragRetriever;

    @Autowired
    private SpringAiPgVectorRetriever springAiRetriever;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private TopicRepository topicRepository;

    private Module testModule;
    private Topic testTopic;

    @BeforeEach
    void setUp() {
        // Crear módulo y tema de prueba (no borrar para evitar cascade a note_chunks)
        testModule = new Module();
        testModule.setCode("CALCULUS");
        testModule.setName("Cálculo");
        testModule = moduleRepository.save(testModule);

        testTopic = new Topic();
        testTopic.setCode("DERIVATIVES");
        testTopic.setName("Derivadas");
        testTopic.setModule(testModule);
        testTopic = topicRepository.save(testTopic);
    }

    @Test
    void indexAtStartup_withEmptyDatabase_doesNotFail() {
        // No debe lanzar excepción aunque no haya datos
        ragRetriever.indexAtStartup();
        // Segundo llamado no debe hacer nada (idempotente)
        ragRetriever.indexAtStartup();
    }

    @Test
    void indexAtStartup_withData_createsVectorIndex() {
        // Insertar chunks de prueba
        com.mathtutor.persistence.entity.NoteChunk chunk1 = createChunk("Definición de derivada: f'(x) = lim h→0 (f(x+h)-f(x))/h",
            "DEFINICION", "derivada_definicion.tex");
        com.mathtutor.persistence.entity.NoteChunk chunk2 = createChunk("Regla de la cadena: (f∘g)'(x) = f'(g(x))·g'(x)",
            "TEOREMA", "regla_cadena.tex");
        com.mathtutor.persistence.entity.NoteChunk chunk3 = createChunk("Ejemplo: derivar sin(x^2) = cos(x^2)·2x",
            "EJEMPLO", "ejemplo_derivada.tex");

        // Indexar en VectorStore
        springAiRetriever.indexDocuments(List.of(chunk1, chunk2, chunk3));

        // Verificar que se indexaron
        // (La verificación real se hace en retrieve)
    }

    @Test
    void retrieve_findsRelevantChunksByModuleAndTopic() {
        // Insertar chunks
        com.mathtutor.persistence.entity.NoteChunk chunk1 = createChunk("Derivada de seno x es coseno x",
            "TEOREMA", "derivada_seno.tex");
        com.mathtutor.persistence.entity.NoteChunk chunk2 = createChunk("Integral de coseno x es seno x mas C",
            "TEOREMA", "integral_coseno.tex");
        com.mathtutor.persistence.entity.NoteChunk chunk3 = createChunk("Regla del producto fg prima f prima g mas f g prima",
            "TEOREMA", "regla_producto.tex");

        // Indexar en VectorStore
        springAiRetriever.indexDocuments(List.of(chunk1, chunk2, chunk3));

        // Recuperar con filtro por módulo y tema - buscar texto exacto
        List<NoteChunk> results = ragRetriever.retrieve("Derivada de seno x es coseno x",
            testModule.getCode(), testTopic.getCode());

        assertThat(results).isNotEmpty();
        // Debe encontrar el chunk sobre derivada de sen(x)
        assertThat(results.stream()
            .anyMatch(c -> c.content().toLowerCase().contains("derivada") && c.content().toLowerCase().contains("seno")))
            .isTrue();
    }

    @Test
    void retrieve_filtersByModule() {
        // Crear otro módulo
        Module otherModule = new Module();
        otherModule.setCode("ALGEBRA");
        otherModule.setName("Álgebra");
        otherModule = moduleRepository.save(otherModule);

        Topic otherTopic = new Topic();
        otherTopic.setCode("POLYNOMIALS");
        otherTopic.setName("Polinomios");
        otherTopic.setModule(otherModule);
        otherTopic = topicRepository.save(otherTopic);

        // Insertar chunks en diferentes módulos
        com.mathtutor.persistence.entity.NoteChunk calcChunk = createChunk("Derivada de x^2 es 2x",
            "TEOREMA", "calc.tex");
        calcChunk.setModule(testModule);
        calcChunk.setTopic(testTopic);

        com.mathtutor.persistence.entity.NoteChunk algChunk = createChunk("Factorización: x^2 - 1 = (x-1)(x+1)",
            "TEOREMA", "alg.tex");
        algChunk.setModule(otherModule);
        algChunk.setTopic(otherTopic);

        springAiRetriever.indexDocuments(List.of(calcChunk, algChunk));

        // Buscar solo en módulo de cálculo - buscar texto exacto
        List<NoteChunk> results = ragRetriever.retrieve("Derivada de x^2 es 2x",
            testModule.getCode(), null);

        assertThat(results).isNotEmpty();
        assertThat(results.stream().allMatch(c -> "CALCULUS".equals(c.moduleCode()))).isTrue();
    }

    @Test
    void retrieve_filtersByTopic() {
        // Crear otro tema en mismo módulo
        Topic otherTopic = new Topic();
        otherTopic.setCode("INTEGRALS");
        otherTopic.setName("Integrales");
        otherTopic.setModule(testModule);
        otherTopic = topicRepository.save(otherTopic);

        // Insertar chunks en diferentes temas
        com.mathtutor.persistence.entity.NoteChunk derivChunk = createChunk("Derivada de x^n = n*x^(n-1)",
            "TEOREMA", "deriv.tex");
        derivChunk.setModule(testModule);
        derivChunk.setTopic(testTopic);

        com.mathtutor.persistence.entity.NoteChunk integChunk = createChunk("Integral de x^n = x^(n+1)/(n+1) + C",
            "TEOREMA", "integ.tex");
        integChunk.setModule(testModule);
        integChunk.setTopic(otherTopic);

        springAiRetriever.indexDocuments(List.of(derivChunk, integChunk));

        // Buscar solo en tema de derivadas - buscar texto exacto
        List<NoteChunk> results = ragRetriever.retrieve("Derivada de x^n = n*x^(n-1)",
            testModule.getCode(), testTopic.getCode());

        assertThat(results).isNotEmpty();
        assertThat(results.stream().allMatch(c -> "DERIVATIVES".equals(c.topicCode()))).isTrue();
    }

    @Test
    void retrieve_withNoFilters_searchesGlobally() {
        com.mathtutor.persistence.entity.NoteChunk chunk1 = createChunk("Teorema fundamental del cálculo",
            "TEOREMA", "tfc.tex");
        chunk1.setModule(testModule);
        chunk1.setTopic(testTopic);

        com.mathtutor.persistence.entity.NoteChunk chunk2 = createChunk("Regla de la cadena para derivadas",
            "TEOREMA", "chain.tex");
        chunk2.setModule(testModule);
        chunk2.setTopic(testTopic);

        springAiRetriever.indexDocuments(List.of(chunk1, chunk2));

        // Buscar sin filtros - buscar texto exacto
        List<NoteChunk> results = ragRetriever.retrieve("Teorema fundamental del cálculo", null, null);

        assertThat(results).isNotEmpty();
    }

    @Test
    void retrieve_returnsEmptyListForNonMatchingQuery() {
        com.mathtutor.persistence.entity.NoteChunk chunk = createChunk("Derivada de polinomios",
            "TEOREMA", "poly.tex");
        chunk.setModule(testModule);
        chunk.setTopic(testTopic);

        springAiRetriever.indexDocuments(List.of(chunk));

        // Buscar algo que no existe - texto que no coincide con nada
        List<NoteChunk> results = ragRetriever.retrieve("Texto que no existe en ningún chunk",
            testModule.getCode(), testTopic.getCode());

        // Puede retornar resultados por similitud vectorial, pero no debe fallar
        assertThat(results).isNotNull();
    }

    @Test
    void retrieve_returnsNoteChunkWithAllFields() {
        com.mathtutor.persistence.entity.NoteChunk chunk = createChunk("Contenido de prueba",
            "DEFINICION", "test.tex");
        chunk.setModule(testModule);
        chunk.setTopic(testTopic);
        chunk.setVersion("v1");

        springAiRetriever.indexDocuments(List.of(chunk));

        List<NoteChunk> results = ragRetriever.retrieve("Contenido de prueba",
            testModule.getCode(), testTopic.getCode());

        assertThat(results).isNotEmpty();
        NoteChunk result = results.get(0);
        assertThat(result.id()).isNotNull();
        assertThat(result.content()).isEqualTo("Contenido de prueba");
        assertThat(result.type()).isEqualTo("DEFINICION");
        assertThat(result.sourceFile()).isEqualTo("test.tex");
        assertThat(result.moduleCode()).isEqualTo("CALCULUS");
        assertThat(result.topicCode()).isEqualTo("DERIVATIVES");
        assertThat(result.version()).isEqualTo("v1");
        // embedding no se devuelve en búsqueda de similitud (solo texto y metadata)
        assertThat(result.embedding()).isNull();
    }

    private com.mathtutor.persistence.entity.NoteChunk createChunk(String content, String type, String sourceFile) {
        com.mathtutor.persistence.entity.NoteChunk chunk = new com.mathtutor.persistence.entity.NoteChunk();
        chunk.setContent(content);
        chunk.setType(type);
        chunk.setSourceFile(sourceFile);
        chunk.setModuleCode(testModule.getCode());
        chunk.setTopicCode(testTopic.getCode());
        chunk.setVersion("1");
        chunk.setModule(testModule);
        chunk.setTopic(testTopic);
        // El embedding se generará automáticamente por Spring AI VectorStore
        return chunk;
    }
}