package com.mathtutor.application;

import com.mathtutor.persistence.entity.User;
import com.mathtutor.persistence.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static com.mathtutor.application.SprintEightApiTest.actor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** @brief Verifica los contratos de uso y auditoría de UC-09 con PostgreSQL. */
@SpringBootTest(properties = {"spring.config.import=", "jwt.secret=test-secret-key-at-least-256-bits-long-for-hs256-signing"})
@AutoConfigureMockMvc
@Testcontainers
class AdminUsageApiTest {
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

    @Test
    void topicUsageCountsCompletedTurnsAndDistinctSessionsWithoutIdentities() throws Exception {
        User admin = account(User.Role.ADMIN);
        User teacher = account(User.Role.PROFESOR);
        User student = account(User.Role.ALUMNO);
        long first = session(student, "funciones");
        long second = session(student, "funciones");
        long other = session(teacher, "funciones");
        turn(first, "limites", "COMPLETADO");
        turn(first, "limites", "COMPLETADO");
        turn(second, "limites", "COMPLETADO");
        turn(other, "limites", "ERROR");
        turn(other, null, "COMPLETADO");
        turn(other, "", "COMPLETADO");

        String path = "/api/admin/usage/topics";
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        mvc.perform(get(path).with(actor(teacher))).andExpect(status().isForbidden());
        mvc.perform(get(path).with(actor(admin)))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].module").value("funciones"))
                .andExpect(jsonPath("$.items[0].topic").value("limites"))
                .andExpect(jsonPath("$.items[0].sessions").value(2))
                .andExpect(jsonPath("$.items[0].turns").value(3))
                .andExpect(jsonPath("$.items[0].userId").doesNotExist())
                .andExpect(jsonPath("$.hasNext").value(false));
        assertThat(audits(admin, student)).isZero();
    }

    @Test
    void individualSessionsArePagedMinimalAndAuditedForEverySuccessfulRead() throws Exception {
        User admin = account(User.Role.ADMIN);
        User student = account(User.Role.ALUMNO);
        User other = account(User.Role.ALUMNO);
        long older = session(student, "funciones");
        long newer = session(student, "funciones");
        session(other, "funciones");
        String path = "/api/admin/users/" + student.getId() + "/sessions";
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        mvc.perform(get(path).with(actor(student))).andExpect(status().isForbidden());
        mvc.perform(get(path + "?size=101").with(actor(admin))).andExpect(status().isBadRequest());
        mvc.perform(get(path + "?page=-1").with(actor(admin))).andExpect(status().isBadRequest());
        assertThat(audits(admin, student)).isZero();

        mvc.perform(get(path + "?size=1").with(actor(admin)))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(newer))
                .andExpect(jsonPath("$.items[0].module").value("funciones"))
                .andExpect(jsonPath("$.items[0].status").value("ACTIVA"))
                .andExpect(jsonPath("$.items[0].summary").doesNotExist())
                .andExpect(jsonPath("$.hasNext").value(true));
        mvc.perform(get(path + "?page=1&size=1").with(actor(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(older))
                .andExpect(jsonPath("$.hasNext").value(false));
        mvc.perform(get(path + "?page=2&size=1").with(actor(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());
        assertThat(audits(admin, student)).isEqualTo(3);
        assertThat(audits(admin, other)).isZero();
    }

    @Test
    void teacherReadsStudentStatisticsWithoutAccountManagement() throws Exception {
        User teacher = account(User.Role.PROFESOR);
        User student = account(User.Role.ALUMNO);
        long session = session(student, "funciones");
        jdbc.update("""
                INSERT INTO exercise_runs (session_id, statement_latex, state, progress, current_level, attempts, verified_steps, started_at, last_activity_at)
                VALUES (?, 'Ejercicio', 'COMPLETADO', 1, 'CONCEPTUAL', 0, 0, now(), now())
                """, session);
        String base = "/api/teacher/statistics/students";
        mvc.perform(get(base).with(actor(teacher))).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", org.hamcrest.Matchers.hasItem(student.getId().intValue())))
                .andExpect(jsonPath("$[*].id", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(teacher.getId().intValue()))));
        mvc.perform(get(base + "/" + student.getId() + "/sessions").with(actor(teacher)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(session))
                .andExpect(jsonPath("$.items[0].completedExercises").value(1));
        mvc.perform(get(base + "/" + teacher.getId() + "/sessions").with(actor(teacher)))
                .andExpect(status().isNotFound());
        mvc.perform(get(base).with(actor(student))).andExpect(status().isForbidden());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/admin/users/" + student.getId() + "/deactivate").with(actor(teacher))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isForbidden());
        assertThat(audits(teacher, student)).isEqualTo(1);
    }

    private User account(User.Role role) {
        return users.save(User.builder().email(UUID.randomUUID() + "@test.es").passwordHash("oculto").role(role).build());
    }

    private long session(User owner, String module) {
        return jdbc.queryForObject("""
                INSERT INTO tutor_sessions (user_id, module_id, started_at, last_activity_at, status, summary)
                SELECT ?, id, now(), now(), 'ACTIVA', 'Información privada' FROM modules WHERE code = ? RETURNING id
                """, Long.class, owner.getId(), module);
    }

    private void turn(long session, String topic, String status) {
        jdbc.update("""
                INSERT INTO conversation_turns (session_id, request_id, message, topic, status, created_at)
                VALUES (?, ?, 'Pregunta privada', ?, ?, now())
                """, session, UUID.randomUUID(), topic, status);
    }

    private long audits(User admin, User target) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM audit_logs WHERE actor_id = ? AND target_id = ?
                    AND target_type = 'USER' AND action = 'USER_SESSIONS_VIEWED'
                """, Long.class, admin.getId(), target.getId());
    }
}
