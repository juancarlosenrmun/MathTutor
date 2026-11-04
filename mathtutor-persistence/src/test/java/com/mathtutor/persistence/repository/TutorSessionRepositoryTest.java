package com.mathtutor.persistence.repository;


import com.mathtutor.persistence.PersistenceTestApplication;
import com.mathtutor.persistence.PersistenceTestConfig;
import com.mathtutor.persistence.entity.Module;
import com.mathtutor.persistence.entity.TutorSession;
import com.mathtutor.persistence.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import org.springframework.data.domain.PageRequest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integración para TutorSessionRepository.
 */
@SpringBootTest(classes = PersistenceTestApplication.class)
@Import(PersistenceTestConfig.class)
@Transactional

class TutorSessionRepositoryTest {

    @Autowired
    private TutorSessionRepository sessionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void shouldSaveAndFindSessionByUser() {
        // Given
        User user = User.builder().email("session@test.com").passwordHash("h").role(User.Role.ALUMNO).active(true).build();
        userRepository.save(user);

        Module module = Module.builder().code("SESS_MOD").name("Módulo Sesión").build();
        moduleRepository.save(module);

        TutorSession session = TutorSession.builder()
                .user(user)
                .module(module)
                .status(TutorSession.SessionStatus.ACTIVA)
                .build();

        // When
        TutorSession saved = sessionRepository.save(session);
        List<TutorSession> sessions = sessionRepository.findByUser(user);

        // Then
        assertThat(sessions).hasSize(1);
        assertThat(sessions.get(0).getUser().getEmail()).isEqualTo("session@test.com");
        assertThat(sessions.get(0).getStatus()).isEqualTo(TutorSession.SessionStatus.ACTIVA);
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void shouldFindByUserAndStatus() {
        // Given
        User user = User.builder().email("status@test.com").passwordHash("h").role(User.Role.ALUMNO).active(true).build();
        userRepository.save(user);

        Module module = Module.builder().code("SESS_MOD2").name("Módulo 2").build();
        moduleRepository.save(module);

        TutorSession active = TutorSession.builder().user(user).module(module).status(TutorSession.SessionStatus.ACTIVA).build();
        TutorSession closed = TutorSession.builder().user(user).module(module).status(TutorSession.SessionStatus.CERRADA).build();
        sessionRepository.save(active);
        sessionRepository.save(closed);

        // When
        List<TutorSession> activeSessions = sessionRepository.findByUserAndStatus(user, TutorSession.SessionStatus.ACTIVA);
        List<TutorSession> closedSessions = sessionRepository.findByUserAndStatus(user, TutorSession.SessionStatus.CERRADA);

        // Then
        assertThat(activeSessions).hasSize(1);
        assertThat(closedSessions).hasSize(1);
    }

    @Test
    void shouldFindByUserAndStatusOrderByLastActivityDesc() {
        // Given
        User user = User.builder().email("order@test.com").passwordHash("h").role(User.Role.ALUMNO).active(true).build();
        userRepository.save(user);

        Module module = Module.builder().code("SESS_MOD3").name("Módulo 3").build();
        moduleRepository.save(module);

        TutorSession s1 = TutorSession.builder().user(user).module(module).status(TutorSession.SessionStatus.ACTIVA).build();
        TutorSession s2 = TutorSession.builder().user(user).module(module).status(TutorSession.SessionStatus.ACTIVA).build();
        sessionRepository.save(s1);
        sessionRepository.save(s2);

        // When
        List<TutorSession> sessions = sessionRepository.findByUserAndStatusOrderByLastActivityDesc(user, TutorSession.SessionStatus.ACTIVA);

        // Then
        assertThat(sessions).hasSize(2);
        // La más reciente debería estar primero (lastActivityAt más reciente)
        assertThat(sessions.get(0).getLastActivityAt()).isAfterOrEqualTo(sessions.get(1).getLastActivityAt());
    }

    @Test
    void shouldFindByStatus() {
        // Given
        User user1 = User.builder().email("u1@test.com").passwordHash("h").role(User.Role.ALUMNO).active(true).build();
        User user2 = User.builder().email("u2@test.com").passwordHash("h").role(User.Role.ALUMNO).active(true).build();
        userRepository.save(user1);
        userRepository.save(user2);

        Module module = Module.builder().code("SESS_MOD4").name("Módulo 4").build();
        moduleRepository.save(module);

        TutorSession active1 = TutorSession.builder().user(user1).module(module).status(TutorSession.SessionStatus.ACTIVA).build();
        TutorSession active2 = TutorSession.builder().user(user2).module(module).status(TutorSession.SessionStatus.ACTIVA).build();
        TutorSession closed = TutorSession.builder().user(user1).module(module).status(TutorSession.SessionStatus.CERRADA).build();
        sessionRepository.save(active1);
        sessionRepository.save(active2);
        sessionRepository.save(closed);

        // When
        List<TutorSession> active = sessionRepository.findByStatus(TutorSession.SessionStatus.ACTIVA);
        List<TutorSession> closedSessions = sessionRepository.findByStatus(TutorSession.SessionStatus.CERRADA);

        // Then
        assertThat(active).hasSize(2);
        assertThat(closedSessions).hasSize(1);
    }

    @Test
    void shouldFindByLastActivityAtBefore() {
        // Given
        User user = User.builder().email("old@test.com").passwordHash("h").role(User.Role.ALUMNO).active(true).build();
        userRepository.save(user);

        Module module = Module.builder().code("SESS_MOD5").name("Módulo 5").build();
        moduleRepository.save(module);

        Instant now = Instant.now();
        Instant threshold = now.minusSeconds(43200); // 12 horas

        TutorSession oldSession = TutorSession.builder().user(user).module(module).status(TutorSession.SessionStatus.ACTIVA).build();
        sessionRepository.save(oldSession);
        entityManager.flush();
        // Manually update lastActivityAt via native query to bypass @UpdateTimestamp
        entityManager.createNativeQuery("UPDATE tutor_sessions SET last_activity_at = :ts WHERE id = :id")
                .setParameter("ts", now.minusSeconds(86400))
                .setParameter("id", oldSession.getId())
                .executeUpdate();

        TutorSession newSession = TutorSession.builder().user(user).module(module).status(TutorSession.SessionStatus.ACTIVA).build();
        sessionRepository.save(newSession);
        entityManager.flush();
        // Manually update lastActivityAt via native query
        entityManager.createNativeQuery("UPDATE tutor_sessions SET last_activity_at = :ts WHERE id = :id")
                .setParameter("ts", now.minusSeconds(21600))
                .setParameter("id", newSession.getId())
                .executeUpdate();

        // When
        List<TutorSession> oldSessions = sessionRepository.findByLastActivityAtBefore(threshold);

        // Then
        assertThat(oldSessions).hasSize(1);
        assertThat(oldSessions.get(0).getId()).isEqualTo(oldSession.getId());
    }
}
