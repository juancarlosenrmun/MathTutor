package com.mathtutor.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mathtutor.auth.filter.JwtAuthenticationFilter.AuthenticatedUser;
import com.mathtutor.common.dto.NoteChunk;
import com.mathtutor.common.port.RagRetriever;
import com.mathtutor.persistence.entity.User;
import com.mathtutor.persistence.repository.UserRepository;
import com.mathtutor.tutor.TutorSessions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** @brief Contrato UC-24 sobre el banco real, permisos e historial PostgreSQL. */
@SpringBootTest(properties = {"spring.config.import=", "jwt.secret=test-secret-key-at-least-256-bits-long-for-hs256-signing"})
@AutoConfigureMockMvc
@Testcontainers
class PedagogicalProgressionApiTest {
    @Container static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>("pgvector/pgvector:pg16");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DATABASE::getJdbcUrl);
        registry.add("spring.datasource.username", DATABASE::getUsername);
        registry.add("spring.datasource.password", DATABASE::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired TutorSessions sessions;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @MockitoBean RagRetriever retriever;

    @Test
    void followsAllStagesWhileKeepingActiveRunAndNeverReturnsSolutions() throws Exception {
        var owner = account();
        long session = sessions.start(owner.getId(), "funciones").id();
        advance(owner, session).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.type").value("PROCEDIMENTAL"))
                .andExpect(jsonPath("$.statement").isNotEmpty())
                .andExpect(jsonPath("$.reference").doesNotExist())
                .andExpect(jsonPath("$.bankReference").doesNotExist());
        long procedure = activeRun(session);
        reply(owner, session, "0");
        advance(owner, session).andExpect(jsonPath("$.type").value("PROCEDIMENTAL"));
        assertThat(activeRun(session)).isEqualTo(procedure);
        reply(owner, session, "-1");

        advance(owner, session).andExpect(jsonPath("$.type").value("TEORICO"));
        long theory = activeRun(session);
        reply(owner, session, "Define continuidad de f en a.");
        reply(owner, session, "¿Cuál es la capital de Brasil?");
        advance(owner, session).andExpect(jsonPath("$.type").value("TEORICO"));
        assertThat(activeRun(session)).isEqualTo(theory);
        String theoryReply = "El valor de la función coincide con el límite en ese punto.";
        noteFor(theoryReply, "funciones-continuidad", "El valor y el límite determinan la continuidad de una función.");
        reply(owner, session, theoryReply);
        advance(owner, session).andExpect(jsonPath("$.type").value("TEORICO"));
        reply(owner, session, "Ejercicio completado");

        advance(owner, session).andExpect(jsonPath("$.type").value("INTERPRETACION"));
        String interpretation = "Solo admite números reales no negativos.";
        noteFor(interpretation, "numeros-reales-complejos", "Una raíz cuadrada real requiere un radicando mayor o igual a cero.");
        reply(owner, session, interpretation);
        reply(owner, session, "Ejercicio completado");
        advance(owner, session).andExpect(status().isNoContent());
    }

    @Test
    void isolatesLearnerAndModuleAndReadsCompletedWorkFromPreviousSessions() throws Exception {
        var owner = account();
        var other = account();
        long original = sessions.start(owner.getId(), "derivadas-integrales").id();
        advance(owner, original).andExpect(jsonPath("$.type").value("PROCEDIMENTAL"));
        jdbc.update("UPDATE exercise_runs SET state = 'COMPLETADO' WHERE id = ?", activeRun(original));
        jdbc.update("UPDATE tutor_sessions SET status = 'CERRADA' WHERE id = ?", original);
        long resumed = sessions.start(owner.getId(), "derivadas-integrales").id();
        advance(owner, resumed).andExpect(jsonPath("$.type").value("TEORICO"));
        reply(owner, resumed, "Ejercicio completado");
        advance(owner, resumed).andExpect(jsonPath("$.type").value("INTERPRETACION"));
        long otherModule = sessions.start(owner.getId(), "metodos-3d").id();
        advance(owner, otherModule).andExpect(jsonPath("$.type").value("PROCEDIMENTAL"));
        jdbc.update("UPDATE exercise_runs SET state = 'COMPLETADO' WHERE id = ?", activeRun(otherModule));
        advance(owner, otherModule).andExpect(jsonPath("$.type").value("TEORICO"));
        reply(owner, otherModule, "Ejercicio completado");
        advance(owner, otherModule).andExpect(jsonPath("$.type").value("INTERPRETACION"));
        advance(other, resumed).andExpect(status().isNotFound());
        advance(owner, original).andExpect(status().isNotFound());
        mvc.perform(post(path(resumed)).with(actor(owner))).andExpect(status().isForbidden());
    }

    private ResultActions advance(User owner, long session) throws Exception {
        return mvc.perform(post(path(session)).with(actor(owner)).with(csrf()));
    }

    private void reply(User owner, long session, String message) throws Exception {
        var result = mvc.perform(post("/api/tutor/sessions/" + session + "/messages")
                        .with(actor(owner)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("message", message))))
                .andExpect(request().asyncStarted()).andReturn();
        mvc.perform(asyncDispatch(result)).andExpect(status().isOk())
                .andExpect(content().string(containsString("\"type\":\"DONE\"")))
                .andExpect(content().string(not(containsString("\"type\":\"ERROR\""))));
    }

    private void noteFor(String message, String topic, String note) {
        when(retriever.retrieve(message, "funciones", topic))
                .thenReturn(List.of(new NoteChunk(1L, note, null, "DEFINICION", "apuntes.tex", "funciones", topic, "1")));
    }

    private String path(long session) {
        return "/api/tutor/sessions/" + session + "/progression";
    }

    private long activeRun(long session) {
        return jdbc.queryForObject("SELECT id FROM exercise_runs WHERE session_id = ? AND state = 'COMENZADO'",
                Long.class, session);
    }

    private User account() {
        return users.save(User.builder().email(UUID.randomUUID() + "@test.example")
                .passwordHash("!").role(User.Role.ALUMNO).active(true).build());
    }

    private RequestPostProcessor actor(User user) {
        return authentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(user.getId(), user.getEmail(), "ALUMNO"), null,
                List.of(new SimpleGrantedAuthority("ROLE_ALUMNO"))));
    }
}
