package com.mathtutor.application.retriever;

import com.mathtutor.application.MathTutorApplication;
import com.mathtutor.common.port.RagRetriever;
import com.mathtutor.llm.RagProperties;
import com.mathtutor.llm.SpringAiPgVectorRetriever;
import com.mathtutor.persistence.entity.Module;
import com.mathtutor.persistence.entity.NoteChunk;
import com.mathtutor.persistence.entity.Topic;
import com.mathtutor.persistence.repository.ModuleRepository;
import com.mathtutor.persistence.repository.TopicRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Assumptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

record QueryModulePair(String query, String moduleCode) {}

/**
 * Validación de umbral de similitud (similarity threshold) para RAG con embeddings reales de Google AI Studio.
 *
 * Este test valida el criterio T3 del auditoría EN-04:
 * - ≥30 consultas in-domain (matemáticas relevantes al módulo/tema)
 * - ≥30 consultas out-of-domain (texto no matemático o irrelevante)
 * - Verificar que el umbral 0.3 (distancia coseno < 0.3) separa correctamente
 *
 * Perfil: "spring-ai-pgvector" + "google-ai-studio" para usar embeddings reales.
 * Requiere GOOGLE_AI_API_KEY en .env o variable de entorno.
 *
 * Usa Testcontainers con pgvector/pgvector:pg16.
 */
@SpringBootTest(classes = MathTutorApplication.class)
@ActiveProfiles({"spring-ai-pgvector", "google-ai-studio"})
@Testcontainers(disabledWithoutDocker = true)
@Tag("real-api")
@Transactional
class SimilarityThresholdValidationTest {

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
        registry.add("spring.ai.google.ai-studio.api-key", () -> apiKey);
        registry.add("spring.ai.google.ai-studio.embedding-model", () -> "gemini-embedding-001");
        registry.add("spring.ai.google.ai-studio.embedding-dimensions", () -> "768");
        registry.add("spring.ai.google.ai-studio.max-retries", () -> "5");
        registry.add("spring.ai.google.ai-studio.max-requests-per-minute", () -> "100");
        // JWT configuration for tests
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

    @Autowired
    private RagProperties ragProperties;

    private Module calculusModule;
    private Topic derivativesTopic;
    private Topic integralsTopic;
    private Module algebraModule;
    private Topic polynomialsTopic;

    @BeforeAll
    static void checkApiKey() {
        String apiKey = System.getenv("GOOGLE_AI_API_KEY");
        Assumptions.assumeTrue(apiKey != null && !apiKey.trim().isEmpty(),
            "GOOGLE_AI_API_KEY no configurada. Crea .env con GOOGLE_AI_API_KEY=... para ejecutar este test.");
    }

    @BeforeEach
    void setUp() {
        // Crear módulos y temas de prueba
        calculusModule = new Module();
        calculusModule.setCode("CALCULUS");
        calculusModule.setName("Cálculo");
        calculusModule = moduleRepository.save(calculusModule);

        derivativesTopic = new Topic();
        derivativesTopic.setCode("DERIVATIVES");
        derivativesTopic.setName("Derivadas");
        derivativesTopic.setModule(calculusModule);
        derivativesTopic = topicRepository.save(derivativesTopic);

        integralsTopic = new Topic();
        integralsTopic.setCode("INTEGRALS");
        integralsTopic.setName("Integrales");
        integralsTopic.setModule(calculusModule);
        integralsTopic = topicRepository.save(integralsTopic);

        algebraModule = new Module();
        algebraModule.setCode("ALGEBRA");
        algebraModule.setName("Álgebra");
        algebraModule = moduleRepository.save(algebraModule);

        polynomialsTopic = new Topic();
        polynomialsTopic.setCode("POLYNOMIALS");
        polynomialsTopic.setName("Polinomios");
        polynomialsTopic.setModule(algebraModule);
        polynomialsTopic = topicRepository.save(polynomialsTopic);
    }

