package com.mathtutor.application;

import com.mathtutor.auth.filter.JwtAuthenticationFilter.AuthenticatedUser;
import com.mathtutor.common.port.*;
import com.mathtutor.common.dto.*;
import com.mathtutor.persistence.entity.*;
import com.mathtutor.persistence.repository.*;
import com.mathtutor.tutor.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import reactor.core.publisher.Flux;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Caja negra con esquema Flyway real, pgvector, autorizaciÃ³n, CSRF y streaming. */
@SpringBootTest(properties={"spring.jpa.hibernate.ddl-auto=validate","spring.jpa.open-in-view=false","jwt.secret=test-secret-key-at-least-256-bits-long-for-hs256-signing"})
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
    @MockBean LlmGateway llm;
    @MockBean RagRetriever retriever;
    @MockBean CasEngine cas;
    Long user,session;
    org.springframework.test.web.servlet.request.RequestPostProcessor actor(String role) {
        return authentication(UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedUser(user,"test@test.es",role),"",List.of(new SimpleGrantedAuthority("ROLE_"+role))));
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
        mvc.perform(get("/api/tutor/progress").with(actor("PROFESOR"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/tutor/progress")).andExpect(status().isUnauthorized());
        Long owner=user; user=users.save(User.builder().email(UUID.randomUUID()+"@test.es").passwordHash("h").active(true).role(User.Role.ALUMNO).build()).getId();
        mvc.perform(get("/api/tutor/sessions/"+session+"/history").with(actor("ALUMNO"))).andExpect(status().isNotFound());
        assertThat(store.sessions(owner,0,100)).hasSize(1);
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
    @Test void freeExerciseNeverExposesReferenceAndRequiresCsrf() throws Exception {
        when(cas.parse("x^2")).thenReturn(new CasEngine.Expr("x^2","x^2",null)); when(cas.derive("x^2","x")).thenReturn("2*x");
        var body="{\"operation\":\"DERIVADA\",\"expression\":\"x^2\"}";
        mvc.perform(post("/api/tutor/sessions/"+session+"/exercises").with(actor("ALUMNO")).contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/tutor/sessions/"+session+"/exercises").with(actor("ALUMNO")).with(csrf()).contentType("application/json").content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.reference").doesNotExist()).andExpect(jsonPath("$.state").value("COMENZADO"));
        when(cas.parse("2*x")).thenReturn(new CasEngine.Expr("2*x","2*x",null));
        when(cas.compare("2*x","2*x")).thenReturn(CasEngine.Comparison.EQUIVALENT);
        tracked.reply(user,session,UUID.randomUUID(),"2*x").blockLast();
        mvc.perform(get("/api/tutor/sessions/"+session+"/exercises").with(actor("ALUMNO"))).andExpect(status().isOk()).andExpect(jsonPath("$[0].state").value("COMPLETADO")).andExpect(jsonPath("$[0].reference").doesNotExist());
    }
    @Test void graphEventsContainValidatedData() throws Exception {
        when(cas.parse("x^2")).thenReturn(new CasEngine.Expr("x^2","x^2",null));
        var result=mvc.perform(post("/api/tutor/sessions/"+session+"/graphics").with(actor("ALUMNO")).with(csrf())
                .contentType("application/json").content("{\"type\":\"CURVA_2D\",\"function\":\"x^2\",\"domain\":{\"min\":-10,\"max\":10,\"steps\":100}}"))
                .andExpect(request().asyncStarted()).andReturn();
        mvc.perform(asyncDispatch(result)).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("GRAPHIC_SPEC")));
    }
    @Test void chatReservesBeforeSseAndReturnsConflictForActiveRequest() throws Exception {
        store.reserve(user,session,UUID.randomUUID(),"ocupado");
        mvc.perform(post("/api/tutor/sessions/"+session+"/messages").with(actor("ALUMNO")).with(csrf()).contentType("application/json")
                .content("{\"message\":\"hola\",\"requestId\":\""+UUID.randomUUID()+"\"}"))
                .andExpect(status().isConflict());
    }
}
