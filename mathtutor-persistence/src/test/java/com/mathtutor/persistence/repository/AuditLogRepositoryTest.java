package com.mathtutor.persistence.repository;


import com.mathtutor.persistence.PersistenceTestApplication;
import com.mathtutor.persistence.PersistenceTestConfig;
import com.mathtutor.persistence.entity.AuditLog;
import com.mathtutor.persistence.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integración para AuditLogRepository.
 */
@SpringBootTest(classes = PersistenceTestApplication.class)
@Import(PersistenceTestConfig.class)
@Transactional

class AuditLogRepositoryTest {

    @Autowired
    private AuditLogRepository auditRepository;

    @Autowired
    private UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void shouldSaveAndFindByActor() {
        // Given
        User user = User.builder().email("audit@test.com").passwordHash("h").role(User.Role.ALUMNO).active(true).build();
        userRepository.save(user);

        AuditLog log1 = AuditLog.builder().action("LOGIN").targetType("USER").targetId(1L).actor(user).build();
        AuditLog log2 = AuditLog.builder().action("LOGOUT").targetType("USER").targetId(1L).actor(user).build();
        auditRepository.save(log1);
        auditRepository.save(log2);

        // When
        List<AuditLog> logs = auditRepository.findByActor(user);

        // Then
        assertThat(logs).hasSize(2);
    }

    @Test
    void shouldFindByAction() {
        // Given
        User user = User.builder().email("audit2@test.com").passwordHash("h").role(User.Role.ALUMNO).active(true).build();
        userRepository.save(user);

        AuditLog log = AuditLog.builder().action("PASSWORD_CHANGE").targetType("USER").targetId(1L).actor(user).build();
        auditRepository.save(log);

        // When
        List<AuditLog> logs = auditRepository.findByAction("PASSWORD_CHANGE");

        // Then
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getAction()).isEqualTo("PASSWORD_CHANGE");
    }

    @Test
    void shouldFindByTargetTypeAndTargetId() {
        // Given
        User user = User.builder().email("audit3@test.com").passwordHash("h").role(User.Role.ALUMNO).active(true).build();
        userRepository.save(user);

        AuditLog log = AuditLog.builder().action("EXERCISE_START").targetType("EXERCISE_RUN").targetId(42L).actor(user).build();
        auditRepository.save(log);

        // When
        List<AuditLog> logs = auditRepository.findByTargetTypeAndTargetId("EXERCISE_RUN", 42L);

        // Then
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getTargetType()).isEqualTo("EXERCISE_RUN");
        assertThat(logs.get(0).getTargetId()).isEqualTo(42L);
    }

    @Test
    void shouldFindByCreatedAtBetween() {
        // Given
        User user = User.builder().email("audit4@test.com").passwordHash("h").role(User.Role.ALUMNO).active(true).build();
        userRepository.save(user);

        Instant now = Instant.now();
        Instant past = now.minusSeconds(3600);

        AuditLog oldLog = AuditLog.builder().action("OLD_ACTION").actor(user).build();
        auditRepository.save(oldLog);
        entityManager.flush();
        // Manually update createdAt via native query to bypass @CreationTimestamp
        entityManager.createNativeQuery("UPDATE audit_logs SET created_at = :ts WHERE id = :id")
                .setParameter("ts", past.minusSeconds(100))
                .setParameter("id", oldLog.getId())
                .executeUpdate();

        AuditLog newLog = AuditLog.builder().action("NEW_ACTION").actor(user).build();
        auditRepository.save(newLog);
        entityManager.flush();
        // Manually update createdAt via native query
        entityManager.createNativeQuery("UPDATE audit_logs SET created_at = :ts WHERE id = :id")
                .setParameter("ts", now.minusSeconds(100))
                .setParameter("id", newLog.getId())
                .executeUpdate();

        // When
        List<AuditLog> logs = auditRepository.findByCreatedAtBetween(past, now);

        // Then
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getAction()).isEqualTo("NEW_ACTION");
    }

    @Test
    void shouldFindAllPaged() {
        // Given
        User user = User.builder().email("audit5@test.com").passwordHash("h").role(User.Role.ALUMNO).active(true).build();
        userRepository.save(user);

        for (int i = 0; i < 5; i++) {
            auditRepository.save(AuditLog.builder().action("ACTION_" + i).actor(user).build());
        }

        // When
        var page = auditRepository.findAll(PageRequest.of(0, 2));

        // Then
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(5);
    }
}