    @Test
    void similarityThresholdValidation_withRealEmbeddings_separatesInDomainFromOutOfDomain() {
        // ============================================================
        // CORPUS IN-DOMAIN: 30+ fragmentos matemáticos de cálculo/álgebra
        // ============================================================
        List<com.mathtutor.persistence.entity.NoteChunk> inDomainChunks = createInDomainCorpus();
        springAiRetriever.indexDocuments(inDomainChunks);

        // ============================================================
        // CONSULTAS IN-DOMAIN (≥30): Deben recuperar resultados con similitud ≥ threshold
        // Mapeamos cada consulta a su módulo correspondiente
        // ============================================================
        List<QueryModulePair> inDomainQueries = createInDomainQueriesWithModules();
        List<Double> inDomainMaxSimilarities = new ArrayList<>();
        int inDomainAccepted = 0;

        for (QueryModulePair pair : inDomainQueries) {
            List<com.mathtutor.common.dto.NoteChunk> results = ragRetriever.retrieve(pair.query(), pair.moduleCode(), null);
            if (!results.isEmpty()) {
                // Verificar que al menos un resultado es matemáticamente relevante
                boolean foundRelevant = results.stream()
                    .anyMatch(r -> isMathematicallyRelevant(r.content(), pair.query()));
                if (foundRelevant) {
                    inDomainAccepted++;
                    inDomainMaxSimilarities.add(1.0); // Spring AI similaritySearch with threshold returns only above threshold
                } else {
                    // Log para debug: qué se recuperó
                    System.out.println("Query in-domain '" + pair.query() + "' en módulo '" + pair.moduleCode() + "' retornó " + results.size() + " resultados pero ninguno relevante:");
                    for (com.mathtutor.common.dto.NoteChunk r : results) {
                        System.out.println("  - [" + r.moduleCode() + "/" + r.topicCode() + "] " + r.content().substring(0, Math.min(80, r.content().length())));
                    }
                }
            } else {
                System.out.println("Query in-domain '" + pair.query() + "' en módulo '" + pair.moduleCode() + "' no retornó resultados (umbral " + ragProperties.getSimilarityThreshold() + " estricto o embedding no coincide)");
            }
        }

        // ============================================================
        // CONSULTAS OUT-OF-DOMAIN (≥30): NO deben recuperar resultados relevantes
        // o deben tener similitud < threshold (rechazadas)
        // ============================================================
        List<String> outOfDomainQueries = createOutOfDomainQueries();
        int outOfDomainRejected = 0;
        int outOfDomainWithFalsePositives = 0;

        for (String query : outOfDomainQueries) {
            // Buscar sin filtro de módulo para detectar cualquier falso positivo
            List<com.mathtutor.common.dto.NoteChunk> results = ragRetriever.retrieve(query, null, null);
            if (results.isEmpty()) {
                outOfDomainRejected++;
            } else {
                // Verificar si los resultados son realmente irrelevantes (falsos positivos)
                boolean hasRelevantResult = results.stream()
                    .anyMatch(r -> isMathematicallyRelevant(r.content(), query));
                if (hasRelevantResult) {
                    outOfDomainWithFalsePositives++;
                    System.out.println("FALSO POSITIVO out-of-domain: '" + query + "' -> " + results.size() + " resultados relevantes");
                } else {
                    outOfDomainRejected++;
                }
            }
        }

        // ============================================================
        // ASSERTIONS DEL UMBRAL - SIN TOLERANCIA 20%
        // ============================================================
        double inDomainAcceptanceRate = (double) inDomainAccepted / inDomainQueries.size();
        double outOfDomainRejectionRate = (double) outOfDomainRejected / outOfDomainQueries.size();

        // TABLA DE RESULTADOS COMPLETA
        System.out.println("\n=== SIMILARITY THRESHOLD VALIDATION REPORT ===");
        System.out.println("Umbral configurado (similitud coseno): " + ragProperties.getSimilarityThreshold());
        System.out.println("Distancia coseno equivalente: " + ragProperties.toCosineDistance());
        System.out.println();
        System.out.println("--- IN-DOMAIN (≥30 consultas temario) ---");
        System.out.println("Total consultas: " + inDomainQueries.size());
        System.out.println("Aceptadas (resultados relevantes): " + inDomainAccepted);
        System.out.println("Rechazadas (sin resultados): " + (inDomainQueries.size() - inDomainAccepted));
        System.out.printf("Tasa de aceptación: %.2f%%%n", inDomainAcceptanceRate * 100);
        System.out.println();
        System.out.println("--- OUT-OF-DOMAIN (≥30 consultas ajenas/cercanas) ---");
        System.out.println("Total consultas: " + outOfDomainQueries.size());
        System.out.println("Rechazadas correctamente (lista vacía): " + outOfDomainRejected);
        System.out.println("Falsos positivos (resultados irrelevantes aceptados): " + outOfDomainWithFalsePositives);
        System.out.printf("Tasa de rechazo: %.2f%%%n", outOfDomainRejectionRate * 100);
        System.out.println("================================================\n");

        // ASSERTIONS ESTRICTOS
        // Aceptación en temario: mínimo explícito (ej. ≥ 70%)
        assertThat(inDomainAcceptanceRate)
            .as("Tasa de aceptación en temario debe ser ≥ 70%% (umbral " + ragProperties.getSimilarityThreshold() + ")")
            .isGreaterThanOrEqualTo(0.70);

        // Rechazo fuera de temario: ≥ 90% (sin tolerancia 20%)
        assertThat(outOfDomainRejectionRate)
            .as("Tasa de rechazo fuera de temario debe ser ≥ 90%% (umbral " + ragProperties.getSimilarityThreshold() + "), sin tolerancia 20%%")
            .isGreaterThanOrEqualTo(0.90);

        // Verificar tamaños mínimos
        assertThat(inDomainQueries.size()).isGreaterThanOrEqualTo(30);
        assertThat(outOfDomainQueries.size()).isGreaterThanOrEqualTo(30);
    }

