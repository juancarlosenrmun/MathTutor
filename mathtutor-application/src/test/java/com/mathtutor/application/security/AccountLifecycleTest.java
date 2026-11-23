package com.mathtutor.application.security;

import com.mathtutor.persistence.entity.User;
import com.mathtutor.persistence.repository.PersonalDataStore;
import com.mathtutor.persistence.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AccountLifecycleTest {
    private final UserRepository users = mock(UserRepository.class);
    private final PersonalDataStore data = mock(PersonalDataStore.class);
    private final AccountLifecycle lifecycle = new AccountLifecycle(users, data);

    @Test
    void erasesDependenciesBeforeDeletingAccount() {
        User student = User.builder().id(1L).role(User.Role.ALUMNO).build();
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(student));
        lifecycle.erase(1L);
        var order = inOrder(data, users);
        order.verify(data).lockAccountChanges();
        order.verify(users).findByIdForUpdate(1L);
        order.verify(data).eraseRelated(1L);
        order.verify(users).delete(student);
    }

    @Test
    void lastActiveAdministratorCannotBeRemoved() {
        User admin = User.builder().id(1L).role(User.Role.ADMIN).build();
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(admin));
        when(users.countByActiveTrueAndRole(User.Role.ADMIN)).thenReturn(1L);
        assertThatThrownBy(() -> lifecycle.erase(1L)).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
        verify(data, never()).eraseRelated(1L);
        when(users.countByActiveTrueAndRole(User.Role.ADMIN)).thenReturn(2L);
        lifecycle.erase(1L);
        verify(data).eraseRelated(1L);
    }

    @Test
    void inactiveAccountCannotEraseDataWithAnOldPrincipal() {
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(User.builder().active(false).build()));
        assertThatThrownBy(() -> lifecycle.erase(1L)).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
        verify(data, never()).eraseRelated(1L);
    }
}
