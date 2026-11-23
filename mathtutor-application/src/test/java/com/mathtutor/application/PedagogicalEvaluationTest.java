package com.mathtutor.application;

import com.mathtutor.auth.filter.JwtAuthenticationFilter.AuthenticatedUser;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** @brief Contrato determinista de RNF-08 con CAS, persistencia y SSE reales. */
@SpringBootTest(properties = "jwt.secret=synthetic-sprint-seven-key-for-local-evaluation-only")
@AutoConfigureMockMvc
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

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
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
        if (item.category().equals("A") || item.category().equals("C")) {
            var expected = exercises.findByExternalId(item.id()).orElseThrow();
            assertThat(expected.getType()).isEqualTo(item.category().equals("A") ? ExerciseType.PROCEDIMENTAL : ExerciseType.TEORICO);
            var run = store.current(user.getId(), adversarialSession);
            assertThat(run.exerciseId()).isEqualTo(expected.getId());
            assertThat(run.topic()).isEqualTo(item.topic());
        }
        verify(llm, times(2)).stream(any(), any());
    }

    private Long prepare(User user, Case item) throws Exception {
        var session = sessions.start(user.getId(), item.module()).id();
        if (item.category().equals("B")) {
            mvc.perform(post("/api/tutor/sessions/{id}/exercises", session).with(actor(user)).with(csrf())
                    .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(
                            Map.of("operation", item.operation(), "expression", item.expression()))))
                    .andExpect(status().isCreated());
            var run = store.current(user.getId(), session);
            assertThat(run.exerciseId()).isNull();
            assertThat(cas.compare(item.reference(), run.reference())).isEqualTo(CasEngine.Comparison.EQUIVALENT);
        }
        return session;
    }

    private String answer(User user, Long session, String prompt) throws Exception {
        var async = mvc.perform(post("/api/tutor/sessions/{id}/messages", session).with(actor(user)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(Map.of("message", prompt))))
                .andExpect(request().asyncStarted()).andReturn();
        String events = mvc.perform(asyncDispatch(async)).andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString(StandardCharsets.UTF_8);
        var visible = new StringBuilder();
        for (String line : events.split("\\R")) {
            if (!line.startsWith("data:")) continue;
            var event = json.readTree(line.substring(5));
            assertThat(event.path("type").asText()).isNotEqualTo("ERROR");
            if (event.path("type").asText().equals("TOKEN")) visible.append(event.path("content").asText());
        }
        return visible.toString();
    }

    private RequestPostProcessor actor(User user) {
        return authentication(UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUser(user.getId(), user.getEmail(), "ALUMNO"), "",
                List.of(new SimpleGrantedAuthority("ROLE_ALUMNO"))));
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