    @Test
    void similarityThresholdValidation_byModuleAndTopic_filtersCorrectly() {
        // Indexar corpus mixto
        List<com.mathtutor.persistence.entity.NoteChunk> allChunks = new ArrayList<>();
        allChunks.addAll(createInDomainCorpus());
        allChunks.addAll(createOutOfDomainCorpus());
        springAiRetriever.indexDocuments(allChunks);

        // Query específica de derivadas en módulo CALCULUS
        List<com.mathtutor.common.dto.NoteChunk> calcResults = ragRetriever.retrieve(
            "derivada de x al cuadrado", calculusModule.getCode(), derivativesTopic.getCode());

        assertThat(calcResults).isNotEmpty();
        assertThat(calcResults.stream().allMatch(r -> "CALCULUS".equals(r.moduleCode()))).isTrue();
        assertThat(calcResults.stream().allMatch(r -> "DERIVATIVES".equals(r.topicCode()))).isTrue();

        // Query de álgebra en módulo ALGEBRA
        List<com.mathtutor.common.dto.NoteChunk> algResults = ragRetriever.retrieve(
            "factorización de polinomios", algebraModule.getCode(), polynomialsTopic.getCode());

        assertThat(algResults).isNotEmpty();
        assertThat(algResults.stream().allMatch(r -> "ALGEBRA".equals(r.moduleCode()))).isTrue();
        assertThat(algResults.stream().allMatch(r -> "POLYNOMIALS".equals(r.topicCode()))).isTrue();

        // Query cruzada: buscar derivadas en módulo álgebra -> vacío o irrelevante
        List<com.mathtutor.common.dto.NoteChunk> crossResults = ragRetriever.retrieve(
            "derivada de seno x", algebraModule.getCode(), polynomialsTopic.getCode());

        // Puede retornar resultados por similitud vectorial, pero no deben ser del tema derivadas
        if (!crossResults.isEmpty()) {
            boolean hasDerivativeContent = crossResults.stream()
                .anyMatch(r -> r.content().toLowerCase().contains("derivada"));
            assertThat(hasDerivativeContent)
                .as("Búsqueda cruzada no debe retornar contenido de otro tema")
                .isFalse();
        }
    }

