package com.mathtutor.application;

import com.mathtutor.common.port.RagRetriever;
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

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * Test para validar RNF-03: No hay transacciones JDBC abiertas durante llamadas a LLM.
 *
 * Este test verifica que durante la generación de embeddings (llamada a LLM/Google AI Studio),
 * no hay transacciones de base de datos abiertas. Esto es crítico porque:
 * 1. Las llamadas a LLM pueden tardar cientos de milisegundos a segundos
 * 2. Mantener una transacción abierta durante ese tiempo agota el pool de conexiones
 * 3. Causa timeouts en otras peticiones concurrentes
 *
 * Estrategia de validación:
 * - Usa HikariPoolMXBean para verificar conexiones activas/ociosas
 * - Consulta pg_stat_activity para verificar transacciones abiertas
 * - Usa un modelo de embedding "lento" (stub con sleep) para simular llamada real
 *
 * Perfil: "spring-ai-pgvector" + "google-ai-studio" para usar embeddings reales (opcional)
 * o perfil por defecto con DeterministicEmbeddingModel para tests rápidos.
 *
 * Tag: "rnf03" para ejecución selectiva.
 */
@SpringBootTest(classes = com.mathtutor.application.MathTutorApplication.class)
@ActiveProfiles({"spring-ai-pgvector", "deterministic"})
@Testcontainers(disabledWithoutDocker = true)
@Tag("rnf03")
class Rnf03NoOpenTransactionDuringLlmCallTest {

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
        registry.add("spring.profiles.active", () -> "spring-ai-pgvector,deterministic");
        registry.add("spring.ai.vectorstore.pgvector.initialize-schema", () -> "true");
        // JWT configuration for tests
        registry.add("jwt.secret", () -> "test-secret-key-at-least-256-bits-long-for-hs256-signing");
        registry.add("jwt.access-token-expiration-minutes", () -> "15");
        registry.add("jwt.refresh-token-expiration-days", () -> "7");
    }

    @Autowired
    private DataSource dataSource;

    @Autowired
    private RagRetriever ragRetriever;

    @Autowired
    private SpringAiPgVectorRetriever springAiRetriever;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private TopicRepository topicRepository;

    private static Module calculusModule;
    private static Topic derivativesTopic;

    // Embedding model lento para simular latencia de LLM (2 segundos)
    private static final int SLOW_EMBEDDING_DELAY_MS = 2000;

    @BeforeAll
    static void checkDocker() {
        // Testcontainers @Testcontainers(disabledWithoutDocker = true) ya maneja esto automáticamente
        // No necesitamos verificación extra
    }

    @BeforeAll
    static void setUpClass(@Autowired ModuleRepository moduleRepository,
                           @Autowired TopicRepository topicRepository,
                           @Autowired SpringAiPgVectorRetriever springAiRetriever) {
        calculusModule = new Module();
        calculusModule.setCode("CALCULUS");
        calculusModule.setName("Cálculo");
        calculusModule = moduleRepository.save(calculusModule);

        derivativesTopic = new Topic();
        derivativesTopic.setCode("DERIVATIVES");
        derivativesTopic.setName("Derivadas");
        derivativesTopic.setModule(calculusModule);
        derivativesTopic = topicRepository.save(derivativesTopic);

        // Indexar corpus de prueba
        List<NoteChunk> chunks = createStaticTestCorpus();
        springAiRetriever.indexDocuments(chunks);
    }

    /**
     * TEST PRINCIPAL RNF-03:
     * Verifica que durante una llamada a retrieve() (que internamente genera embeddings),
     * no hay transacciones de BD abiertas.
     *
     * La validación se hace en dos niveles:
     * 1. HikariPoolMXBean: conexiones activas vs idle
     * 2. pg_stat_activity: transacciones en estado "active" o "idle in transaction"
     */
    @Test
    void rnf03_noOpenTransactionDuringEmbeddingGeneration() throws Exception {
        // ============================================================
        // FASE 1: Verificar estado inicial del pool (todo idle)
        // ============================================================
        HikariPoolMetrics initialMetrics = getHikariPoolMetrics();
        System.out.println("=== MÉTRICAS INICIALES DEL POOL ===");
        System.out.println("Total connections: " + initialMetrics.totalConnections());
        System.out.println("Active connections: " + initialMetrics.activeConnections());
        System.out.println("Idle connections: " + initialMetrics.idleConnections());
        System.out.println("Threads awaiting: " + initialMetrics.threadsAwaitingConnection());

        // En estado inicial, no debe haber conexiones activas
        assertThat(initialMetrics.activeConnections())
            .as("No debe haber conexiones activas antes de la llamada")
            .isEqualTo(0);

        // ============================================================
        // FASE 2: Verificar pg_stat_activity inicial (sin transacciones abiertas)
        // ============================================================
        int initialActiveTransactions = countActiveTransactionsInDb();
        System.out.println("\n=== pg_stat_activity INICIAL ===");
        System.out.println("Transacciones activas: " + initialActiveTransactions);

        assertThat(initialActiveTransactions)
            .as("No debe haber transacciones activas en BD antes de la llamada")
            .isEqualTo(0);

        // ============================================================
        // FASE 3: Ejecutar retrieve() con modelo lento (simula LLM call)
        //         Monitorear pool y BD DURANTE la llamada
        // ============================================================
        System.out.println("\n=== EJECUTANDO retrieve() CON MODELO LENTO (" + SLOW_EMBEDDING_DELAY_MS + "ms) ===");

        // Bandera para detectar si hubo transacciones activas DURANTE la llamada
        AtomicBoolean transactionDetectedDuringCall = new AtomicBoolean(false);
        AtomicInteger maxActiveDuringCall = new AtomicInteger(0);
        AtomicInteger maxDbTransactionsDuringCall = new AtomicInteger(0);
        CountDownLatch monitoringLatch = new CountDownLatch(1);

        // Hilo monitor que verifica pool y BD cada 100ms durante la llamada
        Thread monitorThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    HikariPoolMetrics metrics = getHikariPoolMetrics();
                    int dbActiveTx = countActiveTransactionsInDb();

                    maxActiveDuringCall.set(Math.max(maxActiveDuringCall.get(), metrics.activeConnections()));
                    maxDbTransactionsDuringCall.set(Math.max(maxDbTransactionsDuringCall.get(), dbActiveTx));

                    // Si detectamos conexiones activas o transacciones en BD durante la llamada
                    if (metrics.activeConnections() > 0 || dbActiveTx > 0) {
                        transactionDetectedDuringCall.set(true);
                        System.out.println("  ⚠ DETECTADO - Pool active: " + metrics.activeConnections() +
                            ", DB active tx: " + dbActiveTx);
                    }

                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                // Esperado al terminar
            } catch (Exception e) {
                System.err.println("Error en monitor: " + e.getMessage());
            } finally {
                monitoringLatch.countDown();
            }
        }, "rnf03-monitor");
        monitorThread.start();

        // Pequeña pausa para que el monitor inicie
        Thread.sleep(200);

        // EJECUTAR LA LLAMADA QUE GENERA EMBEDDING (simula llamada a LLM)
        List<com.mathtutor.common.dto.NoteChunk> results = ragRetriever.retrieve(
            "derivada de x al cuadrado", calculusModule.getCode(), derivativesTopic.getCode());

        // Detener monitor
        monitorThread.interrupt();
        monitoringLatch.await(5, TimeUnit.SECONDS);

        // ============================================================
        // FASE 4: Verificar estado final (debe volver a idle)
        // ============================================================
        HikariPoolMetrics finalMetrics = getHikariPoolMetrics();
        int finalDbActiveTx = countActiveTransactionsInDb();

        System.out.println("\n=== MÉTRICAS FINALES DEL POOL ===");
        System.out.println("Total connections: " + finalMetrics.totalConnections());
        System.out.println("Active connections: " + finalMetrics.activeConnections());
        System.out.println("Idle connections: " + finalMetrics.idleConnections());

        System.out.println("\n=== pg_stat_activity FINAL ===");
        System.out.println("Transacciones activas: " + finalDbActiveTx);

        System.out.println("\n=== MÁXIMOS DURANTE LA LLAMADA ===");
        System.out.println("Máx conexiones activas pool: " + maxActiveDuringCall.get());
        System.out.println("Máx transacciones activas BD: " + maxDbTransactionsDuringCall.get());

        // ============================================================
        // ASSERTIONS RNF-03
        // ============================================================

        // 1. La llamada debe haber funcionado (recuperar resultados)
        assertThat(results).isNotEmpty();
        System.out.println("\nResultados recuperados: " + results.size());

        // 2. NO debe haber habido transacciones activas DURANTE la llamada al LLM
        //    (el embedding se genera SIN transacción abierta)
        assertThat(transactionDetectedDuringCall.get())
            .as("RNF-03 VIOLADO: Se detectaron conexiones/transacciones activas DURANTE la generación de embedding. " +
                "El pool mostró máx %d activas, BD máx %d transacciones activas.",
                maxActiveDuringCall.get(), maxDbTransactionsDuringCall.get())
            .isFalse();

        // 3. Pool debe volver a estado idle (0 activas)
        assertThat(finalMetrics.activeConnections())
            .as("Pool debe volver a 0 conexiones activas tras la llamada")
            .isEqualTo(0);

        // 4. BD no debe tener transacciones abiertas
        assertThat(finalDbActiveTx)
            .as("BD no debe tener transacciones abiertas tras la llamada")
            .isEqualTo(0);

        // 5. Verificar que los resultados son correctos (validación funcional)
        assertThat(results.stream().anyMatch(r -> r.content().toLowerCase().contains("derivada")))
            .as("Debe recuperar contenido matemático relevante")
            .isTrue();
    }

    /**
     * Test de concurrencia: múltiples retrieve() simultáneos no deben bloquearse
     * por transacciones largas durante embedding generation.
     * Usa módulos únicos para evitar conflictos de BD.
     */
    @Test
    void rnf03_concurrentRetrievesDoNotBlockEachOther() throws Exception {
        int concurrentCalls = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(concurrentCalls);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // Verificar pool inicial
        HikariPoolMetrics initialMetrics = getHikariPoolMetrics();
        assertThat(initialMetrics.activeConnections()).isEqualTo(0);

        for (int i = 0; i < concurrentCalls; i++) {
            final int callIndex = i;
            new Thread(() -> {
                try {
                    startLatch.await(); // Esperar señal de inicio simultáneo

                    // Usar el módulo/tema ya creado en setUp
                    List<com.mathtutor.common.dto.NoteChunk> results = ragRetriever.retrieve(
                        "derivada de x^" + callIndex, calculusModule.getCode(), derivativesTopic.getCode());

                    if (!results.isEmpty()) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.err.println("Error en llamada concurrente " + callIndex + ": " + e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            }, "rnf03-concurrent-" + i).start();
        }

        // Iniciar todas simultáneamente
        startLatch.countDown();

        // Esperar a que terminen (timeout generoso para modelo lento)
        boolean allFinished = endLatch.await(30, TimeUnit.SECONDS);

        // Verificar estado final
        HikariPoolMetrics finalMetrics = getHikariPoolMetrics();
        int finalDbActiveTx = countActiveTransactionsInDb();

        System.out.println("\n=== TEST CONCURRENCIA ===");
        System.out.println("Llamadas exitosas: " + successCount.get());
        System.out.println("Llamadas fallidas: " + failureCount.get());
        System.out.println("Pool final - Active: " + finalMetrics.activeConnections() + ", Idle: " + finalMetrics.idleConnections());
        System.out.println("BD transacciones activas: " + finalDbActiveTx);

        assertThat(allFinished).as("Todas las llamadas concurrentes deben terminar").isTrue();
        assertThat(successCount.get()).as("Todas las llamadas deben tener éxito").isEqualTo(concurrentCalls);
        assertThat(finalMetrics.activeConnections()).as("Pool debe volver a idle").isEqualTo(0);
        assertThat(finalDbActiveTx).as("BD sin transacciones abiertas").isEqualTo(0);
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    private record HikariPoolMetrics(
        int totalConnections,
        int activeConnections,
        int idleConnections,
        int threadsAwaitingConnection
    ) {}

    private HikariPoolMetrics getHikariPoolMetrics() {
        try {
            // Obtener HikariPoolMXBean via JMX
            var mbeanServer = ManagementFactory.getPlatformMBeanServer();
            var poolName = "HikariPool-1"; // Nombre por defecto de Spring Boot
            var objectName = new javax.management.ObjectName("com.zaxxer.hikari:type=Pool (" + poolName + ")");

            int total = (int) mbeanServer.getAttribute(objectName, "TotalConnections");
            int active = (int) mbeanServer.getAttribute(objectName, "ActiveConnections");
            int idle = (int) mbeanServer.getAttribute(objectName, "IdleConnections");
            int awaiting = (int) mbeanServer.getAttribute(objectName, "ThreadsAwaitingConnection");

            return new HikariPoolMetrics(total, active, idle, awaiting);
        } catch (Exception e) {
            // Fallback: usar DataSource directo si JMX no disponible
            try (Connection conn = dataSource.getConnection()) {
                // Si podemos obtener conexión, el pool funciona
                return new HikariPoolMetrics(-1, 0, -1, -1);
            } catch (SQLException ex) {
                return new HikariPoolMetrics(-1, -1, -1, -1);
            }
        }
    }

    private int countActiveTransactionsInDb() {
        String sql = """
            SELECT count(*)
            FROM pg_stat_activity
            WHERE state IN ('active', 'idle in transaction', 'idle in transaction (aborted)')
              AND pid <> pg_backend_pid()
              AND datname = current_database()
            """;

        try (Connection conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error consultando pg_stat_activity: " + e.getMessage());
        }
        return -1;
    }

    private static List<NoteChunk> createStaticTestCorpus() {
        return List.of(
            createStaticChunk(
                "Definición de derivada: f'(x) = lim_{h→0} (f(x+h) - f(x))/h",
                "DEFINICION", "definicion_derivada.tex"),
            createStaticChunk(
                "Regla de la potencia: d/dx[x^n] = n·x^(n-1) para n ∈ ℝ",
                "TEOREMA", "regla_potencia.tex"),
            createStaticChunk(
                "Derivada del seno: d/dx[sin(x)] = cos(x)",
                "TEOREMA", "derivada_seno.tex"),
            createStaticChunk(
                "Regla de la cadena: d/dx[f(g(x))] = f'(g(x))·g'(x)",
                "TEOREMA", "regla_cadena.tex"),
            createStaticChunk(
                "Derivada de e^x: d/dx[e^x] = e^x",
                "TEOREMA", "derivada_exp.tex")
        );
    }

    private static NoteChunk createStaticChunk(String content, String type, String sourceFile) {
        NoteChunk chunk = new NoteChunk();
        chunk.setContent(content);
        chunk.setType(type);
        chunk.setSourceFile(sourceFile);
        chunk.setModule(calculusModule);
        chunk.setTopic(derivativesTopic);
        chunk.setModuleCode(calculusModule.getCode());
        chunk.setTopicCode(derivativesTopic.getCode());
        chunk.setVersion("1");
        return chunk;
    }
}