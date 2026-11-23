package com.mathtutor.application;

import com.mathtutor.auth.adapter.TokenServiceAdapter;
import com.mathtutor.common.dto.NoteChunk;
import com.mathtutor.common.port.CasEngine;
import com.mathtutor.common.port.ConversationStore;
import com.mathtutor.common.port.LlmGateway;
import com.mathtutor.common.port.RagRetriever;
import com.mathtutor.persistence.entity.Exercise.ExerciseType;
import com.mathtutor.persistence.entity.User;
import com.mathtutor.persistence.repository.ExerciseRepository;
import com.mathtutor.persistence.repository.UserRepository;
import com.mathtutor.tutor.TutorSessions;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** @brief Contrato determinista de RNF-08 con CAS, persistencia y SSE reales. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"jwt.secret=synthetic-sprint-seven-key-for-local-evaluation-only",
                "mathtutor.security.secure-cookies=false"})
@Testcontainers
class PedagogicalEvaluationTest {
    private static final String HINT = "¿Qué dato del problema puedes relacionar con el concepto de los apuntes?";
    private record Case(String id, String category, String module, String topic, String prompt,
                        String operation, String expression, String reference) {
        @Override public String toString() { return id; }
    }

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
    @Autowired ExerciseRepository exercises;
    @Autowired TutorSessions sessions;
    @Autowired ConversationStore store;
    @Autowired CasEngine cas;
    @MockitoBean RagRetriever retriever;
    @MockitoBean LlmGateway llm;