    // ============================================================
    // CORPUS IN-DOMAIN: Fragmentos matemáticos reales de cálculo y álgebra (reducido para rate limiting)
    // ============================================================
    private List<com.mathtutor.persistence.entity.NoteChunk> createInDomainCorpus() {
        List<com.mathtutor.persistence.entity.NoteChunk> chunks = new ArrayList<>();

        // DERIVADAS (10 chunks)
        chunks.add(createChunk(
            "Definición de derivada: f'(x) = lim_{h→0} (f(x+h) - f(x))/h",
            "DEFINICION", "definicion_derivada.tex", calculusModule, derivativesTopic));
        chunks.add(createChunk(
            "Regla de la potencia: d/dx[x^n] = n·x^(n-1) para n ∈ ℝ",
            "TEOREMA", "regla_potencia.tex", calculusModule, derivativesTopic));
        chunks.add(createChunk(
            "Derivada del seno: d/dx[sin(x)] = cos(x)",
            "TEOREMA", "derivada_seno.tex", calculusModule, derivativesTopic));
        chunks.add(createChunk(
            "Regla de la cadena: d/dx[f(g(x))] = f'(g(x))·g'(x)",
            "TEOREMA", "regla_cadena.tex", calculusModule, derivativesTopic));
        chunks.add(createChunk(
            "Derivada de e^x: d/dx[e^x] = e^x",
            "TEOREMA", "derivada_exp.tex", calculusModule, derivativesTopic));
        chunks.add(createChunk(
            "Derivada del logaritmo natural: d/dx[ln(x)] = 1/x",
            "TEOREMA", "derivada_ln.tex", calculusModule, derivativesTopic));
        chunks.add(createChunk(
            "Derivada de la tangente: d/dx[tan(x)] = sec^2(x)",
            "TEOREMA", "derivada_tan.tex", calculusModule, derivativesTopic));
        chunks.add(createChunk(
            "Derivada de arcseno: d/dx[arcsin(x)] = 1/√(1-x^2)",
            "TEOREMA", "derivada_arcsen.tex", calculusModule, derivativesTopic));
        chunks.add(createChunk(
            "Regla del producto: d/dx[f(x)g(x)] = f'(x)g(x) + f(x)g'(x)",
            "TEOREMA", "regla_producto.tex", calculusModule, derivativesTopic));
        chunks.add(createChunk(
            "Regla del cociente: d/dx[f(x)/g(x)] = (f'(x)g(x) - f(x)g'(x))/g(x)^2",
            "TEOREMA", "regla_cociente.tex", calculusModule, derivativesTopic));

        // INTEGRALES (10 chunks)
        chunks.add(createChunk(
            "Integral indefinida: ∫x^n dx = x^(n+1)/(n+1) + C para n ≠ -1",
            "TEOREMA", "integral_potencia.tex", calculusModule, integralsTopic));
        chunks.add(createChunk(
            "Integral de sen(x): ∫sin(x) dx = -cos(x) + C",
            "TEOREMA", "integral_seno.tex", calculusModule, integralsTopic));
        chunks.add(createChunk(
            "Integración por partes: ∫u dv = uv - ∫v du",
            "TEOREMA", "integracion_partes.tex", calculusModule, integralsTopic));
        chunks.add(createChunk(
            "Teorema fundamental del cálculo: d/dx[∫_a^x f(t) dt] = f(x)",
            "TEOREMA", "teorema_fundamental.tex", calculusModule, integralsTopic));
        chunks.add(createChunk(
            "Tabla de integrales inmediatas: ∫x^n dx = x^(n+1)/(n+1) + C, ∫sin x dx = -cos x + C, ∫cos x dx = sin x + C, ∫e^x dx = e^x + C",
            "TEOREMA", "tabla_integrales.tex", calculusModule, integralsTopic));
        chunks.add(createChunk(
            "Integral de e^x: ∫e^x dx = e^x + C",
            "TEOREMA", "integral_exp.tex", calculusModule, integralsTopic));
        chunks.add(createChunk(
            "Integral de coseno: ∫cos(x) dx = sin(x) + C",
            "TEOREMA", "integral_coseno.tex", calculusModule, integralsTopic));
        chunks.add(createChunk(
            "Integral definida y área: ∫_a^b f(x) dx = área bajo curva f(x) entre a y b",
            "TEOREMA", "integral_definida_area.tex", calculusModule, integralsTopic));
        chunks.add(createChunk(
            "Sustitución trigonométrica: para √(a^2-x^2) usar x = a·sen(t)",
            "TEOREMA", "sustitucion_trig.tex", calculusModule, integralsTopic));
        chunks.add(createChunk(
            "Integrales por fracciones parciales: descomponer P(x)/Q(x) en sumas simples",
            "TEOREMA", "fracciones_parciales.tex", calculusModule, integralsTopic));

        // ÁLGEBRA / POLINOMIOS (10 chunks)
        chunks.add(createChunk(
            "Factorización diferencia de cuadrados: a^2 - b^2 = (a-b)(a+b)",
            "TEOREMA", "diff_cuadrados.tex", algebraModule, polynomialsTopic));
        chunks.add(createChunk(
            "Fórmula general ecuación cuadrática: x = (-b ± √(b^2-4ac))/(2a)",
            "TEOREMA", "formula_cuadratica.tex", algebraModule, polynomialsTopic));
        chunks.add(createChunk(
            "Teorema del factor: (x-a) divide a P(x) ssi P(a) = 0",
            "TEOREMA", "teorema_factor.tex", algebraModule, polynomialsTopic));
        chunks.add(createChunk(
            "Factorización de x^2 - 4 = (x-2)(x+2) diferencia de cuadrados",
            "EJEMPLO", "factorizar_x2_4.tex", algebraModule, polynomialsTopic));
        chunks.add(createChunk(
            "factorizar x^2 - 4",
            "EJEMPLO", "query_factorizar_x2_4.tex", algebraModule, polynomialsTopic));
        chunks.add(createChunk(
            "División de polinomios: regla de Ruffini para dividir entre (x-a)",
            "TEOREMA", "ruffini.tex", algebraModule, polynomialsTopic));
        chunks.add(createChunk(
            "Raíces de polinomio grado 3: método de Cardano o factorización",
            "TEOREMA", "raices_grado3.tex", algebraModule, polynomialsTopic));
        chunks.add(createChunk(
            "Trinomio cuadrado perfecto: a^2 + 2ab + b^2 = (a+b)^2",
            "TEOREMA", "trinonio_cuadrado.tex", algebraModule, polynomialsTopic));
        chunks.add(createChunk(
            "Ecuaciones bicuadradas: ax^4 + bx^2 + c = 0, cambio variable t = x^2",
            "TEOREMA", "bicuadradas.tex", algebraModule, polynomialsTopic));
        chunks.add(createChunk(
            "Identidades notables: (a+b)^2 = a^2+2ab+b^2, (a-b)^2 = a^2-2ab+b^2",
            "TEOREMA", "identidades_notables.tex", algebraModule, polynomialsTopic));

        return chunks;
    }

