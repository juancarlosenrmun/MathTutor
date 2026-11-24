package com.mathtutor.application;

import com.mathtutor.auth.filter.JwtAuthenticationFilter.AuthenticatedUser;
import com.mathtutor.auth.adapter.TokenServiceAdapter;
import com.mathtutor.common.port.*;
import com.mathtutor.common.dto.*;
import com.mathtutor.persistence.entity.*;
import com.mathtutor.persistence.repository.*;
import com.mathtutor.tutor.*;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.netty.http.client.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Caja negra con esquema Flyway real, pgvector, autorizaciÃ³n, CSRF y streaming. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties={"spring.jpa.hibernate.ddl-auto=validate","spring.jpa.open-in-view=false",
        "jwt.secret=test-secret-key-at-least-256-bits-long-for-hs256-signing",
        "tutor.quota.usd-per-million-input=1", "tutor.quota.usd-per-million-output=2"})
@AutoConfigureMockMvc @Testcontainers
class SprintFourIntegrationTest {
    @Container static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("pgvector/pgvector:pg16");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",postgres::getJdbcUrl); r.add("spring.datasource.username",postgres::getUsername); r.add("spring.datasource.password",postgres::getPassword);
    }
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired TutorSessionRepository sessions;
    @Autowired ModuleRepository modules;
    @Autowired ConversationStore store;
    @Autowired TrackedConversation tracked;
    @Autowired StudyExercises exercises;
    @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
    @Autowired javax.sql.DataSource dataSource;
    @Autowired TokenBudget budget;
    @Autowired TokenServiceAdapter tokens;
    @Autowired @Qualifier("streamingSecurityFilterChain") SecurityFilterChain streamingSecurity;
    @LocalServerPort int port;
    @MockBean LlmGateway llm;
    @MockBean RagRetriever retriever;
    @MockBean CasEngine cas;
    Long user,session;
    org.springframework.test.web.servlet.request.RequestPostProcessor actor(String role) {
        return authentication(UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedUser(user,"test@test.es",role),"",List.of(new SimpleGrantedAuthority("ROLE_"+role))));
    }

    /** @brief Mantiene el repositorio CSRF real cuando MockMvc y el servidor comparten contexto. */
    private static RequestPostProcessor csrfCookie() {
        return request -> {
            String token = UUID.randomUUID().toString();
            request.setCookies(new Cookie("XSRF-TOKEN", token));
            request.addHeader("X-XSRF-TOKEN", token);
            return request;
        };
    }
    @BeforeEach void setup() {
        user=users.save(User.builder().email(UUID.randomUUID()+"@test.es").passwordHash("h").active(true).role(User.Role.ALUMNO).build()).getId();
        session=sessions.save(TutorSession.builder().user(users.getReferenceById(user)).module(modules.findAll().getFirst()).status(TutorSession.SessionStatus.ACTIVA).build()).getId();
        when(retriever.retrieve(anyString(),anyString(),any())).thenReturn(List.of(new com.mathtutor.common.dto.NoteChunk(1L,"apunte",null,"DEFINICION","a.tex","funciones","tema","1")));
        when(llm.stream(any(),any())).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return Flux.just("Pista");
        });
    }
    @Test void ownHistoryAndPaginationAreProtected() throws Exception {
        mvc.perform(get("/api/tutor/progress").with(actor("ALUMNO"))).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(session));
        mvc.perform(get("/api/tutor/progress?size=101").with(actor("ALUMNO"))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/tutor/sessions/"+session+"/history").with(actor("ALUMNO"))).andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/api/tutor/sessions/999999/history").with(actor("ALUMNO"))).andExpect(status().isNotFound());
        mvc.perform(get("/api/tutor/progress").with(actor("PROFESOR"))).andExpect(status().isOk());
        mvc.perform(get("/api/tutor/progress")).andExpect(status().isUnauthorized());
        Long owner=user; user=users.save(User.builder().email(UUID.randomUUID()+"@test.es").passwordHash("h").active(true).role(User.Role.ALUMNO).build()).getId();
        mvc.perform(get("/api/tutor/sessions/"+session+"/history").with(actor("ALUMNO"))).andExpect(status().isNotFound());
        assertThat(store.sessions(owner,0,100)).hasSize(1);
    }
    @Test void studentCanDeleteOnlyTheirOwnConversationAndItsExercises() throws Exception {
        store.select(user, session, null, "Deriva x^2", "2*x");
        var request = UUID.randomUUID();
        store.reserve(user, session, request, "2*x");
        String path = "/api/tutor/sessions/" + session;
        mvc.perform(delete(path).with(actor("ALUMNO")).with(csrfCookie())).andExpect(status().isConflict());
        store.finish(user, session, request, new ConversationStore.Result("Correcto", null,
                ErrorType.NINGUNO, "COMPLETADO", 0, null, null, 0, true));
        mvc.perform(delete(path).with(actor("ALUMNO"))).andExpect(status().isForbidden());
        mvc.perform(delete(path).with(actor("PROFESOR")).with(csrfCookie())).andExpect(status().isForbidden());
        Long owner = user;
        user = users.save(User.builder().email(UUID.randomUUID()+"@test.es").passwordHash("h")
                .active(true).role(User.Role.ALUMNO).build()).getId();
        mvc.perform(delete(path).with(actor("ALUMNO")).with(csrfCookie())).andExpect(status().isNotFound());
        user = owner;
        store.select(user, session, null, "Deriva x^3", "3*x^2");
        mvc.perform(delete(path).with(actor("ALUMNO")).with(csrfCookie())).andExpect(status().isNoContent());
        mvc.perform(delete(path).with(actor("ALUMNO")).with(csrfCookie())).andExpect(status().isNotFound());
        var sql = new org.springframework.jdbc.core.JdbcTemplate(dataSource);
        assertThat(sql.queryForObject("SELECT SUM(abandoned) FROM exercise_deletion_counts", Long.class)).isEqualTo(1L);
        mvc.perform(get("/api/teacher/statistics").with(actor("PROFESOR")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].abandoned", org.hamcrest.Matchers.hasItem(1)));
        mvc.perform(get(path + "/history").with(actor("ALUMNO"))).andExpect(status().isNotFound());
        assertThat(store.sessions(user, 0, 20)).isEmpty();
    }

    @Test void bankCatalogueIsOwnedPaginatedAndNeverExposesSolutions() throws Exception {
        mvc.perform(get("/api/tutor/sessions/"+session+"/bank?size=1").with(actor("ALUMNO")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].statement").isString()).andExpect(jsonPath("$[0].reference").doesNotExist());
        mvc.perform(get("/api/tutor/sessions/999999/bank").with(actor("ALUMNO"))).andExpect(status().isNotFound());
        mvc.perform(get("/api/tutor/sessions/"+session+"/bank?size=101").with(actor("ALUMNO"))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/tutor/sessions/"+session+"/bank").with(actor("PROFESOR"))).andExpect(status().isOk());
    }
    @Test void writesContextAndReplaysWithExactlyOneLlmCall() {
        UUID id=UUID.randomUUID();
        assertThat(tracked.reply(user,session,id,"concepto").collectList().block()).containsExactly("Pista");
        assertThat(tracked.reply(user,session,id,"concepto").collectList().block()).containsExactly("Pista");
        verify(llm,times(1)).stream(any(),any());
        assertThat(store.history(user,session,0,20).getFirst().topic()).isEqualTo("tema");
        assertThat(store.history(user,session,0,20).getFirst().verdict()).isEqualTo("NO_APLICA");
        tracked.reply(user,session,UUID.randomUUID(),"otra pregunta").blockLast();
        var prompts=org.mockito.ArgumentCaptor.forClass(Prompt.class); verify(llm,times(2)).stream(prompts.capture(),any());
        assertThat(prompts.getAllValues().getLast().contextChunks()).anyMatch(s -> s.contains("HISTORIAL") && s.contains("concepto"));
    }
    @Test void followsTheStudentsReasoningAndShortRepliesInTheSameSession() throws Exception {
        var functions = modules.findAll().stream().filter(module -> module.getCode().equals("funciones"))
                .findFirst().orElseThrow();
        session = sessions.save(TutorSession.builder().user(users.getReferenceById(user)).module(functions)
                .status(TutorSession.SessionStatus.ACTIVA).build()).getId();
        when(retriever.retrieve(anyString(), anyString(), any())).thenReturn(List.of());
        var json = new com.fasterxml.jackson.databind.ObjectMapper();
        for (String message : List.of("¿Qué es la continuidad de una función?", "f(x) = x^2+2*x es continua ?",
                "Una fucnión polinomica siempre es continua y derivable", "¿Por qué?")) {
            assertThat(streamOverHttp("messages", json.writeValueAsString(Map.of("message", message))))
                    .contains("Pista", "DONE");
        }
        for (String message : List.of("Calcula la derivada de la función x^2", "¿Quién ganó el partido?")) {
            assertThat(streamOverHttp("messages", json.writeValueAsString(Map.of("message", message))))
                    .contains("dentro del temario");
        }
        var prompts = org.mockito.ArgumentCaptor.forClass(Prompt.class);
        verify(llm, times(4)).stream(prompts.capture(), any());
        assertThat(prompts.getAllValues().get(2).contextChunks())
                .anyMatch(chunk -> chunk.contains("f(x) = x^2+2*x es continua ?"));
        assertThat(prompts.getAllValues().getLast().contextChunks())
                .anyMatch(chunk -> chunk.contains("Una fucnión polinomica siempre es continua y derivable"));
    }
    @Test void keepsHelpingWhenTheStudentDoesNotKnowTheCrossProduct() throws Exception {
        var methods = modules.findAll().stream().filter(module -> module.getCode().equals("metodos-3d"))
                .findFirst().orElseThrow();
        session = sessions.save(TutorSession.builder().user(users.getReferenceById(user)).module(methods)
                .status(TutorSession.SessionStatus.ACTIVA).build()).getId();
        when(retriever.retrieve(anyString(), anyString(), any())).thenReturn(List.of());
        var json = new com.fasterxml.jackson.databind.ObjectMapper();
        assertThat(streamOverHttp("messages", json.writeValueAsString(Map.of("message", "¿Qué es un espacio vectorial?"))))
                .contains("Pista", "DONE");
        assertThat(streamOverHttp("messages", json.writeValueAsString(Map.of("message", "Calcula e_1 \\times e_2"))))
                .contains("regla de la mano derecha", "DONE");
        assertThat(streamOverHttp("messages", json.writeValueAsString(Map.of("message", "no lo se"))))
                .contains("Pista", "DONE");
        var prompts = org.mockito.ArgumentCaptor.forClass(Prompt.class);
        verify(llm, times(2)).stream(prompts.capture(), any());
        assertThat(prompts.getValue().contextChunks()).anyMatch(chunk -> chunk.contains("e_1 \\times e_2"));
        assertThat(prompts.getValue().constraints()).contains("pista más concreta");
        assertThat(store.context(user, session).getLast().topic()).isEqualTo("geometria");
        assertThat(store.current(user, session).attempts()).isZero();
        verify(cas, never()).compare(eq("no lo se"), anyString());
    }
    @Test void keepsTheExerciseAndItsStartingPointBeyondTheRecentHistoryWindow() throws Exception {
        var calculus = modules.findAll().stream().filter(module -> module.getCode().equals("derivadas-integrales"))
                .findFirst().orElseThrow();
        session = sessions.save(TutorSession.builder().user(users.getReferenceById(user)).module(calculus)
                .status(TutorSession.SessionStatus.ACTIVA).build()).getId();
        var run = store.select(user, session, null, "DERIVADA: abs(x)", "sign(x)");
        when(retriever.retrieve(anyString(), anyString(), any())).thenReturn(List.of());
        when(cas.compare(anyString(), eq("sign(x)"))).thenReturn(CasEngine.Comparison.DIFFERENT);
        when(llm.stream(any(), any())).thenAnswer(call -> {
            java.util.function.Consumer<LlmGateway.Usage> usage = call.getArgument(1);
            usage.accept(new LlmGateway.Usage(200, 50));
            return Flux.just("Pista");
        });
        var json = new com.fasterxml.jackson.databind.ObjectMapper();
        var steps = List.of("Quiero estudiar la derivabilidad de abs(x) en el origen", "¿Y la continuidad?",
                "Por la izquierda me sale cero", "Por la derecha también obtengo cero", "Entonces coinciden los dos",
                "¿Cómo sigo?", "No lo sé", "Primero sustituyo el valor en la función", "Me queda un cociente",
                "Ahora comparo ambos lados", "¿Por qué?", "El límite lateral cambia de signo", "¿Es correcto?");
        for (String step : steps) {
            assertThat(streamOverHttp("messages", json.writeValueAsString(Map.of("message", step))))
                    .as(step).contains("Pista", "DONE");
        }
        assertThat(streamOverHttp("messages", "{\"message\":\"¿Quién ganó el partido?\"}"))
                .contains("dentro del temario");
        var prompts = org.mockito.ArgumentCaptor.forClass(Prompt.class);
        verify(llm, times(steps.size())).stream(prompts.capture(), any());
        assertThat(prompts.getValue().contextChunks()).anyMatch(chunk -> chunk.contains("Resumen de turnos anteriores")
                && chunk.contains(steps.getFirst()) && chunk.contains(steps.get(2))
                && chunk.contains("Alumno: " + steps.get(steps.size() - 2))
                && !chunk.contains("¿Quién ganó el partido?"));
        assertThat(prompts.getValue().contextChunks()).anyMatch(chunk -> chunk.contains(run.statement()));
        assertThat(prompts.getAllValues().get(2).constraints()).contains("Distingue el objetivo", "límites de la función");
        assertThat(store.select(user, session, null, run.statement(), "sign(x)").id()).isEqualTo(run.id());
        assertThat(store.current(user, session).attempts()).isZero();
        verifyNoInteractions(cas);
    }
    @Test void recordsFreeContinuityCompletionAndStartsAnotherExerciseInTheSameChat() throws Exception {
        var functions = modules.findAll().stream().filter(module -> module.getCode().equals("funciones"))
                .findFirst().orElseThrow();
        session = sessions.save(TutorSession.builder().user(users.getReferenceById(user)).module(functions)
                .status(TutorSession.SessionStatus.ACTIVA).build()).getId();
        when(retriever.retrieve(anyString(), anyString(), any())).thenReturn(List.of());
        var json = new com.fasterxml.jackson.databind.ObjectMapper();
        for (String step : List.of("¿Qué es la continuidad de una función?", "f(x)=x^2+x es continua?",
                "Todas las funciones polinomicas son contninuas, entonces x^2+x es continua tambien, no?")) {
            assertThat(streamOverHttp("messages", json.writeValueAsString(Map.of("message", step))))
                    .as(step).contains("Pista", "DONE");
        }
        assertThat(streamOverHttp("messages", "{\"message\":\"ya las he comprobado y se cumple\"}"))
                .contains("límite existe", "valor de la función", "DONE");
        var active = store.current(user, session);
        assertThat(active).isNotNull();
        assertThat(streamOverHttp("messages", json.writeValueAsString(Map.of("message", "f(x)=x^3 es continua?"))))
                .contains("Primero termina", "DONE");
        assertThat(store.current(user, session).id()).isEqualTo(active.id());
        assertThat(streamOverHttp("messages", "{\"message\":\"ejercicio completado\"}"))
                .contains("Damos por terminado", "DONE");
        assertThat(store.context(user, session).getLast().topic()).isEqualTo("funciones-continuidad");
        var finished = store.runs(user, session, 0, 20).getFirst();
        assertThat(finished.state()).isEqualTo("COMPLETADO");
        assertThat(finished.topic()).isEqualTo("funciones-continuidad");
        assertThat(finished.verifiedSteps()).isZero();
        assertThat(streamOverHttp("messages", json.writeValueAsString(Map.of("message", "f(x)=x^3 es continua?"))))
                .contains("DONE");
        assertThat(store.current(user, session).id()).isNotEqualTo(finished.id());
        verifyNoInteractions(cas);
    }

    @Test void aCompletionRequestKeepsTheProceduralExerciseOpenUntilItsResultIsVerified() {
        var run = store.select(user, session, null, "DERIVADA: x^2", "2*x");
        assertThat(streamOverHttp("messages", "{\"message\":\"ejercicio completado\"}"))
                .contains("Envía tu resultado final", "DONE");
        assertThat(store.current(user, session).id()).isEqualTo(run.id());
        assertThat(store.current(user, session).attempts()).isZero();
        verifyNoInteractions(cas, llm);
        when(cas.compare("2*x", "2*x")).thenReturn(CasEngine.Comparison.EQUIVALENT);
        assertThat(streamOverHttp("messages", "{\"message\":\"2*x\"}"))
                .contains("Tu resultado ha sido verificado", "DONE");
        assertThat(store.current(user, session)).isNull();
        assertThat(store.runs(user, session, 0, 20).getFirst().state()).isEqualTo("COMPLETADO");
    }
    @Test void providerOutageStreamsAndPersistsNotesWithoutPartialGeneratedText() {
        when(llm.stream(any(), any())).thenReturn(Flux.just("delta privado")
                .concatWith(Flux.error(new IllegalStateException("fallo interno"))));
        UUID request = UUID.randomUUID();
        String body = "{\"message\":\"concepto\",\"requestId\":\"" + request + "\"}";
        assertThat(streamOverHttp("messages", body)).contains("Modo solo apuntes", "apunte", "DONE")
                .doesNotContain("delta privado", "fallo interno");
        assertThat(store.history(user, session, 0, 20).getFirst().response()).contains("Modo solo apuntes");
        assertThat(streamOverHttp("messages", body)).contains("Modo solo apuntes", "DONE");
        verify(llm, times(1)).stream(any(), any());
    }
    @Test void metricsReflectCommittedTurnsWithoutCountingSseReplays() throws Exception {
        when(llm.stream(any(), any())).thenAnswer(invocation -> {
            java.util.function.Consumer<LlmGateway.Usage> usage = invocation.getArgument(1);
            usage.accept(new LlmGateway.Usage(13, 4));
            return Flux.just("Pista");
        });
        var path = "/actuator/metrics/mathtutor.turn.tokens?tag=direction:input";
        var admin = actor("ADMIN");
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        mvc.perform(get(path).with(actor("ALUMNO"))).andExpect(status().isForbidden());
        double previous = inputTokensMetric(path, admin);
        String body = "{\"message\":\"concepto\",\"requestId\":\"" + UUID.randomUUID() + "\"}";
        assertThat(streamOverHttp("messages", body)).contains("DONE");
        assertThat(streamOverHttp("messages", body)).contains("DONE");
        assertThat(inputTokensMetric(path, admin) - previous).isEqualTo(13);
        mvc.perform(get("/actuator/metrics/mathtutor.turn.latency").with(admin)).andExpect(status().isOk());
    }

    private double inputTokensMetric(String path, RequestPostProcessor admin) throws Exception {
        var response = mvc.perform(get(path).with(admin)).andReturn().getResponse();
        if (response.getStatus() == 404) return 0;
        assertThat(response.getStatus()).isEqualTo(200);
        return json.readTree(response.getContentAsString()).path("measurements").path(0).path("value").asDouble();
    }
    @Test void freeExerciseNeverExposesReferenceAndRequiresCsrf() throws Exception {
        when(cas.parse("x^2")).thenReturn(new CasEngine.Expr("x^2","x^2",null)); when(cas.derive("x^2","x")).thenReturn("2*x");
        var body="{\"operation\":\"DERIVADA\",\"expression\":\"x^2\"}";
        mvc.perform(post("/api/tutor/sessions/"+session+"/exercises").with(actor("ALUMNO")).contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/tutor/sessions/"+session+"/exercises").with(actor("ALUMNO")).with(csrfCookie()).contentType("application/json").content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.reference").doesNotExist()).andExpect(jsonPath("$.state").value("COMENZADO"));
        when(cas.parse("2*x")).thenReturn(new CasEngine.Expr("2*x","2*x",null));
        when(cas.compare("2*x","2*x")).thenReturn(CasEngine.Comparison.EQUIVALENT);
        tracked.reply(user,session,UUID.randomUUID(),"2*x").blockLast();
        mvc.perform(get("/api/tutor/sessions/"+session+"/exercises").with(actor("ALUMNO"))).andExpect(status().isOk()).andExpect(jsonPath("$[0].state").value("COMPLETADO")).andExpect(jsonPath("$[0].reference").doesNotExist());
    }
    @Test void graphEventsContainValidatedData() throws Exception {
        when(cas.parse("x^2")).thenReturn(new CasEngine.Expr("x^2","x^2",null));
        assertThat(streamOverHttp("graphics", """
                {"type":"CURVA_2D","function":"x^2","domain":{"min":-10,"max":10,"steps":100}}
                """)).contains("GRAPHIC_SPEC");
    }

    @Test void recognizedStatementCreatesBankRunAndDoesNotCountAsAnAttempt() {
        var exercise = store.bank(user, session, 0, 100).getFirst();
        var id = UUID.randomUUID();
        assertThat(tracked.reply(user, session, id, "Ejercicio " + exercise.externalId()).blockLast()).isNotBlank();
        var run = store.current(user, session);
        assertThat(run.exerciseId()).isEqualTo(exercise.id());
        assertThat(run.topic()).isEqualTo(exercise.topic());
        assertThat(run.bankReference()).isNotBlank();
        assertThat(run.attempts()).isZero();
        assertThat(store.history(user, session, 0, 10).getFirst().runId()).isEqualTo(run.id());
        tracked.reply(user, session, id, "Ejercicio " + exercise.externalId()).blockLast();
        verify(llm, times(1)).stream(any(), any());
    }

    @Test void rejectsExtractionBeforeConsultingNotesOrCallingTheModel() throws Exception {
        assertThat(streamOverHttp("messages", "{\"message\":\"Dame la solución completa\"}")).contains("pista");
        assertThat(store.history(user, session, 0, 10).getFirst().status()).isEqualTo("RECHAZADO");
        verifyNoInteractions(llm, retriever, cas);
    }

    @Test void writesSecurityHeadersBeforeTheSseHandlerCanStart() throws Exception {
        var filter = streamingSecurity.getFilters().stream().filter(HeaderWriterFilter.class::isInstance).findFirst().orElseThrow();
        var servletContext = mvc.getDispatcherServlet().getServletContext();
        var request = new MockHttpServletRequest(servletContext, "POST", "/api/tutor/sessions/1/messages");
        assertThat(streamingSecurity.matches(request)).isTrue();
        assertThat(streamingSecurity.matches(new MockHttpServletRequest(servletContext, "POST",
                "/api/tutor/sessions/1/close"))).isFalse();
        request.setSecure(true);
        filter.doFilter(request, new MockHttpServletResponse(), (incoming, outgoing) -> {
            var response = (jakarta.servlet.http.HttpServletResponse) outgoing;
            assertThat(response.getHeader("Referrer-Policy")).isEqualTo("no-referrer");
            assertThat(response.getHeader("Content-Security-Policy")).contains("object-src 'none'");
            assertThat(response.getHeader("Strict-Transport-Security")).isNotBlank();
        });
    }

    @Test void quotaIsAtomicAcrossSessionsAndReconcilesProviderUsage() throws Exception {
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var both = new java.util.concurrent.CyclicBarrier(2);
            java.util.concurrent.Callable<Optional<TokenBudget.Reservation>> reserve = () -> {
                both.await();
                return budget.reserve(user, 30000, 30000);
            };
            var first = executor.submit(reserve);
            var second = executor.submit(reserve);
            var allowed = List.of(first.get(), second.get()).stream().flatMap(Optional::stream).toList();
            assertThat(allowed).hasSize(1);
            budget.settle(allowed.getFirst(), new LlmGateway.Usage(2, 3));
            assertThat(budget.reserve(user, 99994, 1)).isPresent();
        }
        var jdbc = new org.springframework.jdbc.core.JdbcTemplate(dataSource);
        assertThat(jdbc.queryForObject("SELECT cost_total FROM usage_quotas WHERE user_id = ?",
                java.math.BigDecimal.class, user)).isEqualByComparingTo("0.000008");
        assertThat(streamOverHttp("messages", "{\"message\":\"concepto\"}")).contains("cuota diaria");
        verifyNoInteractions(llm);
    }

    /** @brief Los emisores inmediatos escriben sobre la respuesta real para evitar carreras en MockMvc. */
    private String streamOverHttp(String action, String body) {
        var client = WebTestClient.bindToServer(new ReactorClientHttpConnector(HttpClient.newConnection()))
                .baseUrl("http://127.0.0.1:" + port)
                .responseTimeout(Duration.ofSeconds(35)).build();
        var csrf = client.get().uri("/api/csrf-token").exchange().expectStatus().isOk()
                .expectCookie().exists("XSRF-TOKEN").expectBody().returnResult();
        String value = csrf.getResponseCookies().getFirst("XSRF-TOKEN").getValue();
        User account = users.findById(user).orElseThrow();
        String token = tokens.generateAccessToken(user, account.getEmail(), account.getRole().name());
        byte[] response = client.post().uri("/api/tutor/sessions/{id}/{action}", session, action)
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.TEXT_EVENT_STREAM)
                .cookie("XSRF-TOKEN", value).header("X-XSRF-TOKEN", value)
                .headers(headers -> headers.setBearerAuth(token)).bodyValue(body).exchange()
                .expectStatus().isOk().expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
                .expectHeader().valueEquals("Referrer-Policy", "no-referrer")
                .expectBody().returnResult().getResponseBody();
        return new String(response, StandardCharsets.UTF_8);
    }

    @Test void secureCsrfExchangeAndBrowserHeadersAreEnforced() throws Exception {
        mvc.perform(get("/api/csrf-token").secure(true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.csrfToken").isString())
                .andExpect(header().exists("Strict-Transport-Security"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Content-Security-Policy", org.hamcrest.Matchers.containsString("object-src 'none'")));
        mvc.perform(get("/api/csrf-token").header("Origin", "https://untrusted.example"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk())
                .andExpect(jsonPath("$.components").doesNotExist());
    }
    @Test void chatReservesBeforeSseAndReturnsConflictForActiveRequest() throws Exception {
        store.reserve(user,session,UUID.randomUUID(),"ocupado");
        mvc.perform(post("/api/tutor/sessions/"+session+"/messages").with(actor("ALUMNO")).with(csrfCookie()).contentType("application/json")
                .content("{\"message\":\"hola\",\"requestId\":\""+UUID.randomUUID()+"\"}"))
                .andExpect(status().isConflict());
    }

    @Test void socraticLevelsAndLeakFallbackSurviveReplayAndHistory() throws Exception {
        store.select(user,session,null,"Deriva x^2","2*x");
        when(cas.compare(anyString(),eq("2*x"))).thenReturn(CasEngine.Comparison.UNKNOWN);
        for (String attempt : List.of("3","4","5","6")) {
            when(cas.compare(attempt,"2*x")).thenReturn(CasEngine.Comparison.DIFFERENT);
        }
        when(cas.compare("x+x","2*x")).thenReturn(CasEngine.Comparison.EQUIVALENT);
        var deltas = Sinks.many().unicast().<String>onBackpressureBuffer();
        when(llm.stream(any(),any())).thenReturn(deltas.asFlux(),
                Flux.just("La pendiente es $x", "+x$."));

        UUID initial = UUID.randomUUID();
        // Emite después de que MockMvc complete el despacho inicial y sus cabeceras.
        String feedback = turn("3", initial, () -> {
            deltas.emitNext("La pendiente es $x", Sinks.EmitFailureHandler.FAIL_FAST);
            deltas.emitNext("+x$.", Sinks.EmitFailureHandler.FAIL_FAST);
            deltas.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST);
        });
        assertThat(feedback).contains("concepto").doesNotContain("La pendiente", "x+x", "+x$");
        assertThat(tracked.reply(user, session, initial, "3").blockLast()).contains("concepto");
        assertThat(tracked.reply(user, session, UUID.randomUUID(), "¿Puedes orientarme?").blockLast()).contains("operación");
        assertThat(store.current(user,session).attempts()).isEqualTo(1);
        assertThat(tracked.reply(user, session, UUID.randomUUID(), "4").blockLast()).contains("operación");
        assertThat(tracked.reply(user, session, UUID.randomUUID(), "5").blockLast()).contains("problema más sencillo");
        assertThat(tracked.reply(user, session, UUID.randomUUID(), "6").blockLast()).contains("2*x");

        mvc.perform(get("/api/tutor/sessions/" + session + "/history").with(actor("ALUMNO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].helpLevel").value("REVELACION"))
                .andExpect(jsonPath("$[1].helpLevel").value("EJEMPLO_ANALOGO"))
                .andExpect(jsonPath("$[2].helpLevel").value("PROCEDIMENTAL"))
                .andExpect(jsonPath("$[4].helpLevel").value("CONCEPTUAL"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("x+x"))));
        var prompts = org.mockito.ArgumentCaptor.forClass(Prompt.class);
        verify(llm, times(4)).stream(prompts.capture(), any());
        assertThat(prompts.getAllValues()).allSatisfy(prompt -> assertThat(prompt.constraints())
                .contains("No des la solución").doesNotContain("2*x"));
    }

    @Test void migrationCountsOnlyPreviouslyVerifiableAttempts() {
        var run = store.select(user, session, null, "Deriva x^2", "2*x");
        for (String verdict : List.of("INCORRECTO", "INDETERMINADO", "NO_APLICA")) {
            var request = UUID.randomUUID();
            store.reserve(user, session, request, "Propuesta");
            // El constructor anterior contabilizaba cualquier turno completado.
            store.finish(user, session, request,
                    new ConversationStore.Result("Pista", null, ErrorType.OTRO, "COMPLETADO",
                            0, null, null, 0, false, verdict));
        }
        assertThat(store.current(user, session).attempts()).isEqualTo(3);
        new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(
                new org.springframework.core.io.ClassPathResource("db/migration/V10__verified_exercise_attempts.sql"))
                .execute(dataSource);
        assertThat(store.current(user, session).attempts()).isEqualTo(1);
        assertThat(store.history(user, session, 0, 10)).hasSize(3).allMatch(entry -> entry.runId().equals(run.id()));
    }

    private String turn(String message, UUID id, Runnable emitProviderResponse) throws Exception {
        String body = json.writeValueAsString(Map.of("message", message, "requestId", id));
        var result = mvc.perform(post("/api/tutor/sessions/" + session + "/messages")
                        .with(actor("ALUMNO")).with(csrfCookie()).contentType("application/json").content(body))
                .andExpect(request().asyncStarted()).andReturn();
        emitProviderResponse.run();
        return mvc.perform(asyncDispatch(result)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"type\":\"DONE\"")))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    }
}