    @Test
    void corpusMeetsTheCategoryMinimumsAndKeepsExercisesDistinct() throws IOException {
        var cases = cases();
        assertThat(cases).extracting(Case::id).doesNotHaveDuplicates();
        assertThat(cases.stream().filter(c -> !c.category().equals("F"))).hasSizeGreaterThanOrEqualTo(50);
        for (String category : List.of("A", "B", "C", "D", "F")) {
            assertThat(cases.stream().filter(c -> c.category().equals(category))).hasSizeGreaterThanOrEqualTo(10);
        }
        assertThat(cases.stream().map(c -> c.prompt() + "|" + c.expression())).doesNotHaveDuplicates();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void evaluatesThePublishedTurn(Case item) throws Exception {
        var user = users.save(User.builder().email(UUID.randomUUID() + "@evaluation.invalid")
                .passwordHash("sin-inicio-de-sesion").role(User.Role.ALUMNO).build());
        if (item.category().equals("F")) {
            when(retriever.retrieve(anyString(), eq(item.module()), any())).thenReturn(List.of());
            var session = sessions.start(user.getId(), item.module()).id();
            assertThat(answer(user, session, item.prompt())).contains("dentro del temario");
            assertThat(store.history(user.getId(), session, 0, 1).getFirst().status()).isEqualTo("RECHAZADO");
            verifyNoInteractions(llm);
            return;
        }
        if (item.category().equals("C")) {
            var session = prepare(user, item);
            String visible = answer(user, session, item.prompt());
            var run = assertBankRun(user, session, item, ExerciseType.TEORICO);
            assertThat(run.seedHints()).hasSize(1);
            assertThat(visible).isEqualTo(run.seedHints().getFirst()).endsWith("?").doesNotContain(item.reference());
            assertThat(store.history(user.getId(), session, 0, 1).getFirst().response()).isEqualTo(visible);
            verifyNoInteractions(llm, retriever);
            return;
        }

        String leak = switch (item.category()) {
            case "A", "B" -> "Al operar se obtiene $" + item.reference() + "$.";
            default -> item.reference();
        };
        String notes = item.category().equals("D")
                ? "\\begin{solution}\n" + item.reference() + "\n\\end{solution}" : "Apunte contextual de " + item.topic();
        when(retriever.retrieve(anyString(), eq(item.module()), any())).thenReturn(List.of(
                new NoteChunk(1L, notes, null, "DEFINICION", "evaluacion.tex", item.module(), item.topic(), "1")));
        when(llm.stream(any(), any())).thenAnswer(ignored -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return Flux.just(HINT);
        }).thenAnswer(ignored -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            int midpoint = leak.length() / 2;
            return Flux.just(leak.substring(0, midpoint), leak.substring(midpoint));
        });
        var safeSession = prepare(user, item);
        assertThat(answer(user, safeSession, item.prompt())).isEqualTo(HINT);
        var adversarialSession = prepare(user, item);
        String visible = answer(user, adversarialSession, item.prompt());
        assertThat(visible).endsWith("?").doesNotContain(item.reference()).isNotEqualTo(leak);
        var saved = store.history(user.getId(), adversarialSession, 0, 1).getFirst();
        assertThat(saved.response()).isEqualTo(visible);
        assertThat(saved.status()).isEqualTo("COMPLETADO");
        if (item.category().equals("A")) assertBankRun(user, adversarialSession, item, ExerciseType.PROCEDIMENTAL);
        verify(llm, times(2)).stream(any(), any());
    }

    private ConversationStore.Run assertBankRun(User user, Long session, Case item, ExerciseType type) {
        var expected = exercises.findByExternalId(item.id()).orElseThrow();
        assertThat(expected.getType()).isEqualTo(type);
        var run = store.current(user.getId(), session);
        assertThat(run.exerciseId()).isEqualTo(expected.getId());
        assertThat(run.topic()).isEqualTo(item.topic());
        return run;
    }

    private Long prepare(User user, Case item) throws Exception {
        var session = sessions.start(user.getId(), item.module()).id();
        if (item.category().equals("B")) {
            post(user, "/api/tutor/sessions/" + session + "/exercises")
                    .bodyValue(Map.of("operation", item.operation(), "expression", item.expression()))
                    .exchange().expectStatus().isCreated();
            var run = store.current(user.getId(), session);
            assertThat(run.exerciseId()).isNull();
            assertThat(cas.compare(item.reference(), run.reference())).isEqualTo(CasEngine.Comparison.EQUIVALENT);
        }
        return session;
    }

    private String answer(User user, Long session, String prompt) throws Exception {
        byte[] response = post(user, "/api/tutor/sessions/" + session + "/messages")
                .accept(MediaType.TEXT_EVENT_STREAM).bodyValue(Map.of("message", prompt))
                .exchange().expectStatus().isOk().expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
                .expectBody().returnResult().getResponseBody();
        assertThat(response).isNotEmpty();
        String events = new String(response, StandardCharsets.UTF_8);
        var visible = new StringBuilder();
        String lastType = "";
        for (String line : events.split("\\R")) {
            if (!line.startsWith("data:")) continue;
            var event = json.readTree(line.substring(5));
            lastType = event.path("type").asText();
            assertThat(lastType).isNotEqualTo("ERROR");
            if (lastType.equals("TOKEN")) visible.append(event.path("content").asText());
        }
        assertThat(lastType).isEqualTo("DONE");
        return visible.toString();
    }

    private WebTestClient.RequestBodySpec post(User user, String path) {
        // Tomcat gestiona las escrituras concurrentes de SSE sobre un socket real.
        var client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofSeconds(35)).build();
        var csrf = client.get().uri("/api/csrf-token").exchange().expectStatus().isOk()
                .expectCookie().exists("XSRF-TOKEN").expectBody().returnResult();
        String value = csrf.getResponseCookies().getFirst("XSRF-TOKEN").getValue();
        String token = tokens.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        return client.post().uri(path).contentType(MediaType.APPLICATION_JSON)
                .cookie("XSRF-TOKEN", value).header("X-XSRF-TOKEN", value)
                .headers(headers -> headers.setBearerAuth(token));
    }

    static List<Case> cases() throws IOException {
        try (var resource = PedagogicalEvaluationTest.class.getResourceAsStream("/pedagogical-evaluation.tsv");
             var reader = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))) {
            return reader.lines().skip(1).map(line -> {
                String[] fields = line.split("\\t", -1);
                return new Case(fields[0], fields[1], fields[2], fields[3], fields[4], fields[5], fields[6], fields[7]);
            }).toList();
        }
    }
}