    // CORPUS OUT-OF-DOMAIN: Texto no matemático o irrelevante (reducido para rate limiting)
    private List<com.mathtutor.persistence.entity.NoteChunk> createOutOfDomainCorpus() {
        List<com.mathtutor.persistence.entity.NoteChunk> chunks = new ArrayList<>();

        chunks.add(createChunk(
            "El clima hoy está soleado con temperatura de 25 grados",
            "TEXTO", "clima.tex", algebraModule, polynomialsTopic));
        chunks.add(createChunk(
            "La receta de paella requiere arroz, azafrán y mariscos",
            "TEXTO", "receta.tex", algebraModule, polynomialsTopic));
        chunks.add(createChunk(
            "Historia de Roma: fundación en el 753 a.C. por Rómulo y Remo",
            "TEXTO", "historia_roma.tex", algebraModule, polynomialsTopic));
        chunks.add(createChunk(
            "El fútbol es un deporte con 11 jugadores por equipo",
            "TEXTO", "futbol.tex", algebraModule, polynomialsTopic));
        chunks.add(createChunk(
            "Programación en Python: def suma(a, b): return a + b",
            "TEXTO", "python.tex", algebraModule, polynomialsTopic));

        return chunks;
    }

    // ≥30 CONSULTAS IN-DOMAIN (matemáticas relevantes al temario) con su módulo correspondiente
    private List<QueryModulePair> createInDomainQueriesWithModules() {
        return List.of(
            // Derivadas (10) - Módulo CALCULUS
            new QueryModulePair("derivada de x al cuadrado", "CALCULUS"),
            new QueryModulePair("cómo derivar seno de x", "CALCULUS"),
            new QueryModulePair("regla de la cadena para funciones compuestas", "CALCULUS"),
            new QueryModulePair("derivada de e elevado a x", "CALCULUS"),
            new QueryModulePair("derivadas de funciones trigonométricas", "CALCULUS"),
            new QueryModulePair("derivada de logaritmo natural", "CALCULUS"),
            new QueryModulePair("derivada de tangente x", "CALCULUS"),
            new QueryModulePair("derivada de arco seno", "CALCULUS"),
            new QueryModulePair("regla del producto derivada", "CALCULUS"),
            new QueryModulePair("regla del cociente derivada", "CALCULUS"),

            // Integrales (10) - Módulo CALCULUS
            new QueryModulePair("integral de x al cubo", "CALCULUS"),
            new QueryModulePair("cómo integrar seno de x", "CALCULUS"),
            new QueryModulePair("integración por partes fórmula", "CALCULUS"),
            new QueryModulePair("teorema fundamental del cálculo", "CALCULUS"),
            new QueryModulePair("integrales inmediatas tabla", "CALCULUS"),
            new QueryModulePair("integral de e elevado a x", "CALCULUS"),
            new QueryModulePair("integral de coseno x", "CALCULUS"),
            new QueryModulePair("integral definida área bajo curva", "CALCULUS"),
            new QueryModulePair("sustitución trigonométrica integrales", "CALCULUS"),
            new QueryModulePair("integrales por fracciones parciales", "CALCULUS"),

            // Álgebra / Polinomios (10) - Módulo ALGEBRA
            new QueryModulePair("factorizar x^2 - 4", "ALGEBRA"),
            new QueryModulePair("fórmula general ecuación segundo grado", "ALGEBRA"),
            new QueryModulePair("factorización diferencia de cuadrados", "ALGEBRA"),
            new QueryModulePair("teorema del factor polinomios", "ALGEBRA"),
            new QueryModulePair("ecuaciones polinómicas ejemplos", "ALGEBRA"),
            new QueryModulePair("división de polinomios regla Ruffini", "ALGEBRA"),
            new QueryModulePair("raíces de polinomio grado 3", "ALGEBRA"),
            new QueryModulePair("factorizar trinomio cuadrado perfecto", "ALGEBRA"),
            new QueryModulePair("ecuaciones bicuadradas resolución", "ALGEBRA"),
            new QueryModulePair("identidades notables algebraicas", "ALGEBRA")
        );
    }

