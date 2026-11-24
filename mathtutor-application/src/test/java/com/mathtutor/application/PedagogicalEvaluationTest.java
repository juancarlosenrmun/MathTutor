package com.mathtutor.application;

import com.mathtutor.auth.adapter.TokenServiceAdapter;
import com.mathtutor.common.dto.NoteChunk;
import com.mathtutor.common.port.CasEngine;
import com.mathtutor.common.port.ConversationStore;
import com.mathtutor.common.port.LlmGateway;
import com.mathtutor.common.port.RagRetriever;
import com.mathtutor.persistence.entity.CanonicalStep;
import com.mathtutor.persistence.entity.Exercise;
import com.mathtutor.persistence.entity.Exercise.ExerciseType;
import com.mathtutor.persistence.entity.User;
import com.mathtutor.persistence.repository.ExerciseRepository;
import com.mathtutor.persistence.repository.ExerciseStatistics;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mockingDetails;
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
    private record Dialogue(String id, String split, String module, String operation, String expression,
                            String exercise, List<Message> messages, List<ExpectedTurn> expected) {
        @Override public String toString() { return split + "/" + id; }
    }
    private record Message(String role, String content) {}
    private record ExpectedTurn(String kind, String status, String verdict, String state, double progress, int attempts,
                                int verifiedSteps) {}

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
    @Autowired ExerciseStatistics statistics;
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
    @MethodSource("dialogues")
    void preservesContextAndRecordsEachSimulatedTurn(Dialogue dialogue) throws Exception {
        var user = users.save(User.builder().email(UUID.randomUUID() + "@evaluation.invalid")
                .passwordHash("sin-inicio-de-sesion").role(User.Role.ALUMNO).build());
        var session = sessions.start(user.getId(), dialogue.module()).id();
        var exercisesPath = "/api/tutor/sessions/" + session + "/exercises";
        boolean verifiesPartialProgress = "SIM-PROC-001".equals(dialogue.exercise());
        long nearSolutionBefore = verifiesPartialProgress ? nearSolution(dialogue.module()) : 0;
        when(retriever.retrieve(anyString(), eq(dialogue.module()), any())).thenReturn(List.of());
        when(llm.stream(any(), any())).thenAnswer(ignored -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return Flux.just(HINT);
        });
        if (dialogue.operation() != null) {
            post(user, exercisesPath).bodyValue(Map.of("operation", dialogue.operation(), "expression", dialogue.expression()))
                    .exchange().expectStatus().isCreated();
        }
        if (verifiesPartialProgress) {
            createSimulatedExercise();
        }
        assertThat(dialogue.messages()).hasSize(dialogue.expected().size() * 2);
        Long exerciseRun = null;
        UUID request = null;
        String visible = null;
        for (int i = 0; i < dialogue.expected().size(); i++) {
            var student = dialogue.messages().get(2 * i);
            var tutor = dialogue.messages().get(2 * i + 1);
            var expected = dialogue.expected().get(i);
            assertThat(student.role()).isEqualTo("user");
            assertThat(tutor.role()).isEqualTo("assistant");
            assertThat(tutor.content()).isNotBlank();
            request = UUID.randomUUID();
            int calls = mockingDetails(llm).getInvocations().size();
            visible = answer(user, session, Map.of("message", student.content(), "requestId", request));
            assertThat(visible).as(student.content()).isNotBlank();
            int generationCalls = mockingDetails(llm).getInvocations().size() - calls;
            if (expected.kind().equals("fuera_de_contexto")) {
                assertThat(visible).contains("dentro del temario");
                assertThat(generationCalls).isZero();
            } else {
                assertThat(visible).as(student.content()).doesNotContain("dentro del temario");
                assertThat(generationCalls).isBetween(0, 1);
            }
            var saved = store.history(user.getId(), session, 0, 1).getFirst();
            assertThat(saved.status()).as(student.content()).isEqualTo(expected.status());
            assertThat(saved.verdict()).as(student.content()).isEqualTo(expected.verdict());
            assertThat(saved.response()).isEqualTo(visible);
            var run = store.runs(user.getId(), session, 0, 5);
            assertThat(run).hasSize(1);
            assertThat(run.getFirst().attempts()).as(student.content()).isEqualTo(expected.attempts());
            if (exerciseRun == null) exerciseRun = run.getFirst().id();
            assertThat(run.getFirst().id()).isEqualTo(exerciseRun);
            if (dialogue.exercise() != null) {
                assertThat(run.getFirst().exerciseId()).isEqualTo(exercises.findByExternalId(dialogue.exercise())
                        .orElseThrow().getId());
            }
            assertThat(run.getFirst().verifiedSteps()).as(student.content()).isEqualTo(expected.verifiedSteps());
            client().get().uri(exercisesPath).headers(headers -> headers.setBearerAuth(token(user)))
                    .exchange().expectStatus().isOk().expectBody()
                    .jsonPath("$[0].state").isEqualTo(expected.state())
                    .jsonPath("$[0].progress").isEqualTo(expected.progress())
                    .jsonPath("$[0].verifiedSteps").isEqualTo(expected.verifiedSteps())
                    .jsonPath("$[0].reference").doesNotExist();
            if (verifiesPartialProgress) {
                long nearSolutionDelta = expected.progress() == 0.5 ? 1 : 0;
                assertThat(nearSolution(dialogue.module())).as(student.content())
                        .isEqualTo(nearSolutionBefore + nearSolutionDelta);
            }
        }
        var lastStudent = dialogue.messages().get(dialogue.messages().size() - 2);
        int calls = mockingDetails(llm).getInvocations().size();
        assertThat(answer(user, session, Map.of("message", lastStudent.content(), "requestId", request))).isEqualTo(visible);
        assertThat(mockingDetails(llm).getInvocations()).hasSize(calls);
        assertThat(store.history(user.getId(), session, 0, 20)).hasSize(dialogue.expected().size());
        assertThat(store.runs(user.getId(), session, 0, 5).getFirst().attempts())
                .isEqualTo(dialogue.expected().getLast().attempts());
    }

    private void createSimulatedExercise() {
        var seed = exercises.findByExternalId("CAL-PROC-001").orElseThrow();
        var exercise = Exercise.builder().externalId("SIM-PROC-001").type(ExerciseType.PROCEDIMENTAL)
                .difficulty(2).module(seed.getModule()).topic(seed.getTopic())
                .statementLatex("Calcula la derivada de x^3+5*x^2.")
                .referenceSolutionLatex("3*x^2+10*x").build();
        exercise.getCanonicalSteps().add(CanonicalStep.builder().exercise(exercise)
                .position(1).expressionLatex("3*x^2").build());
        exercise.getCanonicalSteps().add(CanonicalStep.builder().exercise(exercise)
                .position(2).expressionLatex("3*x^2+10*x").build());
        exercises.save(exercise);
    }

    private long nearSolution(String module) {
        return statistics.summarize(module, Instant.now()).stream().mapToLong(ExerciseStatistics.Summary::nearSolution).sum();
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

        if (item.category().equals("D")) {
            var session = prepare(user, item);
            String visible = answer(user, session, item.prompt());
            var saved = store.history(user.getId(), session, 0, 1).getFirst();
            assertThat(visible).contains("¿").endsWith("?").doesNotContain(item.reference(), "Qué hipótesis tienes");
            assertThat(saved.response()).isEqualTo(visible);
            assertThat(saved.topic()).isEqualTo(item.topic());
            assertThat(saved.status()).isEqualTo("COMPLETADO");
            assertThat(store.current(user.getId(), session)).isNull();
            verifyNoInteractions(llm, retriever);
            return;
        }

        String leak = "Al operar se obtiene $" + item.reference() + "$.";
        String notes = "Apunte contextual de " + item.topic();
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
        return answer(user, session, Map.of("message", prompt));
    }

    private String answer(User user, Long session, Map<String, ?> body) throws Exception {
        byte[] response = post(user, "/api/tutor/sessions/" + session + "/messages")
                .accept(MediaType.TEXT_EVENT_STREAM).bodyValue(body)
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
        var client = client();
        var csrf = client.get().uri("/api/csrf-token").exchange().expectStatus().isOk()
                .expectCookie().exists("XSRF-TOKEN").expectBody().returnResult();
        String value = csrf.getResponseCookies().getFirst("XSRF-TOKEN").getValue();
        return client.post().uri(path).contentType(MediaType.APPLICATION_JSON)
                .cookie("XSRF-TOKEN", value).header("X-XSRF-TOKEN", value)
                .headers(headers -> headers.setBearerAuth(token(user)));
    }

    private String token(User user) {
        return tokens.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
    }

    private WebTestClient client() {
        // Tomcat gestiona las escrituras concurrentes de SSE sobre un socket real.
        return WebTestClient.bindToServer().baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofSeconds(35)).build();
    }

    static List<Dialogue> dialogues() throws IOException {
        var mapper = new ObjectMapper();
        try (var resource = PedagogicalEvaluationTest.class.getResourceAsStream("/tutor-conversations.jsonl");
             var reader = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))) {
            var dialogues = new ArrayList<Dialogue>();
            for (String line; (line = reader.readLine()) != null;) {
                dialogues.add(mapper.readValue(line, Dialogue.class));
            }
            return List.copyOf(dialogues);
        }
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
