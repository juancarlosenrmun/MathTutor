package com.mathtutor.tutor;

import com.mathtutor.common.dto.TutorSessionInfo;
import com.mathtutor.common.dto.TutorModule;
import com.mathtutor.common.port.TutorSessionStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** UC-12: caminos de creación y particiones de acceso y selección. */
class TutorSessionsTest {
    final TutorSessionStore store = mock(TutorSessionStore.class);
    final TutorSessions sessions = new TutorSessions(store);

    @Test void listsTheAvailableModules() {
        var modules = List.of(new TutorModule("funciones", "Funciones"));
        when(store.modules()).thenReturn(modules);
        assertThat(sessions.modules()).isEqualTo(modules);
    }

    @Test void everySelectionCreatesANewSession() {
        var first = new TutorSessionInfo(1L, "funciones", "ACTIVA");
        var second = new TutorSessionInfo(2L, "funciones", "ACTIVA");
        when(store.create(7L, "funciones")).thenReturn(first, second);
        assertThat(sessions.start(7L, "funciones")).isEqualTo(first);
        assertThat(sessions.start(7L, "funciones")).isEqualTo(second);
        verify(store, times(2)).create(7L, "funciones");
    }

    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {" ", "x"})
    void rejectsAnUnknownModule(String code) {
        assertThatThrownBy(() -> sessions.start(7L, code)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(store);
    }

    @ParameterizedTest @ValueSource(strings = {"funciones", "derivadas-integrales", "metodos-3d"})
    void acceptsAllThreeBlocks(String code) {
        sessions.start(7L, code);
        verify(store).create(7L, code);
    }

    @Test void missingAndForeignSessionsHaveTheSameFailure() {
        when(store.findOwned(1L, 7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> sessions.active(7L, 1L)).isInstanceOf(SessionUnavailableException.class);
    }

    @Test void aClosedSessionCannotChat() {
        when(store.findOwned(1L, 7L)).thenReturn(Optional.of(new TutorSessionInfo(1L, "funciones", "CERRADA")));
        assertThatThrownBy(() -> sessions.active(7L, 1L)).isInstanceOf(SessionUnavailableException.class);
    }

    @Test void anOwnedActiveSessionKeepsItsModule() {
        var info = new TutorSessionInfo(1L, "funciones", "ACTIVA");
        when(store.findOwned(1L, 7L)).thenReturn(Optional.of(info));
        assertThat(sessions.active(7L, 1L)).isEqualTo(info);
    }
}
