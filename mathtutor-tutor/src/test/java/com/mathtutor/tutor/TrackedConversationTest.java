package com.mathtutor.tutor;

import com.mathtutor.common.dto.*;
import com.mathtutor.common.port.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import java.time.Instant;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class TrackedConversationTest {
    final ConversationStore store=mock(ConversationStore.class);
    final TutorConversation conversation=mock(TutorConversation.class);
    final ExerciseVerification verification=mock(ExerciseVerification.class);
    final TrackedConversation tracked=new TrackedConversation(store,conversation,verification,
            new PedagogicalPolicyImpl(),new ExerciseRecognizer(mock(com.mathtutor.common.port.CasEngine.class)));
    final UUID request=UUID.randomUUID();
    ConversationStore.Entry entry(String status) {
        return new ConversationStore.Entry(1L,request,"hola","guardado","funciones",null,null,"CONCEPTUAL",0,null,null,status,Instant.EPOCH,null);
    }
    void prepare(Flux<String> response) {
        when(store.reserve(1L,2L,request,"hola")).thenReturn(entry("EN_CURSO"));
        when(store.context(1L,2L)).thenReturn(List.of());
        when(verification.verify("hola",null)).thenReturn(new ExerciseVerification.Diagnosis("NO_APLICA",ErrorType.OTRO,"Sin objetivo",0,false));
        when(conversation.prepare(eq(1L),eq(2L),eq("hola"),anyList(),anyString(),isNull())).thenReturn(new TutorConversation.Prepared(response,"tema",false));
    }
    @Test void savesCompleteResponseBeforeCompletion() {
        prepare(Flux.just("res","puesta"));
        StepVerifier.create(tracked.reply(1L,2L,request,"hola")).expectNext("res","puesta").verifyComplete();
        var result=org.mockito.ArgumentCaptor.forClass(ConversationStore.Result.class);
        verify(store).finish(eq(1L),eq(2L),eq(request),result.capture());
        assertThat(result.getValue().response()).isEqualTo("respuesta");
        assertThat(result.getValue().tokensIn()).isNull();
    }
    @Test void replaysWithoutCallingLlm() {
        when(store.reserve(1L,2L,request,"hola")).thenReturn(entry("COMPLETADO"));
        StepVerifier.create(tracked.reply(1L,2L,request,"hola")).expectNext("guardado").verifyComplete();
        verifyNoInteractions(conversation,verification);
    }
    @Test void recordsCancellation() {
        prepare(Flux.never());
        StepVerifier.create(tracked.reply(1L,2L,request,"hola")).thenCancel().verify();
        verify(store).finish(eq(1L),eq(2L),eq(request),argThat(r -> r.status().equals("CANCELADO")));
    }
    @Test void recordsFailureAndPropagatesIt() {
        prepare(Flux.error(new IllegalStateException("proveedor")));
        StepVerifier.create(tracked.reply(1L,2L,request,"hola")).verifyError(IllegalStateException.class);
        verify(store).finish(eq(1L),eq(2L),eq(request),argThat(r -> r.status().equals("ERROR")));
    }
    @Test void boundsHistoryByTurnsAndCharacters() {
        var entries=new ArrayList<ConversationStore.Entry>();
        for (int i=0;i<20;i++) entries.add(entry("COMPLETADO"));
        assertThat(TrackedConversation.boundedHistory(entries)).hasSize(10);
        entries.add(new ConversationStore.Entry(9L,request,"x".repeat(16000),"",null,null,null,null,0,null,null,"COMPLETADO",Instant.EPOCH,null));
        assertThat(TrackedConversation.boundedHistory(entries)).isEmpty();
    }
    @Test void rejectsInvalidInputBeforeReserving() {
        for (String message:new String[]{null,""," ","x".repeat(4001)})
            assertThatThrownBy(() -> tracked.reply(1L,2L,request,message)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tracked.reply(1L,2L,null,"hola")).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(store);
    }
    @Test void storesPreparationFailure() {
        when(store.reserve(1L,2L,request,"hola")).thenReturn(entry("EN_CURSO"));
        when(store.current(1L,2L)).thenThrow(new IllegalStateException("BD"));
        assertThatThrownBy(() -> tracked.reply(1L,2L,request,"hola")).isInstanceOf(IllegalStateException.class);
        verify(store).finish(eq(1L),eq(2L),eq(request),argThat(r -> r.status().equals("ERROR")));
    }
    @Test void rejectedTurnsAndProviderMetricsArePreserved() {
        prepare(Flux.just("reformula"));
        var usage=new java.util.concurrent.atomic.AtomicReference<LlmGateway.Usage>(new LlmGateway.Usage(12,4));
        when(conversation.prepare(eq(1L),eq(2L),eq("hola"),anyList(),anyString(),isNull())).thenReturn(new TutorConversation.Prepared(Flux.just("reformula"),"tema",true,usage));
        tracked.reply(1L,2L,request,"hola").blockLast();
        verify(store).finish(eq(1L),eq(2L),eq(request),argThat(r -> r.status().equals("RECHAZADO") && r.tokensIn()==12 && r.tokensOut()==4));
    }
    @Test void rejectsOversizedProviderResponse() {
        prepare(Flux.just("x".repeat(64001)));
        StepVerifier.create(tracked.reply(1L,2L,request,"hola")).verifyError(IllegalStateException.class);
        verify(store).finish(eq(1L),eq(2L),eq(request),argThat(r -> r.status().equals("ERROR")));
    }
    @Test void anEmptyProviderResponseIsNotACompletedTurn() {
        prepare(Flux.empty());
        StepVerifier.create(tracked.reply(1L,2L,request,"hola")).verifyError(IllegalStateException.class);
        verify(store).finish(eq(1L),eq(2L),eq(request),argThat(r -> r.status().equals("ERROR")));
    }
    @Test void subscribingTwiceCannotCallTheProviderTwice() {
        prepare(Flux.just("respuesta"));
        var stream=tracked.reply(1L,2L,request,"hola");
        stream.blockLast();
        StepVerifier.create(stream).verifyError(com.mathtutor.common.dto.ConversationConflictException.class);
    }
    @Test void totalTurnDeadlineReleasesTheReservation() {
        prepare(Flux.just("inicio").concatWith(Flux.never()));
        StepVerifier.withVirtualTime(() -> tracked.reply(1L,2L,request,"hola"))
                .expectNext("inicio").thenAwait(java.time.Duration.ofSeconds(31)).expectError(IllegalStateException.class).verify(java.time.Duration.ofSeconds(2));
        verify(store).finish(eq(1L),eq(2L),eq(request),argThat(r -> r.status().equals("ERROR")));
    }

    @ParameterizedTest
    @CsvSource({
            "2*x, INCORRECTO, false, REVELACION, true",
            "2*x, INDETERMINADO, false, EJEMPLO_ANALOGO, false",
            "2*x, CORRECTO, false, EJEMPLO_ANALOGO, true",
            "2*x, CORRECTO, true, CONCEPTUAL, true",
            ", NO_APLICA, false, EJEMPLO_ANALOGO, true"
    })
    void recordsOnlyVerifiableAttemptsForExercisesWithReference(
            String reference, String verdict, boolean completed, HelpLevel level, boolean attempted) {
        var run = new ConversationStore.Run(1L, null, "Deriva", reference, "derivadas", "COMENZADO",
                0, 0, List.of(), 3);
        var diagnosis = new ExerciseVerification.Diagnosis(verdict, ErrorType.OTRO, "Pista", 0, completed);
        when(store.reserve(1L, 2L, request, "hola")).thenReturn(entry("EN_CURSO"));
        when(store.current(1L, 2L)).thenReturn(run);
        when(store.context(1L, 2L)).thenReturn(List.of());
        when(verification.verify("hola", run)).thenReturn(diagnosis);
        when(conversation.prepareExercise(eq(1L), eq(2L), eq("hola"), anyList(), eq(run), eq(diagnosis), any()))
                .thenReturn(new TutorConversation.Prepared(Flux.just("Pista"), null, false));

        StepVerifier.create(tracked.reply(1L, 2L, request, "hola")).expectNext("Pista").verifyComplete();
        verify(store).finish(eq(1L), eq(2L), eq(request), argThat(r ->
                r.helpLevel() == level && r.attempted() == attempted && r.topic().equals("derivadas")));
    }

    @ParameterizedTest
    @CsvSource({
            "'  EXPLICA EL SIGNIFICADO DEL LÍMITE.  ', false",
            "'El valor se aproxima por ambos lados.', true"
    })
    void distinguishesProsePracticeFromRestatingThePrompt(String message, boolean attempted) {
        var run = new ConversationStore.Run(1L, 3L, "Explica el significado del límite.", null,
                "limites", "COMENZADO", 0, 0, List.of());
        var diagnosis = new ExerciseVerification.Diagnosis("NO_APLICA", ErrorType.OTRO, "Pista", 0, false);
        when(store.reserve(1L, 2L, request, message)).thenReturn(entry("EN_CURSO"));
        when(store.current(1L, 2L)).thenReturn(run);
        when(store.context(1L, 2L)).thenReturn(List.of());
        when(verification.verify(message, run)).thenReturn(diagnosis);
        when(conversation.prepareExercise(eq(1L), eq(2L), eq(message), anyList(), eq(run), eq(diagnosis), any()))
                .thenReturn(new TutorConversation.Prepared(Flux.just("Pista"), null, false));

        StepVerifier.create(tracked.reply(1L, 2L, request, message)).expectNext("Pista").verifyComplete();
        verify(store).finish(eq(1L), eq(2L), eq(request), argThat(result -> result.attempted() == attempted));
    }

    @Test void terminalRequestsDoNotPrepareOrDiscloseAResponse() {
        for (String status : List.of("ERROR", "CANCELADO")) {
            when(store.reserve(1L, 2L, request, "hola")).thenReturn(entry(status));
            StepVerifier.create(tracked.reply(1L, 2L, request, "hola")).verifyError(ConversationTerminalException.class);
        }
        verifyNoInteractions(conversation, verification);
    }
}
