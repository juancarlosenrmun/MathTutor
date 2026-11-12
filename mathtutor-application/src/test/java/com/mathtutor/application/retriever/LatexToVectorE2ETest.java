package com.mathtutor.application.retriever;

import com.mathtutor.application.MathTutorApplication;
import com.mathtutor.cas.LatexContentExtractor;
import com.mathtutor.common.dto.NoteChunk;
import com.mathtutor.common.port.RagRetriever;
import com.mathtutor.llm.SpringAiPgVectorRetriever;
import com.mathtutor.persistence.entity.Module;
import com.mathtutor.persistence.entity.Topic;
import com.mathtutor.persistence.repository.ModuleRepository;
import com.mathtutor.persistence.repository.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Test E2E de integración completa: .tex → LatexContentExtractor → Google AI Studio embeddings → pgvector → búsqueda.
 *
 * Este test valida el flujo completo del pipeline RAG:
 * 1. Archivo .tex con macros y entornos según contrato LaTeX
 * 2. Extracción de contenido con LatexContentExtractor (macros, entornos, fórmulas)
 * 3. Conversión a NoteChunks del dominio
 * 4. Generación de embeddings con Google AI Studio (gemini-embedding-001, 768 dim)
 * 5. Almacenamiento en pgvector via Spring AI VectorStore
 * 6. Búsqueda por similitud con umbral configurable
 *
 * Perfiles requeridos: "spring-ai-pgvector" + "google-ai-studio"
 * Requiere: GOOGLE_AI_API_KEY en .env o variable de entorno
 * Tag: "real-api" para ejecución selectiva (requiere API key real)
 */
