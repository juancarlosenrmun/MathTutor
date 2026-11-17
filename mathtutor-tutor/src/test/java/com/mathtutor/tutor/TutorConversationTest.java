package com.mathtutor.tutor;

import com.mathtutor.common.dto.Prompt;
import com.mathtutor.common.dto.TutorSessionInfo;
import com.mathtutor.common.dto.NoteChunk;
import com.mathtutor.common.port.LlmGateway;
import com.mathtutor.common.port.RagRetriever;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

/** UC-13: caminos, fronteras de entrada y contrato de una llamada por turno. */
class TutorConversationTest {
    final TutorSessions sessions = mock(TutorSessions.class);
    final LlmGateway llm = mock(LlmGateway.class);
    final RagRetriever retriever = mock(RagRetriever.class);
    final TutorConversation conversation = new TutorConversation(sessions, llm, retriever);
    void active() {
        when(sessions.active(7L, 1L)).thenReturn(new TutorSessionInfo(1L, "funciones", "ACTIVA"));
        when(retriever.retrieve(anyString(), eq("funciones"), isNull())).thenReturn(List.of(
                new NoteChunk(1L, "Definición de función", null, "DEFINICION", "apunte.tex", "funciones", "intro", "1")));
    }
    @Test void noRelevantFragmentReturnsOutOfSyllabusWithoutCallingLlm() {
        active(); when(retriever.retrieve(anyString(), eq("funciones"), isNull())).thenReturn(List.of());
        StepVerifier.create(conversation.reply(7L, 1L, "tema ajeno"))
                .expectNext("No encuentro este tema en los apuntes del módulo. Reformula tu pregunta dentro del temario.")
                .verifyComplete();
        verifyNoInteractions(llm);
    }
    @Test void streamsDeltasInOrderWithOneSubscription() {
        active(); when(llm.stream(any())).thenReturn(Flux.just("Una ", "función."));
        var stream = conversation.reply(7L, 1L, "¿Qué es una función?");
        verifyNoInteractions(llm);
        StepVerifier.create(stream).expectNext("Una ", "función.").verifyComplete();
        var prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(llm).stream(prompt.capture());
        assertThat(prompt.getValue().contextChunks()).containsExactly("Definición de función");
        verify(retriever).retrieve(anyString(), eq("funciones"), isNull());
        assertThat(prompt.getValue().systemPrompt()).contains("español", "funciones");
        assertThat(prompt.getValue().userPrompt()).isEqualTo("¿Qué es una función?");
    }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {" ", "\n"})
    void emptyMessagesNeverCallTheModel(String message) {
        assertThatThrownBy(() -> conversation.reply(7L, 1L, message)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(llm, sessions);
    }
    @Test void enforcesThe4000CharacterBoundary() {
        assertThatThrownBy(() -> conversation.reply(7L, 1L, "a".repeat(4001))).isInstanceOf(IllegalArgumentException.class);
        active(); when(llm.stream(any())).thenReturn(Flux.just("ok"));
        StepVerifier.create(conversation.reply(7L, 1L, "a".repeat(4000))).expectNext("ok").verifyComplete();
    }
    @Test void unauthorizedSessionsNeverCallTheModel() {
        when(sessions.active(7L, 1L)).thenThrow(new SessionUnavailableException());
        assertThatThrownBy(() -> conversation.reply(7L, 1L, "hola")).isInstanceOf(SessionUnavailableException.class);
        verifyNoInteractions(llm);
    }
    @Test void propagatesCancellationWithoutRetrying() {
        active(); var cancelled = new java.util.concurrent.atomic.AtomicBoolean();
        when(llm.stream(any())).thenReturn(Flux.<String>never().doOnCancel(() -> cancelled.set(true)));
        StepVerifier.create(conversation.reply(7L, 1L, "hola")).thenCancel().verify();
        assertThat(cancelled).isTrue(); verify(llm).stream(any());
    }
    @Test void errorsAreNotRetried() {
        active(); when(llm.stream(any())).thenReturn(Flux.error(new IllegalStateException("proveedor")));
        StepVerifier.create(conversation.reply(7L, 1L, "hola")).verifyError(IllegalStateException.class);
        verify(llm).stream(any());
    }
}
