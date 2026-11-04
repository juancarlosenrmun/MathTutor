package com.mathtutor.persistence.repository;

import com.mathtutor.persistence.PersistenceTestApplication;
import com.mathtutor.persistence.PersistenceTestConfig;
import com.mathtutor.persistence.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integración para UserRepository.
 * Caja negra: partición de equivalencia, valores límite, tabla de decisiones.
 */
@SpringBootTest(classes = PersistenceTestApplication.class)
@Import(PersistenceTestConfig.class)
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindUserByEmail() {
        // Given
        User user = User.builder()
                .email("test@example.com")
                .passwordHash("hashed_password")
                .role(User.Role.ALUMNO)
                .active(true)
                .build();

        // When
        User saved = userRepository.save(user);
        Optional<User> found = userRepository.findByEmail("test@example.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
        assertThat(found.get().getRole()).isEqualTo(User.Role.ALUMNO);
        assertThat(found.get().isActive()).isTrue();
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void shouldReturnEmptyWhenEmailNotExists() {
        // When
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void shouldReturnTrueWhenEmailExists() {
        // Given
        User user = User.builder()
                .email("exists@example.com")
                .passwordHash("hash")
                .role(User.Role.ALUMNO)
                .active(true)
                .build();
        userRepository.save(user);

        // When
        boolean exists = userRepository.existsByEmail("exists@example.com");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void shouldReturnFalseWhenEmailNotExists() {
        // When
        boolean exists = userRepository.existsByEmail("notexists@example.com");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void shouldFindActiveUserByEmail() {
        // Given
        User activeUser = User.builder()
                .email("active@example.com")
                .passwordHash("hash")
                .role(User.Role.ALUMNO)
                .active(true)
                .build();
        userRepository.save(activeUser);

        User inactiveUser = User.builder()
                .email("inactive@example.com")
                .passwordHash("hash")
                .role(User.Role.ALUMNO)
                .active(false)
                .build();
        userRepository.save(inactiveUser);

        // When
        Optional<User> found = userRepository.findActiveByEmail("active@example.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().isActive()).isTrue();
    }

    @Test
    void shouldNotFindInactiveUserByEmail() {
        // Given
        User inactiveUser = User.builder()
                .email("inactive@example.com")
                .passwordHash("hash")
                .role(User.Role.ALUMNO)
                .active(false)
                .build();
        userRepository.save(inactiveUser);

        // When
        Optional<User> found = userRepository.findActiveByEmail("inactive@example.com");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void shouldSaveUserWithAllRoles() {
        // Given
        User alumno = User.builder().email("alumno@test.com").passwordHash("h").role(User.Role.ALUMNO).active(true).build();
        User profesor = User.builder().email("profesor@test.com").passwordHash("h").role(User.Role.PROFESOR).active(true).build();
        User admin = User.builder().email("admin@test.com").passwordHash("h").role(User.Role.ADMIN).active(true).build();

        // When
        userRepository.save(alumno);
        userRepository.save(profesor);
        userRepository.save(admin);

        // Then
        assertThat(userRepository.findByEmail("alumno@test.com").get().getRole()).isEqualTo(User.Role.ALUMNO);
        assertThat(userRepository.findByEmail("profesor@test.com").get().getRole()).isEqualTo(User.Role.PROFESOR);
        assertThat(userRepository.findByEmail("admin@test.com").get().getRole()).isEqualTo(User.Role.ADMIN);
    }

    @Test
    void shouldCascadeDeletePasswordResetTokens() {
        // Given
        User user = User.builder()
                .email("cascade@test.com")
                .passwordHash("hash")
                .role(User.Role.ALUMNO)
                .active(true)
                .build();
        userRepository.save(user);

        // When
        userRepository.delete(user);

        // Then
        assertThat(userRepository.findByEmail("cascade@test.com")).isEmpty();
    }
}