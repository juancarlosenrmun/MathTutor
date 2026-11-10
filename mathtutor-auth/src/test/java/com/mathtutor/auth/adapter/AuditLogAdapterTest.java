package com.mathtutor.auth.adapter;

import com.mathtutor.common.auth.domain.DomainEvent;
import com.mathtutor.common.auth.domain.UserLoggedInEvent;
import com.mathtutor.common.auth.domain.UserLoggedOutEvent;
import com.mathtutor.common.auth.domain.UserLoginFailedEvent;
import com.mathtutor.common.auth.domain.UserRegisteredEvent;
import com.mathtutor.common.auth.domain.UserRegistrationFailedEvent;
import com.mathtutor.common.auth.port.AuditLogPort;
import com.mathtutor.persistence.entity.AuditLog;
import com.mathtutor.persistence.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests para AuditLogAdapter.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditLogAdapter")
class AuditLogAdapterTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditLogAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AuditLogAdapter(auditLogRepository);
    }

    @Nested
    @DisplayName("save() - UserRegisteredEvent")
    class SaveUserRegisteredEvent {

        @Test
        @DisplayName("Debe persistir evento de registro de usuario")
        void shouldPersistUserRegisteredEvent() {
            // Given
            UserRegisteredEvent event = new UserRegisteredEvent(1L, "test@example.com", Instant.now());

            // When
            adapter.save(event);

            // Then
            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(captor.capture());

            AuditLog saved = captor.getValue();
            assertThat(saved.getAction()).isEqualTo("UserRegisteredEvent");
            assertThat(saved.getTargetType()).isEqualTo("User");
            assertThat(saved.getTargetId()).isEqualTo(1L);
            assertThat(saved.getCreatedAt()).isEqualTo(event.occurredAt());
        }
    }

    @Nested
    @DisplayName("save() - UserRegistrationFailedEvent")
    class SaveUserRegistrationFailedEvent {

        @Test
        @DisplayName("Debe persistir evento de fallo de registro")
        void shouldPersistUserRegistrationFailedEvent() {
            // Given
            UserRegistrationFailedEvent event = new UserRegistrationFailedEvent("test@example.com", "Email ya existe", Instant.now());

            // When
            adapter.save(event);

            // Then
            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(captor.capture());

            AuditLog saved = captor.getValue();
            assertThat(saved.getAction()).isEqualTo("UserRegistrationFailedEvent");
            assertThat(saved.getTargetType()).isEqualTo("UserRegistration");
            assertThat(saved.getTargetId()).isNull();
            assertThat(saved.getCreatedAt()).isEqualTo(event.occurredAt());
        }
    }

    @Nested
    @DisplayName("save() - UserLoggedInEvent")
    class SaveUserLoggedInEvent {

        @Test
        @DisplayName("Debe persistir evento de inicio de sesión")
        void shouldPersistUserLoggedInEvent() {
            // Given
            UserLoggedInEvent event = new UserLoggedInEvent(1L, "test@example.com", Instant.now(), "192.168.1.1");

            // When
            adapter.save(event);

            // Then
            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(captor.capture());

            AuditLog saved = captor.getValue();
            assertThat(saved.getAction()).isEqualTo("UserLoggedInEvent");
            assertThat(saved.getTargetType()).isEqualTo("User");
            assertThat(saved.getTargetId()).isEqualTo(1L);
            assertThat(saved.getCreatedAt()).isEqualTo(event.occurredAt());
        }
    }

    @Nested
    @DisplayName("save() - UserLoggedOutEvent")
    class SaveUserLoggedOutEvent {

        @Test
        @DisplayName("Debe persistir evento de cierre de sesión")
        void shouldPersistUserLoggedOutEvent() {
            // Given
            UserLoggedOutEvent event = new UserLoggedOutEvent(1L, "test@example.com", Instant.now());

            // When
            adapter.save(event);

            // Then
            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(captor.capture());

            AuditLog saved = captor.getValue();
            assertThat(saved.getAction()).isEqualTo("UserLoggedOutEvent");
            assertThat(saved.getTargetType()).isEqualTo("User");
            assertThat(saved.getTargetId()).isEqualTo(1L);
            assertThat(saved.getCreatedAt()).isEqualTo(event.occurredAt());
        }
    }

    @Nested
    @DisplayName("save() - UserLoginFailedEvent")
    class SaveUserLoginFailedEvent {

        @Test
        @DisplayName("Debe persistir evento de fallo de inicio de sesión")
        void shouldPersistUserLoginFailedEvent() {
            // Given
            UserLoginFailedEvent event = new UserLoginFailedEvent("test@example.com", "Credenciales inválidas", "192.168.1.1", Instant.now());

            // When
            adapter.save(event);

            // Then
            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(captor.capture());

            AuditLog saved = captor.getValue();
            assertThat(saved.getAction()).isEqualTo("UserLoginFailedEvent");
            assertThat(saved.getTargetType()).isEqualTo("UserLogin");
            assertThat(saved.getTargetId()).isNull();
            assertThat(saved.getCreatedAt()).isEqualTo(event.occurredAt());
        }
    }

    @Nested
    @DisplayName("Comportamiento transaccional")
    class TransactionalBehavior {

        @Test
        @DisplayName("Debe llamar a save en el repositorio para cada evento")
        void shouldCallSaveForEachEvent() {
            // Given
            UserRegisteredEvent event1 = new UserRegisteredEvent(1L, "test@example.com", Instant.now());
            UserLoggedInEvent event2 = new UserLoggedInEvent(1L, "test@example.com", Instant.now(), "192.168.1.1");

            // When
            adapter.save(event1);
            adapter.save(event2);

            // Then
            verify(auditLogRepository, times(2)).save(any(AuditLog.class));
        }
    }
}