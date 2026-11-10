package com.mathtutor.auth.adapter;

import com.mathtutor.common.auth.domain.User;
import com.mathtutor.common.auth.port.UserRepositoryPort;
import com.mathtutor.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests para UserRepositoryAdapter (TDD - Red phase primero).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserRepositoryAdapter")
class UserRepositoryAdapterTest {

    @Mock
    private UserRepository userRepository;

    private UserRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new UserRepositoryAdapter(userRepository);
    }

    @Nested
    @DisplayName("findByEmail")
    class FindByEmail {

        @Test
        @DisplayName("Debe retornar usuario si existe")
        void shouldReturnUserIfExists() {
            // Given
            com.mathtutor.persistence.entity.User entity = com.mathtutor.persistence.entity.User.builder()
                    .id(1L)
                    .email("test@example.com")
                    .passwordHash("hash")
                    .role(com.mathtutor.persistence.entity.User.Role.ALUMNO)
                    .active(true)
                    .build();

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(entity));

            // When
            Optional<com.mathtutor.common.auth.domain.User> result = adapter.findByEmail("test@example.com");

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo(1L);
            assertThat(result.get().email()).isEqualTo("test@example.com");
            assertThat(result.get().role()).isEqualTo(com.mathtutor.common.auth.domain.User.Role.ALUMNO);
        }

        @Test
        @DisplayName("Debe retornar empty si no existe")
        void shouldReturnEmptyIfNotExists() {
            // Given
            when(userRepository.findByEmail("none@test.com")).thenReturn(Optional.empty());

            // When
            Optional<com.mathtutor.common.auth.domain.User> result = adapter.findByEmail("none@test.com");

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByEmail")
    class ExistsByEmail {

        @Test
        @DisplayName("Debe retornar true si existe")
        void shouldReturnTrueIfExists() {
            // Given
            when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

            // When
            boolean result = adapter.existsByEmail("test@example.com");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Debe retornar false si no existe")
        void shouldReturnFalseIfNotExists() {
            // Given
            when(userRepository.existsByEmail("none@test.com")).thenReturn(false);

            // When
            boolean result = adapter.existsByEmail("none@test.com");

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("findActiveByEmail")
    class FindActiveByEmail {

        @Test
        @DisplayName("Debe retornar usuario activo si existe")
        void shouldReturnActiveUserIfExists() {
            // Given
            com.mathtutor.persistence.entity.User entity = com.mathtutor.persistence.entity.User.builder()
                    .id(1L)
                    .email("test@example.com")
                    .passwordHash("hash")
                    .role(com.mathtutor.persistence.entity.User.Role.ALUMNO)
                    .active(true)
                    .build();

            when(userRepository.findActiveByEmail("test@example.com")).thenReturn(Optional.of(entity));

            // When
            Optional<com.mathtutor.common.auth.domain.User> result = adapter.findActiveByEmail("test@example.com");

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo(1L);
            assertThat(result.get().active()).isTrue();
        }

        @Test
        @DisplayName("Debe retornar empty si usuario inactivo")
        void shouldReturnEmptyIfUserInactive() {
            // Given
            when(userRepository.findActiveByEmail("inactive@test.com")).thenReturn(Optional.empty());

            // When
            Optional<com.mathtutor.common.auth.domain.User> result = adapter.findActiveByEmail("inactive@test.com");

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("Debe guardar usuario y retornar con ID asignado")
        void shouldSaveUserAndReturnWithAssignedId() {
            // Given
            com.mathtutor.common.auth.domain.User domainUser = com.mathtutor.common.auth.domain.User.create("new@test.com", "hash", com.mathtutor.common.auth.domain.User.Role.ALUMNO);
            com.mathtutor.persistence.entity.User savedEntity = com.mathtutor.persistence.entity.User.builder()
                    .id(42L)
                    .email("new@test.com")
                    .passwordHash("hash")
                    .role(com.mathtutor.persistence.entity.User.Role.ALUMNO)
                    .active(true)
                    .build();

            when(userRepository.save(any(com.mathtutor.persistence.entity.User.class))).thenReturn(savedEntity);

            // When
            com.mathtutor.common.auth.domain.User result = adapter.save(domainUser);

            // Then
            assertThat(result.id()).isEqualTo(42L);
            assertThat(result.email()).isEqualTo("new@test.com");
            assertThat(result.passwordHash()).isEqualTo("hash");
            assertThat(result.role()).isEqualTo(com.mathtutor.common.auth.domain.User.Role.ALUMNO);
            assertThat(result.active()).isTrue();

            verify(userRepository).save(argThat(entity ->
                    entity.getEmail().equals("new@test.com") &&
                    entity.getPasswordHash().equals("hash") &&
                    entity.getRole() == com.mathtutor.persistence.entity.User.Role.ALUMNO &&
                    entity.isActive()
            ));
        }
    }

    @Nested
    @DisplayName("Mapeo de roles")
    class RoleMapping {

        @Test
        @DisplayName("Debe mapear ALUMNO correctamente")
        void shouldMapAlumnoRole() {
            // Given
            com.mathtutor.persistence.entity.User entity = com.mathtutor.persistence.entity.User.builder()
                    .id(1L).email("a@test.com").passwordHash("h").role(com.mathtutor.persistence.entity.User.Role.ALUMNO).active(true).build();
            when(userRepository.findByEmail("a@test.com")).thenReturn(Optional.of(entity));

            // When
            com.mathtutor.common.auth.domain.User result = adapter.findByEmail("a@test.com").orElseThrow();

            // Then
            assertThat(result.role()).isEqualTo(com.mathtutor.common.auth.domain.User.Role.ALUMNO);
        }

        @Test
        @DisplayName("Debe mapear PROFESOR correctamente")
        void shouldMapProfesorRole() {
            // Given
            com.mathtutor.persistence.entity.User entity = com.mathtutor.persistence.entity.User.builder()
                    .id(1L).email("p@test.com").passwordHash("h").role(com.mathtutor.persistence.entity.User.Role.PROFESOR).active(true).build();
            when(userRepository.findByEmail("p@test.com")).thenReturn(Optional.of(entity));

            // When
            com.mathtutor.common.auth.domain.User result = adapter.findByEmail("p@test.com").orElseThrow();

            // Then
            assertThat(result.role()).isEqualTo(com.mathtutor.common.auth.domain.User.Role.PROFESOR);
        }

        @Test
        @DisplayName("Debe mapear ADMIN correctamente")
        void shouldMapAdminRole() {
            // Given
            com.mathtutor.persistence.entity.User entity = com.mathtutor.persistence.entity.User.builder()
                    .id(1L).email("ad@test.com").passwordHash("h").role(com.mathtutor.persistence.entity.User.Role.ADMIN).active(true).build();
            when(userRepository.findByEmail("ad@test.com")).thenReturn(Optional.of(entity));

            // When
            com.mathtutor.common.auth.domain.User result = adapter.findByEmail("ad@test.com").orElseThrow();

            // Then
            assertThat(result.role()).isEqualTo(com.mathtutor.common.auth.domain.User.Role.ADMIN);
        }
    }

    @Nested
    @DisplayName("save() - Actualización de usuario existente")
    class SaveExistingUser {

        @Test
        @DisplayName("Debe actualizar usuario existente cuando ID existe en BD")
        void shouldUpdateExistingUserWhenIdExistsInDb() {
            // Given
            com.mathtutor.common.auth.domain.User domainUser = com.mathtutor.common.auth.domain.User.withId(
                    1L, "existing@test.com", "newhash", com.mathtutor.common.auth.domain.User.Role.PROFESOR, true, null, java.time.Instant.now()
            );

            com.mathtutor.persistence.entity.User existingEntity = com.mathtutor.persistence.entity.User.builder()
                    .id(1L)
                    .email("old@test.com")
                    .passwordHash("oldhash")
                    .role(com.mathtutor.persistence.entity.User.Role.ALUMNO)
                    .active(true)
                    .build();

            com.mathtutor.persistence.entity.User savedEntity = com.mathtutor.persistence.entity.User.builder()
                    .id(1L)
                    .email("existing@test.com")
                    .passwordHash("newhash")
                    .role(com.mathtutor.persistence.entity.User.Role.PROFESOR)
                    .active(true)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(existingEntity));
            when(userRepository.save(any(com.mathtutor.persistence.entity.User.class))).thenReturn(savedEntity);

            // When
            com.mathtutor.common.auth.domain.User result = adapter.save(domainUser);

            // Then
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.email()).isEqualTo("existing@test.com");
            assertThat(result.passwordHash()).isEqualTo("newhash");
            assertThat(result.role()).isEqualTo(com.mathtutor.common.auth.domain.User.Role.PROFESOR);
            assertThat(result.active()).isTrue();

            verify(userRepository).findById(1L);
            verify(userRepository).save(argThat(entity ->
                    entity.getEmail().equals("existing@test.com") &&
                    entity.getPasswordHash().equals("newhash") &&
                    entity.getRole() == com.mathtutor.persistence.entity.User.Role.PROFESOR &&
                    entity.isActive()
            ));
        }

        @Test
        @DisplayName("Debe crear nuevo usuario cuando ID existe pero no está en BD")
        void shouldCreateNewUserWhenIdExistsButNotInDb() {
            // Given
            com.mathtutor.common.auth.domain.User domainUser = com.mathtutor.common.auth.domain.User.withId(
                    99L, "new@test.com", "hash", com.mathtutor.common.auth.domain.User.Role.ALUMNO, true, null, java.time.Instant.now()
            );

            com.mathtutor.persistence.entity.User savedEntity = com.mathtutor.persistence.entity.User.builder()
                    .id(99L)
                    .email("new@test.com")
                    .passwordHash("hash")
                    .role(com.mathtutor.persistence.entity.User.Role.ALUMNO)
                    .active(true)
                    .build();

            when(userRepository.findById(99L)).thenReturn(Optional.empty());
            when(userRepository.save(any(com.mathtutor.persistence.entity.User.class))).thenReturn(savedEntity);

            // When
            com.mathtutor.common.auth.domain.User result = adapter.save(domainUser);

            // Then
            assertThat(result.id()).isEqualTo(99L);
            assertThat(result.email()).isEqualTo("new@test.com");

            verify(userRepository).findById(99L);
            verify(userRepository).save(any(com.mathtutor.persistence.entity.User.class));
        }

        @Test
        @DisplayName("Debe usar createdAt proporcionado cuando no es null")
        void shouldUseProvidedCreatedAtWhenNotNull() {
            // Given
            java.time.Instant customCreatedAt = java.time.Instant.parse("2024-01-01T00:00:00Z");
            com.mathtutor.common.auth.domain.User domainUser = com.mathtutor.common.auth.domain.User.withId(
                    null, "new@test.com", "hash", com.mathtutor.common.auth.domain.User.Role.ALUMNO, true, null, customCreatedAt
            );

            com.mathtutor.persistence.entity.User savedEntity = com.mathtutor.persistence.entity.User.builder()
                    .id(1L)
                    .email("new@test.com")
                    .passwordHash("hash")
                    .role(com.mathtutor.persistence.entity.User.Role.ALUMNO)
                    .active(true)
                    .createdAt(customCreatedAt)
                    .build();

            when(userRepository.save(any(com.mathtutor.persistence.entity.User.class))).thenReturn(savedEntity);

            // When
            com.mathtutor.common.auth.domain.User result = adapter.save(domainUser);

            // Then
            assertThat(result.createdAt()).isEqualTo(customCreatedAt);

            verify(userRepository).save(argThat(entity ->
                    entity.getCreatedAt().equals(customCreatedAt)
            ));
        }
    }
}