package com.mathtutor.application.controller;

import com.mathtutor.auth.adapter.TokenServiceAdapter;
import com.mathtutor.common.auth.domain.User;
import com.mathtutor.common.auth.port.UserRepositoryPort;
import com.mathtutor.common.dto.NoteChunk;
import com.mathtutor.common.port.LlmGateway;
import com.mathtutor.common.port.RagRetriever;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.utility.DockerImageName;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

/** UC-12: contrato HTTP, tabla de permisos y persistencia real PostgreSQL/pgvector. */
@SpringBootTest(properties = "spring.config.import=")
@AutoConfigureMockMvc @Testcontainers @DirtiesContext
class TutorApiIntegrationTest {
    @Container static final PostgreSQLContainer<?> DB = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", DB::getJdbcUrl);
        r.add("spring.datasource.username", DB::getUsername);
        r.add("spring.datasource.password", DB::getPassword);
        r.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        r.add("jwt.secret", () -> "clave-de-prueba-del-sprint-dos-mayor-de-treinta-y-dos");
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepositoryPort users;
    @Autowired TokenServiceAdapter tokens;
    @Autowired javax.sql.DataSource datasource;
    @MockBean LlmGateway llm;
    @MockBean RagRetriever notes;
    String token;
    @BeforeEach void user() {
        var user = users.save(User.create(UUID.randomUUID() + "@test.es", "hash", User.Role.ALUMNO));
        token = tokens.generateAccessToken(user.id(), user.email(), "ALUMNO");
        org.mockito.Mockito.when(notes.retrieve(org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.eq("funciones"), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(java.util.List.of(new NoteChunk(1L, "El dominio indica dónde está definida la función.",
                        null, "DEFINICION", "nota.tex", "funciones", null, "1")));
    }
    @Test void teacherUsesOwnTutorAndCannotReadAnotherSession() throws Exception {
        long studentSession = start("funciones");
        var teacher = users.save(User.create(UUID.randomUUID() + "@test.es", "hash", User.Role.PROFESOR));
        token = tokens.generateAccessToken(teacher.id(), teacher.email(), "PROFESOR");
        long own = start("funciones");
        mvc.perform(get("/api/tutor/progress").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(get("/api/tutor/sessions/" + own + "/history").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(get("/api/tutor/sessions/" + studentSession + "/history").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
    long start(String block) throws Exception {
        var response = mvc.perform(post("/api/tutor/sessions").with(csrf())
                        .header("Authorization", "Bearer " + token).contentType("application/json")
                        .content(json.writeValueAsString(java.util.Map.of("moduleCode", block))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.moduleCode").value(block)).andReturn();
        return json.readTree(response.getResponse().getContentAsString()).get("id").asLong();
    }
    @Test void catalogueRequiresAuthenticationAndExposesThreeBlocks() throws Exception {
        mvc.perform(get("/api/tutor/modules")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/tutor/modules").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
    }
    @Test void switchingBlocksCreatesANewSessionAndPreservesThePreviousOne() throws Exception {
        long first = start("funciones"), second = start("derivadas-integrales");
        org.assertj.core.api.Assertions.assertThat(second).isNotEqualTo(first);
        mvc.perform(get("/api/tutor/sessions/" + first).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.moduleCode").value("funciones"));
    }
    @Test void rejectsMissingCsrfAndUnknownOrEmptyBlocks() throws Exception {
        mvc.perform(post("/api/tutor/sessions").header("Authorization", "Bearer " + token)
                .contentType("application/json").content("{\"moduleCode\":\"funciones\"}"))
                .andExpect(status().isForbidden());
        for (String block : new String[]{"", "no-existe", "x".repeat(51)}) {
            mvc.perform(post("/api/tutor/sessions").with(csrf()).header("Authorization", "Bearer " + token)
                    .contentType("application/json").content(json.writeValueAsString(java.util.Map.of("moduleCode", block))))
                    .andExpect(status().isBadRequest());
        }
    }
    @Test void foreignAndUnknownSessionsAreIndistinguishable() throws Exception {
        long id = start("metodos-3d");
        user();
        for (long target : new long[]{id, Long.MAX_VALUE}) {
            mvc.perform(get("/api/tutor/sessions/" + target).header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound()).andExpect(content().string("Sesión no disponible"));
        }
    }
    @Test void teachingRolesCannotOpenStudentSessions() throws Exception {
        for (User.Role role : new User.Role[]{User.Role.ADMIN}) {
            var u = users.save(User.create(UUID.randomUUID() + "@test.es", "hash", role));
            mvc.perform(get("/api/tutor/modules").header("Authorization", "Bearer " +
                    tokens.generateAccessToken(u.id(), u.email(), role.name())))
                    .andExpect(status().isForbidden());
        }
    }

    @Test void streamsDeltasAndCompletionWithNoTransactionAtTheGateway() throws Exception {
        long id = start("funciones");
        org.mockito.Mockito.when(llm.stream(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenAnswer(call -> {
            org.assertj.core.api.Assertions.assertThat(
                    org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            org.assertj.core.api.Assertions.assertThat(((com.zaxxer.hikari.HikariDataSource) datasource)
                    .getHikariPoolMXBean().getActiveConnections()).isZero();
            return reactor.core.publisher.Flux.defer(() -> {
                org.assertj.core.api.Assertions.assertThat(
                        org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
                org.assertj.core.api.Assertions.assertThat(((com.zaxxer.hikari.HikariDataSource) datasource)
                        .getHikariPoolMXBean().getActiveConnections()).isZero();
                return reactor.core.publisher.Flux.just("Una ", "función");
            });
        });
        var result = mvc.perform(post("/api/tutor/sessions/" + id + "/messages").with(csrf())
                        .header("Authorization", "Bearer " + token).contentType("application/json")
                        .content("{\"message\":\"¿Qué es una función?\"}"))
                .andExpect(request().asyncStarted()).andReturn();
        mvc.perform(asyncDispatch(result)).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/event-stream"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"type\":\"TOKEN\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"type\":\"DONE\"")));
        org.mockito.Mockito.verify(llm).stream(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test void providerErrorsStreamRelevantNotesWithoutLeakingTheCauseOrRetrying() throws Exception {
        long id = start("funciones");
        org.mockito.Mockito.when(llm.stream(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(reactor.core.publisher.Flux.error(new IllegalStateException("secreto-del-proveedor")));
        var result = mvc.perform(post("/api/tutor/sessions/" + id + "/messages").with(csrf())
                        .header("Authorization", "Bearer " + token).contentType("application/json")
                        .content("{\"message\":\"hola\"}"))
                .andExpect(request().asyncStarted()).andReturn();
        mvc.perform(asyncDispatch(result)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Modo solo apuntes")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"type\":\"DONE\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("\"type\":\"ERROR\""))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secreto-del-proveedor"))));
        org.mockito.Mockito.verify(llm).stream(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test void invalidAndForeignMessagesNeverCallTheLlm() throws Exception {
        long id = start("funciones");
        for (String message : new String[]{"", " ", "x".repeat(4001)}) {
            mvc.perform(post("/api/tutor/sessions/" + id + "/messages").with(csrf())
                    .header("Authorization", "Bearer " + token).contentType("application/json")
                    .content(json.writeValueAsString(java.util.Map.of("message", message))))
                    .andExpect(status().isBadRequest());
        }
        user();
        mvc.perform(post("/api/tutor/sessions/" + id + "/messages").with(csrf())
                .header("Authorization", "Bearer " + token).contentType("application/json")
                .content("{\"message\":\"hola\"}")).andExpect(status().isNotFound());
        org.mockito.Mockito.verifyNoInteractions(llm);
    }

    @Test void tangentSpecRejectsMissingAndOutOfDomainCoordinates() throws Exception {
        long id = start("metodos-3d");
        String body = """
                {"type":"PLANO_TANGENTE","function":"x+y","domain":{"min":-1,"max":1,"steps":16},
                 "points":[%s]}
                """;
        var path = "/api/tutor/sessions/" + id + "/graphics";
        for (String invalid : java.util.List.of("{\"x\":0,\"y\":1.01,\"z\":0}",
                "{\"x\":0,\"y\":null,\"z\":0}", "{\"x\":0,\"z\":0}")) {
            mvc.perform(post(path).with(csrf()).header("Authorization", "Bearer " + token)
                            .contentType("application/json").content(body.formatted(invalid)))
                    .andExpect(status().isBadRequest());
        }
    }
}
