package com.mathtutor.tutor;

import com.mathtutor.common.port.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class StudyExercisesTest {
    final ConversationStore store=mock(ConversationStore.class);
    final TutorSessions sessions=mock(TutorSessions.class);
    final CasEngine cas=mock(CasEngine.class);
    final StudyExercises service=new StudyExercises(store,sessions,cas);
    @Test void explicitBankSelectionDoesNotInvokeCas() {
        service.select(1L,2L,3L,null,null);
        verify(store).select(1L,2L,3L,null,null); verifyNoInteractions(cas);
        assertThatThrownBy(() -> service.select(1L,2L,3L,"DERIVADA",null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.select(1L,2L,3L,null,"x")).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void supportedOperationsUseOnlyServerReferences() {
        when(cas.parse("x^2")).thenReturn(new CasEngine.Expr("x^2","AST",null));
        when(cas.derive("x^2","x")).thenReturn("2*x");
        service.select(1L,2L,null,"DERIVADA","x^2");
        verify(store).select(1L,2L,null,"DERIVADA: x^2","2*x");
        service.select(1L,2L,null,"SIMPLIFICAR","x^2");
        verify(store).select(1L,2L,null,"SIMPLIFICAR: x^2","x^2");
    }
    @Test void rejectsInvalidAndUnsupportedInput() {
        for (String expr:new String[]{null,""," ","x".repeat(201)})
            assertThatThrownBy(() -> service.select(1L,2L,null,"DERIVADA",expr)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.select(1L,2L,null,"INTEGRAL","x")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.select(1L,2L,null,null,"x")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.select(1L,2L,null,"DERIVADA","x")).isInstanceOf(IllegalArgumentException.class);
        when(cas.parse("x")).thenReturn(new CasEngine.Expr("x",null,null));
        assertThatThrownBy(() -> service.select(1L,2L,null,"DERIVADA","x")).isInstanceOf(IllegalArgumentException.class);
        when(cas.parse("x")).thenReturn(new CasEngine.Expr("x","",null));
        assertThatThrownBy(() -> service.select(1L,2L,null,"DERIVADA","x")).isInstanceOf(IllegalArgumentException.class);
        when(cas.parse("x")).thenReturn(new CasEngine.Expr("x","x",null));
        assertThatThrownBy(() -> service.select(1L,2L,null,"DERIVADA","x")).isInstanceOf(IllegalArgumentException.class);
        when(cas.derive("x","x")).thenReturn("No se pudo calcular");
        assertThatThrownBy(() -> service.select(1L,2L,null,"DERIVADA","x")).isInstanceOf(IllegalArgumentException.class);
    }
}
