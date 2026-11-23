package com.mathtutor.application.security;

import com.mathtutor.persistence.entity.AuditLog;
import com.mathtutor.persistence.entity.User;
import com.mathtutor.persistence.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserAdministrationTest {
    private final UserRepository users = mock(UserRepository.class);
    private final PersonalDataStore data = mock(PersonalDataStore.class);
    private final RefreshTokenRepository refresh = mock(RefreshTokenRepository.class);
    private final PasswordResetTokenRepository resets = mock(PasswordResetTokenRepository.class);
    private final AuditLogRepository audit = mock(AuditLogRepository.class);
    private final UserAdministration service = new UserAdministration(users, data, refresh, resets, audit);

    @Test
    void deactivationRevokesBothKindsOfPendingCredentialsAndIsRepeatable() {
        User target = manageableTarget();
        service.deactivate(1, 2);
        service.deactivate(1, 2);
        assertThat(target.isActive()).isFalse();
        verify(refresh).revokeAllForUser(eq(2L), any(Instant.class));
        verify(resets).consumePending(eq(2L), any(Instant.class));
        verify(audit).save(argThat(event -> event.getAction().equals("USER_DEACTIVATED")));
    }

    @Test
    void anonymizationDetachesTargetFromPersonalDataAndAudit() {
        User target = manageableTarget();
        service.anonymize(1, 2);
        verify(data).eraseRelated(2);
        assertThat(target.isActive()).isFalse();
        assertThat(target.getEmail()).startsWith("anon-").doesNotContain("@");
        assertThat(target.getAnonymizedAt()).isNotNull();
        verify(audit).save(argThat(event -> event.getTargetId() == null));
        assertThatThrownBy(() -> service.anonymize(1, 2)).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void roleChangeAuditsOnlyActualTransitionsAndRetainsSessions() {
        User target = manageableTarget();
        service.changeRole(1, 2, User.Role.ALUMNO);
        service.changeRole(1, 2, User.Role.PROFESOR);
        assertThat(target.getRole()).isEqualTo(User.Role.PROFESOR);
        assertThat(target.isActive()).isTrue();
        verify(audit).save(argThat(event -> event.getAction().equals("USER_ROLE_CHANGED")));
        verifyNoInteractions(refresh, resets);
        target.setActive(false);
        assertThatThrownBy(() -> service.changeRole(1, 2, User.Role.ADMIN)).hasMessageContaining("409");
    }

    @Test
    void staleAdminAndSelfMutationAreRejectedBeforeTouchingTheTarget() {
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(User.builder().role(User.Role.PROFESOR).build()));
        assertThatThrownBy(() -> service.changeRole(1, 2, User.Role.ADMIN)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.deactivate(1, 1)).hasMessageContaining("409");
        verify(users, never()).findByIdForUpdate(2L);
        verifyNoInteractions(audit);
    }

    @Test
    void assistedResetRecordsTheSelectedAccountAndRejectsInactiveAccounts() {
        User target = manageableTarget();
        service.requestReset(1, 2);
        verify(audit).save(argThat((AuditLog event) -> event.getTargetId().equals(2L)
                && event.getAction().equals("USER_PASSWORD_RESET_REQUESTED")));
        target.setActive(false);
        assertThatThrownBy(() -> service.requestReset(1, 2)).hasMessageContaining("409");
    }

    private User manageableTarget() {
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(User.builder().role(User.Role.ADMIN).build()));
        User target = User.builder().id(2L).email("alumno@test.es").role(User.Role.ALUMNO).build();
        when(users.findByIdForUpdate(2L)).thenReturn(Optional.of(target));
        return target;
    }
}