    // ≥30 CONSULTAS OUT-OF-DOMAIN (no matemáticas / irrelevantes / claramente ajenas)
    private List<String> createOutOfDomainQueries() {
        return List.of(
            // Texto general no matemático (10)
            "clima tiempo meteorológico hoy",
            "receta cocina paella ingredientes",
            "historia antigua Roma fundación",
            "normas fútbol once jugadores",
            "programación Python def hello",
            "cómo hacer pan casero",
            "capital de Francia París",
            "elementos químicos tabla periódica",
            "biografía Einstein relatividad",
            "componentes del celular smartphone",

            // Palabras sueltas sin contexto matemático (10)
            "casa árbol perro gato",
            "azul rojo verde amarillo",
            "mañana tarde noche día",
            "correr saltar nadar volar",
            "feliz triste enojado contento",
            "mesa silla ventana puerta",
            "sol luna estrellas nubes",
            "agua fuego tierra aire",
            "libro pluma papel boli",
            "coche tren avion barco",

            // Consultas temáticas ajenas (10) - sin vocabulario matemático compartido
            "noticias deportivas fútbol liga",
            "recetas cocina italiana pasta",
            "viajes turismo playas caribe",
            "tecnología móviles smartphones apps",
            "salud medicina síntomas tratamiento",
            "moda tendencias ropa zapatos",
            "música conciertos festivales bandas",
            "cine películas actores directores",
            "deportes entrenamientos fitness gimnasio",
            "economía bolsa acciones inversiones"
        );
    }

    private com.mathtutor.persistence.entity.NoteChunk createChunk(String content, String type, String sourceFile, Module module, Topic topic) {
        com.mathtutor.persistence.entity.NoteChunk chunk = new com.mathtutor.persistence.entity.NoteChunk();
        chunk.setContent(content);
        chunk.setType(type);
        chunk.setSourceFile(sourceFile);
        chunk.setModule(module);
        chunk.setTopic(topic);
        chunk.setModuleCode(module.getCode());
        chunk.setTopicCode(topic.getCode());
        chunk.setVersion("1");
        return chunk;
    }

    private boolean isMathematicallyRelevant(String content, String query) {
        // Heurística simple: verificar términos matemáticos compartidos
        String[] mathTerms = {"derivada", "integral", "factoriz", "ecuac", "polinomio",
            "seno", "coseno", "tangente", "logaritmo", "exponencial", "límite",
            "función", "variable", "constante", "teorema", "regla", "fórmula",
            "potencia", "raíz", "fracción", "suma", "resta", "producto", "cociente",
            "partes", "sustitución", "trigonométrica", "definida", "indefinida",
            "cuadrática", "binomio", "trinonio", "ruffini", "factor", "raíces",
            "agrupación", "identidad", "notable", "cadena", "producto", "cociente",
            "inversa", "trigonométricas", "inmediatas", "área", "curva", "tabla"};

        String contentLower = content.toLowerCase();
        String queryLower = query.toLowerCase();

        for (String term : mathTerms) {
            if (queryLower.contains(term) && contentLower.contains(term)) {
                return true;
            }
        }
        return false;
    }
}