@SpringBootTest(classes = MathTutorApplication.class)
@ActiveProfiles({"spring-ai-pgvector", "google-ai-studio"})
@Testcontainers(disabledWithoutDocker = true)
@Tag("real-api")
class LatexToVectorE2ETest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
        .withDatabaseName("mathtutor_test")
        .withUsername("test")
        .withPassword("test")
        .withInitScript("db/migration/test-init.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String apiKey = System.getenv("GOOGLE_AI_API_KEY");
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.profiles.active", () -> "spring-ai-pgvector,google-ai-studio");
        registry.add("spring.ai.vectorstore.pgvector.initialize-schema", () -> "true");
        // Google AI Studio API key (required for google-ai-studio profile)
        registry.add("spring.ai.google.ai-studio.api-key", () -> apiKey);
        registry.add("spring.ai.google.ai-studio.embedding-model", () -> "gemini-embedding-001");
        registry.add("spring.ai.google.ai-studio.embedding-dimensions", () -> "768");
        registry.add("spring.ai.google.ai-studio.max-retries", () -> "5");
        registry.add("spring.ai.google.ai-studio.max-requests-per-minute", () -> "100");
        // JWT configuration for tests (required by TokenServiceAdapter)
        registry.add("jwt.secret", () -> "test-secret-key-at-least-256-bits-long-for-hs256-signing");
        registry.add("jwt.access-token-expiration-minutes", () -> "15");
        registry.add("jwt.refresh-token-expiration-days", () -> "7");
        // Similarity threshold from application.yml (0.7)
        registry.add("tutor.rag.similarity-threshold", () -> "0.7");
    }

    @Autowired
    private RagRetriever ragRetriever;

    @Autowired
    private SpringAiPgVectorRetriever springAiRetriever;

    // LatexContentExtractor is a simple component without dependencies, instantiate directly
    private final LatexContentExtractor latexExtractor = new LatexContentExtractor();

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private TopicRepository topicRepository;

    private Module calculusModule;
    private Topic derivativesTopic;

    @BeforeEach
    void setUp() {
        // Crear módulo y tema de prueba (usar códigos únicos para evitar conflictos entre tests)
        String testSuffix = "_" + System.currentTimeMillis() + "_" + Thread.currentThread().threadId();

        calculusModule = new Module();
        calculusModule.setCode("CALCULUS" + testSuffix);
        calculusModule.setName("Cálculo");
        calculusModule = moduleRepository.save(calculusModule);

        derivativesTopic = new Topic();
        derivativesTopic.setCode("DERIVATIVES" + testSuffix);
        derivativesTopic.setName("Derivadas");
        derivativesTopic.setModule(calculusModule);
        derivativesTopic = topicRepository.save(derivativesTopic);
    }

    /**
     * TEST E2E PRINCIPAL: Flujo completo .tex → extractor → embeddings → pgvector → búsqueda
     */
    @Test
    void e2e_latexToVectorPipeline_worksEndToEnd() {
        // ============================================================
        // PASO 1: Contenido .tex simulado (según contrato LaTeX)
        // ============================================================
        String texContent = """
                \\bloque{Derivadas}
                \\tema{Regla de la cadena}
                \\dificultad{2}

                \\begin{ejercicio}
                Derive la función $f(x) = \\sin(x^2)$ usando la regla de la cadena.
                \\end{ejercicio}

                \\begin{enunciado}
                Dada la función compuesta $f(x) = \\sin(x^2)$, calcule su derivada.
                Identifique la función externa $f(u) = \\sin(u)$ y la interna $u = x^2$.
                \\end{enunciado}

                \\begin{solucion}
                $$\\frac{d}{dx}\\sin(x^2) = \\cos(x^2) \\cdot 2x = 2x\\cos(x^2)$$
                Paso 1: Derivada de la externa $\\cos(u)$
                Paso 2: Derivada de la interna $2x$
                Paso 3: Multiplicar: $\\cos(x^2) \\cdot 2x$
                \\end{solucion}

                \\begin{paso}
                Identificar funci\\'on externa $f(u) = \\sin(u)$ e interna $u = x^2$
                \\end{paso}

                \\begin{paso}
                Derivar externa: $f'(u) = \\cos(u)$
                \\end{paso}

                \\begin{paso}
                Derivar interna: $u' = 2x$
                \\end{paso}

                \\begin{errorTipico}
                Olvidar multiplicar por la derivada de la funci\\'on interna ($2x$)
                \\end{errorTipico}

                \\begin{conceptoClave}
                Regla de la cadena: $(f\\circ g)'(x) = f'(g(x))\\cdot g'(x)$
                \\end{conceptoClave}
                """;

        // ============================================================
        // PASO 2: Extraer contenido con LatexContentExtractor
        // ============================================================
        LatexContentExtractor.ExtractionResult extraction = latexExtractor.extract(texContent);

        // Validar extracción exitosa
        assertThat(extraction.hasErrors()).as("Extracción no debe tener errores: %s", extraction.getErrors()).isFalse();
        assertThat(extraction.getBloque()).isEqualTo("Derivadas");
        assertThat(extraction.getTema()).isEqualTo("Regla de la cadena");
        assertThat(extraction.getDificultad()).isEqualTo(2);

        // Validar entornos extraídos
        assertThat(extraction.getEnvironments()).containsKeys("ejercicio", "enunciado", "solucion", "paso", "errorTipico", "conceptoClave");
        assertThat(extraction.getEnvironments().get("paso")).hasSize(3);

        // Validar fórmulas extraídas (inline + display)
        assertThat(extraction.getFormulas()).isNotEmpty();
        System.out.println("Fórmulas extraídas: " + extraction.getFormulas().size());
        extraction.getFormulas().forEach(f -> System.out.println("  - " + f));

        // ============================================================
        // PASO 3: Convertir a NoteChunks para indexación
        // ============================================================
        List<com.mathtutor.persistence.entity.NoteChunk> chunks = new ArrayList<>();

        // Chunk principal: ejercicio + enunciado
        String mainContent = "Ejercicio: " + join(extraction.getEnvironments().get("ejercicio")) +
                            "\nEnunciado: " + join(extraction.getEnvironments().get("enunciado"));
        chunks.add(createChunk(mainContent, "EJERCICIO", "derivada_cadena_ejercicio.tex"));

        // Chunk: solución completa
        chunks.add(createChunk(join(extraction.getEnvironments().get("solucion")), "SOLUCION", "derivada_cadena_solucion.tex"));

        // Chunks: cada paso individual
        List<String> pasos = extraction.getEnvironments().get("paso");
        for (int i = 0; i < pasos.size(); i++) {
            chunks.add(createChunk("Paso " + (i+1) + ": " + pasos.get(i), "PASO", "derivada_cadena_paso" + (i+1) + ".tex"));
        }

        // Chunk: error típico
        chunks.add(createChunk(join(extraction.getEnvironments().get("errorTipico")), "ERROR_TIPICO", "derivada_cadena_error.tex"));

        // Chunk: concepto clave
        chunks.add(createChunk(join(extraction.getEnvironments().get("conceptoClave")), "CONCEPTO_CLAVE", "derivada_cadena_concepto.tex"));

        System.out.println("Total chunks a indexar: " + chunks.size());

        // ============================================================
        // PASO 4: Indexar en pgvector usando Google AI Studio embeddings
        // ============================================================
        springAiRetriever.indexDocuments(chunks);

        // ============================================================
        // PASO 5: Búsqueda por similitud - consultas en dominio
        // ============================================================
        System.out.println("\n=== BÚSQUEDAS EN DOMINIO ===");

        // Consulta 1: Buscar por "regla de la cadena"
        List<NoteChunk> results1 = ragRetriever.retrieve("regla de la cadena", calculusModule.getCode(), derivativesTopic.getCode());
        assertThat(results1).as("Debe encontrar chunks sobre regla de la cadena").isNotEmpty();
        assertThat(results1.stream().anyMatch(c -> c.content().toLowerCase().contains("regla de la cadena") || c.content().toLowerCase().contains("chain rule")))
            .as("Debe contener contenido sobre regla de la cadena").isTrue();
        System.out.println("Consulta 'regla de la cadena': " + results1.size() + " resultados");
        results1.forEach(r -> System.out.println("  - [" + r.type() + "] " + r.content().substring(0, Math.min(80, r.content().length())) + "..."));

        // Consulta 2: Buscar por "derivada de sen(x^2)"
        List<NoteChunk> results2 = ragRetriever.retrieve("derivada de sen(x^2)", calculusModule.getCode(), derivativesTopic.getCode());
        assertThat(results2).as("Debe encontrar la solución con la derivada").isNotEmpty();
        assertThat(results2.stream().anyMatch(c -> c.content().toLowerCase().contains("cos(x^2)") || c.content().toLowerCase().contains("2xcos")))
            .as("Debe contener la derivada correcta").isTrue();
        System.out.println("Consulta 'derivada de sen(x^2)': " + results2.size() + " resultados");

        // Consulta 3: Buscar por "error típico olvidar multiplicar"
        List<NoteChunk> results3 = ragRetriever.retrieve("error típico olvidar multiplicar derivada interna", calculusModule.getCode(), derivativesTopic.getCode());
        assertThat(results3).as("Debe encontrar el error típico").isNotEmpty();
        assertThat(results3.stream().anyMatch(c -> "ERROR_TIPICO".equals(c.type())))
            .as("Debe ser de tipo ERROR_TIPICO").isTrue();
        System.out.println("Consulta 'error típico': " + results3.size() + " resultados");

        // Consulta 4: Buscar por "concepto clave regla cadena"
        List<NoteChunk> results4 = ragRetriever.retrieve("concepto clave regla cadena", calculusModule.getCode(), derivativesTopic.getCode());
        assertThat(results4).as("Debe encontrar el concepto clave").isNotEmpty();
        assertThat(results4.stream().anyMatch(c -> "CONCEPTO_CLAVE".equals(c.type())))
            .as("Debe ser de tipo CONCEPTO_CLAVE").isTrue();
        System.out.println("Consulta 'concepto clave': " + results4.size() + " resultados");

        // ============================================================
        // PASO 6: Verificar que búsquedas fuera de dominio NO retornan resultados (umbral 0.7)
        // ============================================================
        System.out.println("\n=== BÚSQUEDAS FUERA DE DOMINIO (deben retornar lista vacía) ===");

        List<NoteChunk> outOfDomain = ragRetriever.retrieve("receta de cocina paella valenciana", calculusModule.getCode(), derivativesTopic.getCode());
        assertThat(outOfDomain).as("Consulta fuera de dominio no debe retornar resultados con umbral 0.7").isEmpty();
        System.out.println("Consulta 'receta de cocina': " + outOfDomain.size() + " resultados (esperado: 0)");

        List<NoteChunk> outOfDomain2 = ragRetriever.retrieve("historia de roma antigua", calculusModule.getCode(), derivativesTopic.getCode());
        assertThat(outOfDomain2).as("Consulta fuera de dominio no debe retornar resultados con umbral 0.7").isEmpty();
        System.out.println("Consulta 'historia de roma': " + outOfDomain2.size() + " resultados (esperado: 0)");

        System.out.println("\n=== TEST E2E COMPLETADO EXITOSAMENTE ===");
    }

    /**
     * Test adicional: extracción de fórmulas y búsqueda por fórmula matemática
     */
    @Test
    void e2e_formulaExtractionAndSearch_works() {
        String texWithFormulas = """
                \\bloque{Integrales}
                \\tema{Integral definida}
                \\dificultad{3}

                \\begin{ejercicio}
                Calcule $\\int_0^1 x^2 \\, dx$ y $\\int_0^\\pi \\sin(x) \\, dx$.
                \\end{ejercicio}

                \\begin{solucion}
                $$\\int_0^1 x^2 dx = \\left[\\frac{x^3}{3}\\right]_0^1 = \\frac{1}{3}$$
                $$\\int_0^\\pi \\sin(x) dx = \\left[-\\cos(x)\\right]_0^\\pi = 2$$
                \\end{solucion}
                """;

        LatexContentExtractor.ExtractionResult extraction = latexExtractor.extract(texWithFormulas);

        assertThat(extraction.hasErrors()).isFalse();
        assertThat(extraction.getFormulas()).hasSizeGreaterThanOrEqualTo(4); // 2 inline + 2 display

        // Indexar solución
        List<com.mathtutor.persistence.entity.NoteChunk> chunks = List.of(
            createChunk(join(extraction.getEnvironments().get("solucion")), "SOLUCION", "integrales_solucion.tex")
        );
        springAiRetriever.indexDocuments(chunks);

        // Buscar por fórmula integral x^2
        List<NoteChunk> results = ragRetriever.retrieve("integral de x al cuadrado de 0 a 1", calculusModule.getCode(), derivativesTopic.getCode());
        assertThat(results).isNotEmpty();
        // El contenido usa LaTeX \frac{1}{3}, buscar la representación LaTeX
        assertThat(results.get(0).content()).contains("frac{1}{3}");
        System.out.println("Búsqueda por fórmula: " + results.size() + " resultados");
    }

    private com.mathtutor.persistence.entity.NoteChunk createChunk(String content, String type, String sourceFile) {
        com.mathtutor.persistence.entity.NoteChunk chunk = new com.mathtutor.persistence.entity.NoteChunk();
        chunk.setContent(content);
        chunk.setType(type);
        chunk.setSourceFile(sourceFile);
        chunk.setModuleCode(calculusModule.getCode());
        chunk.setTopicCode(derivativesTopic.getCode());
        chunk.setVersion("1");
        chunk.setModule(calculusModule);
        chunk.setTopic(derivativesTopic);
        return chunk;
    }

    private String join(List<String> list) {
        if (list == null || list.isEmpty()) return "";
        return String.join("\n", list);
    }
}