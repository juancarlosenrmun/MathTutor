package com.mathtutor.application;

import com.mathtutor.auth.filter.JwtAuthenticationFilter.AuthenticatedUser;
import com.mathtutor.auth.adapter.TokenServiceAdapter;
import com.mathtutor.application.security.PasswordResetMail;
import com.mathtutor.common.port.TokenBudget;
import com.mathtutor.common.port.TutorSessionStore;
import com.mathtutor.persistence.entity.User;
import com.mathtutor.persistence.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** @brief Contratos HTTP del sprint sobre PostgreSQL y Flyway. */
@SpringBootTest(properties = {"spring.config.import=", "jwt.secret=test-secret-key-at-least-256-bits-long-for-hs256-signing"})
@AutoConfigureMockMvc
@Testcontainers
class SprintEightApiTest {
    @Container static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>("pgvector/pgvector:pg16");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DATABASE::getJdbcUrl);
        registry.add("spring.datasource.username", DATABASE::getUsername);
        registry.add("spring.datasource.password", DATABASE::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired JdbcTemplate jdbc;
    @Autowired TokenServiceAdapter jwt;
    @Autowired TokenBudget budget;
    @Autowired TutorSessionStore sessions;
    @MockitoBean PasswordResetMail mail;

    @Test
    void exportIsScopedAndErasureRequiresCsrf() throws Exception {
        User owner = account(User.Role.ALUMNO);
        User other = account(User.Role.ALUMNO);
        var reservation = budget.reserve(owner.getId(), 10, 20).orElseThrow();
        Long session = jdbc.queryForObject("""
                INSERT INTO tutor_sessions (user_id, started_at, last_activity_at, status)
                VALUES (?, now(), now(), 'ACTIVA') RETURNING id
                """, Long.class, owner.getId());
        jdbc.update("""
                INSERT INTO conversation_turns (session_id, request_id, message, status, created_at)
                VALUES (?, ?, 'Mi pregunta privada', 'COMPLETADO', now())
                """, session, UUID.randomUUID());
        jdbc.update("INSERT INTO audit_logs (action, actor_id, target_id, target_type) VALUES ('PRUEBA', ?, ?, 'USER')",
                other.getId(), owner.getId());
        mvc.perform(get("/api/privacy/export")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/privacy/export").with(actor(owner)))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.profile.email").value(owner.getEmail()))
                .andExpect(jsonPath("$.profile.password_hash").doesNotExist())
                .andExpect(jsonPath("$.sessions[0].id").value(session))
                .andExpect(jsonPath("$.conversations[0].message").value("Mi pregunta privada"))
                .andExpect(jsonPath("$.conversations[0].request_id").doesNotExist())
                .andExpect(jsonPath("$.accountEvents[0].actor_id").doesNotExist());
        mvc.perform(get("/api/privacy/export").with(actor(other)))
                .andExpect(jsonPath("$.sessions").isEmpty())
                .andExpect(jsonPath("$.conversations").isEmpty());
        mvc.perform(delete("/api/privacy/account").with(actor(owner))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/privacy/account").with(actor(owner)).with(csrf())).andExpect(status().isNoContent());
        assertThat(users.findById(owner.getId())).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM conversation_turns WHERE session_id = ?", Integer.class, session))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT target_id FROM audit_logs WHERE action = 'PRUEBA'", Long.class)).isNull();
        budget.settle(reservation, null);
        assertThat(users.findById(other.getId())).isPresent();
    }

    @Test
    void roleChangesTakeEffectWithTheSameAccessToken() throws Exception {
        User administrator = account(User.Role.ADMIN);
        User student = account(User.Role.ALUMNO);
        String token = jwt.generateAccessToken(student.getId(), student.getEmail(), "ALUMNO");
        String path = "/api/admin/users/" + student.getId() + "/role";
        mvc.perform(put(path).with(actor(student)).with(csrf()).contentType("application/json")
                .content("{\"role\":\"ADMIN\"}")).andExpect(status().isForbidden());
        mvc.perform(put(path).with(actor(administrator)).with(csrf()).contentType("application/json")
                .content("{\"role\":\"PROFESOR\"}")).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.role").value("PROFESOR"));
    }

    @Test
    void administratorDeactivatesAndAnonymizesAnAccount() throws Exception {
        User administrator = account(User.Role.ADMIN);
        User student = account(User.Role.ALUMNO);
        String token = jwt.generateAccessToken(student.getId(), student.getEmail(), "ALUMNO");
        String path = "/api/admin/users/" + student.getId();
        mvc.perform(get("/api/admin/users").with(actor(student))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/users").with(actor(administrator))).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
        mvc.perform(put(path + "/deactivate").with(actor(administrator)).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        mvc.perform(put(path + "/anonymize").with(actor(administrator)).with(csrf())).andExpect(status().isNoContent());
        assertThat(users.findByEmail(student.getEmail())).isEmpty();
        assertThat(users.findById(student.getId()).orElseThrow().getAnonymizedAt()).isNotNull();
        assertThat(budget.reserve(student.getId(), 10, 20)).isEmpty();
        assertThatThrownBy(() -> sessions.create(student.getId(), "funciones")).hasMessageContaining("Cuenta no disponible");
    }

    @Test
    void resetQueuesOnlyTheSelectedActiveAccount() throws Exception {
        User administrator = account(User.Role.ADMIN);
        User student = account(User.Role.ALUMNO);
        String path = "/api/admin/users/" + student.getId() + "/password-reset";
        mvc.perform(post(path).with(actor(student)).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post(path).with(actor(administrator))).andExpect(status().isForbidden());
        mvc.perform(post(path).with(actor(administrator)).with(csrf())).andExpect(status().isServiceUnavailable());
        when(mail.enqueue(student.getId())).thenReturn(true);
        mvc.perform(post(path).with(actor(administrator)).with(csrf())).andExpect(status().isAccepted())
                .andExpect(content().string(""));
        verify(mail, times(2)).enqueue(student.getId());
    }

    @Test
    void teacherSeesOnlyAggregateStatesAndAbandonmentPhases() throws Exception {
        User teacher = account(User.Role.PROFESOR);
        User student = account(User.Role.ALUMNO);
        Long session = jdbc.queryForObject("""
                INSERT INTO tutor_sessions (user_id, module_id, started_at, last_activity_at, status)
                SELECT ?, id, now(), now(), 'ACTIVA' FROM modules WHERE code = 'funciones' RETURNING id
                """, Long.class, student.getId());
        jdbc.update("""
                INSERT INTO exercise_runs (session_id, statement_latex, state, progress, current_level, attempts,
                    started_at, last_activity_at, reference_solution)
                SELECT ?, 'texto privado', state, progress, level, 0, now() - age, now() - age, solution
                FROM (VALUES ('COMPLETADO', 1.0, 'CONCEPTUAL', interval '1 hour', 'CAS'),
                    ('COMENZADO', 0.0, 'CONCEPTUAL', interval '29 minutes', 'CAS'),
                    ('COMENZADO', 0.49, 'CONCEPTUAL', interval '31 minutes', 'CAS'),
                    ('ABANDONADO', 0.5, 'CONCEPTUAL', interval '1 minute', 'CAS'),
                    ('ABANDONADO', 0.0, 'EJEMPLO_ANALOGO', interval '1 minute', NULL))
                    AS sample(state, progress, level, age, solution)
                """, session);
        mvc.perform(get("/api/teacher/statistics").with(actor(student))).andExpect(status().isForbidden());
        mvc.perform(get("/api/teacher/statistics?module=funciones").with(actor(teacher)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].started").value(5))
                .andExpect(jsonPath("$[0].completed").value(1)).andExpect(jsonPath("$[0].abandoned").value(3))
                .andExpect(jsonPath("$[0].abandonedEarly").value(1)).andExpect(jsonPath("$[0].abandonedNearSolution").value(2))
                .andExpect(jsonPath("$[0].active").value(1))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("texto privado"))));
    }

    User account(User.Role role) {
        return users.save(User.builder().email(UUID.randomUUID() + "@test.es").passwordHash("oculto").role(role).build());
    }

    static RequestPostProcessor actor(User user) {
        String role = user.getRole().name();
        return authentication(UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUser(user.getId(), user.getEmail(), role), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
}
