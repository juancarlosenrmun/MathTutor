package com.mathtutor.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mathtutor.auth.adapter.TokenServiceAdapter;
import com.mathtutor.common.dto.NoteChunk;
import com.mathtutor.common.port.LlmGateway;
import com.mathtutor.common.port.RagRetriever;
import com.mathtutor.persistence.entity.User;
import com.mathtutor.persistence.repository.UserRepository;
import com.mathtutor.tutor.TutorSessions;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** @brief Comprueba RNF-01 y RNF-03 por HTTP/SSE en PostgreSQL con generación controlada. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "jwt.secret=synthetic-sprint-ten-key-for-local-load-only",
        "mathtutor.security.secure-cookies=false"})
@Testcontainers
class TutorLoadTest {
    private static final int CONVERSATIONS = 30;
    private static final String HINT = "¿Qué dato de los apuntes puedes relacionar con el problema?";
    private static final String BODY = """
            {"message":"concepto"}
            """;

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort int port;
    @Autowired ObjectMapper json;
    @Autowired TokenServiceAdapter tokens;
    @Autowired UserRepository users;
    @Autowired TutorSessions sessions;
    @Autowired HikariDataSource dataSource;
    @MockitoBean RagRetriever retriever;
    @MockitoBean LlmGateway llm;

    private record Latency(Duration firstToken, Duration complete) {}

    @Test
    @Timeout(60)
    void servesThirtyConcurrentConversationsWithoutHoldingJdbc() throws Exception {
        when(retriever.retrieve(anyString(), anyString(), any())).thenReturn(List.of(
                new NoteChunk(1L, "Apunte de prueba", null, "DEFINICION", "carga.tex", "funciones", "tema", "1")));
        var providerEntered = new CountDownLatch(CONVERSATIONS);
        var release = Sinks.<String>one();
        when(llm.stream(any(), any()))
                .thenReturn(Flux.just(HINT))
                .thenAnswer(call -> {
                    assertNoJdbcOnCurrentThread();
                    return Flux.defer(() -> {
                        assertNoJdbcOnCurrentThread();
                        providerEntered.countDown();
                        return release.asMono();
                    });
                });

        try (var client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()) {
            var csrf = client.send(HttpRequest.newBuilder(uri("/api/csrf-token")).build(),
                    HttpResponse.BodyHandlers.discarding());
            assertThat(csrf.statusCode()).isEqualTo(200);
            String cookie = csrf.headers().allValues("set-cookie").stream()
                    .filter(value -> value.startsWith("XSRF-TOKEN=")).findFirst().orElseThrow().split(";", 2)[0];
            request(client, messageRequest(cookie)); // Calienta el recorrido completo fuera de la muestra.
            var requests = new ArrayList<HttpRequest>();
            for (int i = 0; i < CONVERSATIONS; i++) requests.add(messageRequest(cookie));
            var executor = Executors.newVirtualThreadPerTaskExecutor();
            var results = new ArrayList<Future<Latency>>();
            var start = new CountDownLatch(1);
            try {
                for (var request : requests) results.add(executor.submit(() -> {
                    start.await();
                    return request(client, request);
                }));
                start.countDown();
                assertThat(providerEntered.await(6, TimeUnit.SECONDS))
                        .as("las 30 generaciones deben estar suscritas a la vez").isTrue();
                assertThat(dataSource.getHikariPoolMXBean().getActiveConnections()).isZero();
                assertNoDatabaseTransactions();
                assertThat(release.tryEmitValue(HINT)).isEqualTo(Sinks.EmitResult.OK);

                var timings = new ArrayList<Latency>();
                for (var result : results) timings.add(result.get(18, TimeUnit.SECONDS));
                assertThat(timings).hasSize(CONVERSATIONS);
                verify(llm, times(CONVERSATIONS + 1)).stream(any(), any());
                assertLatencyBudget(timings);
            } finally {
                results.forEach(result -> result.cancel(true));
                release.tryEmitEmpty();
                executor.shutdownNow();
            }
        }
    }

