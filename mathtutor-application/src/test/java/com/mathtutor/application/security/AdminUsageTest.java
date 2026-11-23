package com.mathtutor.application.security;

import com.mathtutor.persistence.entity.AuditLog;
import com.mathtutor.persistence.entity.User;
import com.mathtutor.persistence.repository.AdminUsageRepository;
import com.mathtutor.persistence.repository.AuditLogRepository;
import com.mathtutor.persistence.repository.PersonalDataStore;
import com.mathtutor.persistence.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AdminUsageTest {
    private final UserRepository users = mock(UserRepository.class);
    private final PersonalDataStore personalData = mock(PersonalDataStore.class);
    private final AdminUsageRepository usage = mock(AdminUsageRepository.class);
    private final AuditLogRepository audit = mock(AuditLogRepository.class);
    private final AdminUsage service = new AdminUsage(users, personalData, usage, audit);

    @ParameterizedTest
    @CsvSource({"-1,20", "0,0", "0,101"})
    void rejectsInvalidPagesBeforeTakingLocks(int page, int size) {
        assertThatThrownBy(() -> service.sessions(1L, 2L, page, size)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(personalData, users, usage, audit);
    }

    @ParameterizedTest
    @CsvSource({"ADMIN,false", "PROFESOR,true"})
    void verifiesActorBeforeAnyIndividualRead(User.Role role, boolean active) {
        User actor = User.builder().role(role).active(active).build();
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(actor));

        assertThatThrownBy(() -> service.sessions(1L, 2L, 0, 1)).isInstanceOf(AccessDeniedException.class);
        verify(users, never()).findByIdForUpdate(2L);
        verifyNoInteractions(usage, audit);
    }

    @Test
    void rejectsAnonymizedTargetsWithoutRestoringTheirAuditLink() {
        authorize();
        var target = User.builder().id(2L).anonymizedAt(Instant.EPOCH).build();
        when(users.findByIdForUpdate(2L)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.sessions(1L, 2L, 0, 20))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
        verifyNoInteractions(usage, audit);
    }

    @Test
    void readsUnderAccountLockAndAuditsEvenAnEmptyPageOfAnInactiveProfile() {
        User actor = authorize();
        when(users.findByIdForUpdate(2L)).thenReturn(Optional.of(User.builder().active(false).build()));

        var page = service.sessions(1L, 2L, 3, 100);

        assertThat(page.items()).isEmpty();
        assertThat(page.hasNext()).isFalse();
        var ordered = inOrder(personalData, users, usage, audit);
        ordered.verify(personalData).lockAccountChanges();
        ordered.verify(users).findByIdForUpdate(1L);
        ordered.verify(users).findByIdForUpdate(2L);
        ordered.verify(usage).sessions(2L, 300L, 101);
        ordered.verify(audit).save(argThat((AuditLog event) -> event.getActor() == actor
                && event.getTargetType().equals("USER") && event.getTargetId().equals(2L)
                && event.getAction().equals("USER_SESSIONS_VIEWED")));
    }

    @Test
    void aggregatePageOmitsLookaheadAndNeedsNoAccountData() {
        var first = new AdminUsageRepository.TopicUsage("funciones", "limites", 1, 2);
        var second = new AdminUsageRepository.TopicUsage("funciones", "continuidad", 2, 2);
        when(usage.topics(1, 2)).thenReturn(List.of(first, second));

        var page = service.topics(1, 1);

        assertThat(page.hasNext()).isTrue();
        assertThat(page.items()).containsExactly(first);
        verifyNoInteractions(personalData, users, audit);
    }

    private User authorize() {
        User actor = User.builder().id(1L).role(User.Role.ADMIN).build();
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(actor));
        return actor;
    }
}
