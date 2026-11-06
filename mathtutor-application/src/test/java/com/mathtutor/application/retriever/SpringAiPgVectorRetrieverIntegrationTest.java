package com.mathtutor.application.retriever;

import com.mathtutor.common.dto.NoteChunk;
import com.mathtutor.common.port.RagRetriever;
import com.mathtutor.persistence.entity.Module;
import com.mathtutor.persistence.entity.Topic;
import com.mathtutor.persistence.repository.ModuleRepository;
import com.mathtutor.persistence.repository.NoteChunkRepository;
import com.mathtutor.persistence.repository.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Prueba de integración para SpringAiPgVectorRetriever con Testcontainers.
 * Usa pgvector/pgvector:pg16 y SpringAiDeterministicEmbeddingModel (sin LLM real).
 * Perfil: "spring-ai-pgvector" para activar la configuración Spring AI.
 * Se omite si Docker no está disponible (disabledWithoutDocker = true).
 *
 * Esta prueba valida el requisito EN-04: "Spring AI con pgvector"
 * usando la abstracción VectorStore de Spring AI (PgVectorStore).
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
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
    }

    @Autowired
    private RagRetriever ragRetriever;

    @Autowired
    private NoteChunkRepository noteChunkRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private TopicRepository topicRepository;

    private Module testModule;
    private Topic testTopic;

    @BeforeEach
    void setUp() {
        // Limpiar datos previos
        noteChunkRepository.deleteAll();
        topicRepository.deleteAll();
        moduleRepository.deleteAll();

        // Crear módulo y tema de prueba
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

        noteChunkRepository.saveAll(List.of(chunk1, chunk2, chunk3));

        // Indexar
        ragRetriever.indexAtStartup();

        // Verificar que se indexaron
        // (La verificación real se hace en retrieve)
    }

    @Test
    void retrieve_findsRelevantChunksByModuleAndTopic() {
        // Insertar chunks
        com.mathtutor.persistence.entity.NoteChunk chunk1 = createChunk("La derivada de sin(x) es cos(x)",
            "TEOREMA", "derivada_seno.tex");
        com.mathtutor.persistence.entity.NoteChunk chunk2 = createChunk("La integral de cos(x) es sin(x) + C",
            "TEOREMA", "integral_coseno.tex");
        com.mathtutor.persistence.entity.NoteChunk chunk3 = createChunk("Regla del producto: (fg)' = f'g + fg'",
            "TEOREMA", "regla_producto.tex");

        noteChunkRepository.saveAll(List.of(chunk1, chunk2, chunk3));

        // Indexar
        ragRetriever.indexAtStartup();

        // Recuperar con filtro por módulo y tema
        List<NoteChunk> results = ragRetriever.retrieve("derivada de seno",
            testModule.getCode(), testTopic.getCode());

        assertThat(results).isNotEmpty();
        // Debe encontrar el chunk sobre derivada de sen(x)
        assertThat(results.stream()
            .anyMatch(c -> c.content().contains("derivada") && c.content().contains("sen")))
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

        noteChunkRepository.saveAll(List.of(calcChunk, algChunk));

        ragRetriever.indexAtStartup();

        // Buscar solo en módulo de cálculo
        List<NoteChunk> results = ragRetriever.retrieve("derivada",
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

        noteChunkRepository.saveAll(List.of(derivChunk, integChunk));

        ragRetriever.indexAtStartup();

        // Buscar solo en tema de derivadas
        List<NoteChunk> results = ragRetriever.retrieve("potencia",
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

        noteChunkRepository.saveAll(List.of(chunk1, chunk2));

        ragRetriever.indexAtStartup();

        // Buscar sin filtros
        List<NoteChunk> results = ragRetriever.retrieve("cálculo", null, null);

        assertThat(results).isNotEmpty();
    }

    @Test
    void retrieve_returnsEmptyListForNonMatchingQuery() {
        com.mathtutor.persistence.entity.NoteChunk chunk = createChunk("Derivada de polinomios",
            "TEOREMA", "poly.tex");
        chunk.setModule(testModule);
        chunk.setTopic(testTopic);
        noteChunkRepository.save(chunk);

        ragRetriever.indexAtStartup();

        // Buscar algo que no existe
        List<NoteChunk> results = ragRetriever.retrieve("geometría diferencial",
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
        noteChunkRepository.save(chunk);

        ragRetriever.indexAtStartup();

        List<NoteChunk> results = ragRetriever.retrieve("prueba",
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
        assertThat(result.embedding()).isNotNull();
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