    /** @brief Mide el primer TOKEN legible y el DONE desde el envío de la petición. */
    private Latency request(HttpClient client, HttpRequest request) throws Exception {
        long started = System.nanoTime();
        var response = client.send(request, HttpResponse.BodyHandlers.ofLines());
        try (var lines = response.body()) {
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().firstValue("content-type").orElseThrow()).startsWith("text/event-stream");
            Duration first = null;
            var answer = new StringBuilder();
            var iterator = lines.iterator();
            while (iterator.hasNext()) {
                String line = iterator.next();
                if (!line.startsWith("data:")) continue;
                long received = System.nanoTime() - started;
                var event = json.readTree(line.substring(5));
                switch (event.path("type").asText()) {
                    case "TOKEN" -> {
                        if (first == null) first = Duration.ofNanos(received);
                        answer.append(event.path("content").asText());
                    }
                    case "DONE" -> {
                        assertThat(answer.toString()).isEqualTo(HINT);
                        assertThat(first).isNotNull();
                        return new Latency(first, Duration.ofNanos(received));
                    }
                    default -> throw new AssertionError("Evento SSE inesperado: " + event.path("type").asText());
                }
            }
        }
        throw new AssertionError("El tutor cerró el flujo sin DONE");
    }

    private HttpRequest messageRequest(String cookie) {
        var user = users.save(User.builder().email(UUID.randomUUID() + "@load.invalid")
                .passwordHash("carga-sintetica").role(User.Role.ALUMNO).build());
        var session = sessions.start(user.getId(), "funciones").id();
        String token = tokens.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        return HttpRequest.newBuilder(uri("/api/tutor/sessions/" + session + "/messages"))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .header("Authorization", "Bearer " + token)
                .header("Cookie", cookie)
                .header("X-XSRF-TOKEN", cookie.substring("XSRF-TOKEN=".length()))
                .POST(HttpRequest.BodyPublishers.ofString(BODY)).build();
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    private void assertNoJdbcOnCurrentThread() {
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        assertThat(TransactionSynchronizationManager.hasResource(dataSource)).isFalse();
    }

    /** @brief Observa las transacciones desde una conexión externa al pool de la aplicación. */
    private static void assertNoDatabaseTransactions() throws Exception {
        try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             var statement = connection.createStatement();
             var result = statement.executeQuery("""
                     SELECT count(*) FROM pg_stat_activity
                     WHERE datname = current_database() AND pid <> pg_backend_pid() AND xact_start IS NOT NULL
                     """)) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isZero();
        }
    }

    private static void assertLatencyBudget(List<Latency> timings) {
        var first = p95(timings.stream().mapToLong(result -> result.firstToken().toNanos()));
        var slowestFirst = timings.stream().map(Latency::firstToken).max(Duration::compareTo).orElseThrow();
        var complete = p95(timings.stream().mapToLong(result -> result.complete().toNanos()));
        System.out.printf("EN-06, %d conversaciones: primer TOKEN p95=%d ms, máximo=%d ms, DONE p95=%d ms%n",
                timings.size(), first.toMillis(), slowestFirst.toMillis(), complete.toMillis());
        assertThat(slowestFirst).isLessThanOrEqualTo(Duration.ofSeconds(2));
        assertThat(complete).isLessThanOrEqualTo(Duration.ofSeconds(15));
    }

    /** @brief Calcula el p95 por rango más próximo, sin interpolar entre observaciones. */
    private static Duration p95(LongStream observations) {
        long[] sorted = observations.sorted().toArray();
        return Duration.ofNanos(sorted[(int) Math.ceil(sorted.length * 0.95) - 1]);
    }

    @Test
    void percentileSortsObservationsAndUsesTheNearestRank() {
        assertThat(p95(LongStream.iterate(30, value -> value - 1).limit(30))).isEqualTo(Duration.ofNanos(29));
        assertThat(p95(LongStream.of(75))).isEqualTo(Duration.ofNanos(75));
    }
}
