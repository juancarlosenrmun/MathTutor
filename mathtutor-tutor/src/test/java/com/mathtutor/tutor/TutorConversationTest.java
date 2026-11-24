package com.mathtutor.tutor;

import com.mathtutor.common.dto.Prompt;
import com.mathtutor.common.dto.TutorSessionInfo;
import com.mathtutor.common.dto.NoteChunk;
import com.mathtutor.common.port.LlmGateway;
import com.mathtutor.common.port.RagRetriever;
import com.mathtutor.common.port.CasEngine;
import com.mathtutor.common.port.TokenBudget;
import com.mathtutor.common.dto.ErrorType;
import com.mathtutor.common.dto.PedagogicalAction;
import com.mathtutor.common.port.ConversationStore.Run;
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
    final CasEngine cas = mock(CasEngine.class);
    final TokenBudget budget = mock(TokenBudget.class);
    final TutorConversation conversation = new TutorConversation(sessions, llm, retriever, cas, budget);
    @org.junit.jupiter.api.BeforeEach
    void allowGeneration() {
        when(budget.reserve(anyLong(), anyInt(), anyInt())).thenAnswer(invocation -> java.util.Optional.of(
                new TokenBudget.Reservation(invocation.getArgument(0), java.time.LocalDate.of(2026, 1, 1),
                        invocation.getArgument(1), invocation.getArgument(2))));
    }
    void active() {
        when(sessions.active(7L, 1L)).thenReturn(new TutorSessionInfo(1L, "funciones", "ACTIVA"));
        when(retriever.retrieve(anyString(), eq("funciones"), isNull())).thenReturn(List.of(
                new NoteChunk(1L, "Definición de función", null, "DEFINICION", "apunte.tex", "funciones", "intro", "1")));
        when(retriever.retrieve(anyString(), eq("funciones"), eq("intro"))).thenReturn(List.of(
                new NoteChunk(1L, "Definición de función", null, "DEFINICION", "apunte.tex", "funciones", "intro", "1")));
    }
    @Test void noRelevantFragmentReturnsOutOfSyllabusWithoutCallingLlm() {
        active(); when(retriever.retrieve(anyString(), eq("funciones"), isNull())).thenReturn(List.of());
        StepVerifier.create(conversation.reply(7L, 1L, "tema ajeno"))
                .expectNext("No encuentro este tema en los apuntes del módulo. Reformula tu pregunta dentro del temario.")
                .verifyComplete();
        verifyNoInteractions(llm);
    }
    @Test void aGenericFunctionMentionDoesNotAdmitAnIntegralInTheFunctionsModule() {
        active(); when(retriever.retrieve(anyString(), eq("funciones"), isNull())).thenReturn(List.of());
        var turn = conversation.prepare(7L, 1L, "Calcula la integral de la función x^5+2", List.of(), "");
        assertThat(turn.rejected()).isTrue();
        StepVerifier.create(turn.response()).expectNextMatches(text -> text.contains("dentro del temario")).verifyComplete();
        verifyNoInteractions(llm);
    }
    @ParameterizedTest
    @ValueSource(strings = {"Sí, por favor.", "¿Y en x=2?", "No lo entiendo", "no lo se", "¡No lo sé!"})
    void shortFollowUpsReuseThePreviousTopic(String message) {
        when(sessions.active(7L, 1L)).thenReturn(new TutorSessionInfo(1L, "funciones", "ACTIVA"));
        when(llm.stream(any(), any())).thenReturn(Flux.just("¿Qué dice la definición?"));
        var history = List.of("Alumno: Explícame la continuidad\nTutor: ¿Quieres ver un ejemplo?");
        var turn = conversation.prepare(7L, 1L, message, history, "", "funciones-continuidad");
        assertThat(turn.topic()).isEqualTo("funciones-continuidad");
        StepVerifier.create(turn.response()).expectNext("¿Qué dice la definición?").verifyComplete();
        verify(retriever).retrieve(message, "funciones", "funciones-continuidad");
        verify(llm).stream(any(), any());
    }

    @Test void anUnanchoredFollowUpDoesNotUseAnotherModuleAsContext() {
        when(sessions.active(7L, 1L)).thenReturn(new TutorSessionInfo(1L, "funciones", "ACTIVA"));
        var turn = conversation.prepare(7L, 1L, "¿Por qué?", List.of("Turno previo"), "", "calculo-diferencial");
        assertThat(turn.rejected()).isTrue();
        verifyNoInteractions(llm);
    }

    @Test void aClosingRequestWithoutAnExerciseExplainsWhenItApplies() {
        when(sessions.active(7L, 1L)).thenReturn(new TutorSessionInfo(1L, "funciones", "ACTIVA"));
        StepVerifier.create(conversation.reply(7L, 1L, "Ejercicio completado."))
                .expectNextMatches(text -> text.contains("después de trabajar un ejercicio")).verifyComplete();
        verifyNoInteractions(retriever, llm, cas);
    }
    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource(value = {
            "funciones|Demuestra que componer dos funciones continuas conserva la continuidad.|qué función se aplica primero",
            "funciones|¿Por qué el conjugado facilita la división de complejos?|una fracción equivalente",
            "derivadas-integrales|Justifica por qué integrar una función impar en límites simétricos tiene ese resultado.|separas la integral",
            "derivadas-integrales|Explica por qué el valor absoluto no es derivable en el origen.|a izquierda y derecha",
            "metodos-3d|Razona cómo cambia el determinante al permutar filas.|distinguir el orden de las filas",
            "metodos-3d|¿Por qué los vectores ortogonales se estudian con el producto escalar?|qué datos geométricos"
    }, delimiter = '|')
    void theoreticalQuestionsUseCourseHintsWithoutRequestingASolution(String module, String message, String clue) {
        when(sessions.active(7L, 1L)).thenReturn(new TutorSessionInfo(1L, module, "ACTIVA"));
        var turn = conversation.prepare(7L, 1L, message, List.of(), "");
        StepVerifier.create(turn.response()).expectNextMatches(text -> text.toLowerCase(java.util.Locale.ROOT)
                .contains(clue.toLowerCase(java.util.Locale.ROOT)) && text.endsWith("?")).verifyComplete();
        assertThat(turn.rejected()).isFalse();
        verifyNoInteractions(retriever, llm, budget);
    }

    @Test void anUncataloguedProofUsesRetrievedContextWithoutPublishingAGeneratedProof() {
        active();
        var turn = conversation.prepare(7L, 1L, "¿Por qué una función continua puede no ser inyectiva?", List.of(), "");
        assertThat(turn.rejected()).isFalse();
        assertThat(turn.topic()).isEqualTo("intro");
        StepVerifier.create(turn.response()).expectNextMatches(text -> text.contains("hipótesis") && text.endsWith("?"))
                .verifyComplete();
        verifyNoInteractions(llm, budget);
    }

    @ParameterizedTest
    @ValueSource(strings = {"¿Por qué el gazpacho se sirve frío?",
            "¿Por qué el producto escalar sirve para estudiar vectores perpendiculares?"})
    void aProofWithoutSupportInTheSelectedModuleIsStillRejected(String message) {
        active();
        when(retriever.retrieve(anyString(), eq("funciones"), isNull())).thenReturn(List.of());
        var turn = conversation.prepare(7L, 1L, message, List.of(), "");
        assertThat(turn.rejected()).isTrue();
        StepVerifier.create(turn.response()).expectNextMatches(text -> text.contains("dentro del temario")).verifyComplete();
        verifyNoInteractions(llm, budget);
    }
    @Test void selectsTheTopMatchingTopicAndDoesNotMixOtherTopicsInPrompt() {
        active();
        when(retriever.retrieve(anyString(), eq("funciones"), isNull())).thenReturn(List.of(
                new NoteChunk(2L, "Continuidad", null, "DEFINICION", "a.tex", "funciones", "continuidad", "1"),
                new NoteChunk(3L, "Números complejos", null, "DEFINICION", "b.tex", "funciones", "complejos", "1")));
        when(retriever.retrieve(anyString(), eq("funciones"), eq("continuidad"))).thenReturn(List.of(
                new NoteChunk(2L, "Continuidad", null, "DEFINICION", "a.tex", "funciones", "continuidad", "1")));
        when(llm.stream(any(),any())).thenReturn(Flux.just("Pista"));
        StepVerifier.create(conversation.reply(7L, 1L, "¿Qué es continuidad?"))
                .expectNext("Pista").verifyComplete();
        var prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(llm).stream(prompt.capture(),any());
        assertThat(prompt.getValue().contextChunks()).containsExactly("Continuidad");
        verify(retriever).retrieve(anyString(), eq("funciones"), eq("continuidad"));
    }
    @Test void missingTopicMetadataUsesOnlyTheModuleFilter() {
        active(); when(retriever.retrieve(anyString(), eq("funciones"), isNull())).thenReturn(List.of(
                new NoteChunk(4L, "Apunte general", null, "OTRO", "a.tex", "funciones", null, "1")));
        when(llm.stream(any(),any())).thenReturn(Flux.just("Pista"));
        StepVerifier.create(conversation.reply(7L, 1L, "pregunta general"))
                .expectNext("Pista").verifyComplete();
        verify(retriever, times(1)).retrieve(anyString(), eq("funciones"), isNull());
    }
    @Test void emptyTopicFilteredResultNeverCallsLlm() {
        active(); when(retriever.retrieve(anyString(), eq("funciones"), eq("intro"))).thenReturn(List.of());
        StepVerifier.create(conversation.reply(7L, 1L, "tema sin coincidencia"))
                .expectNext("No encuentro este tema en los apuntes del módulo. Reformula tu pregunta dentro del temario.")
                .verifyComplete();
        verifyNoInteractions(llm);
    }
    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource(value = {
            "funciones|funciones-continuidad|Estudia la continuidad de f(x)=x^2+7",
            "funciones|funciones-continuidad|f(x)=x^3+5*x",
            "funciones|funciones-continuidad|Resuelve x^2-5=0",
            "derivadas-integrales|calculo-integral|Calcula la integral de x^5+3*x",
            "metodos-3d|matrices|Calcula el determinante de la matriz [[3,7],[2,5]]",
            "metodos-3d|geometria|Calcula e_{2} × e_{1}"
    }, delimiter = '|')
    void newExercisesOnTheSyllabusReachTheLlmWithoutAnIndexedMatch(String module, String topic, String message) {
        when(sessions.active(7L, 1L)).thenReturn(new TutorSessionInfo(1L, module, "ACTIVA"));
        when(llm.stream(any(), any())).thenReturn(Flux.just("¿Qué regla aplicarías primero?"));
        var turn = conversation.prepare(7L, 1L, message, List.of(), "");
        assertThat(turn.rejected()).isFalse();
        assertThat(turn.topic()).isEqualTo(topic);
        StepVerifier.create(turn.response()).expectNext("¿Qué regla aplicarías primero?").verifyComplete();
        var prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(llm).stream(prompt.capture(), any());
        assertThat(prompt.getValue().userPrompt()).isEqualTo(message);
        assertThat(prompt.getValue().contextChunks()).isEmpty();
        verifyNoInteractions(cas);
    }
    @Test void validStudentExpressionAddsDeterministicFormToPrompt() {
        active(); when(cas.parse("x^2")).thenReturn(new CasEngine.Expr("x^2", "Power(x,2)", "Power(x,2)"));
        when(llm.stream(any(),any())).thenReturn(Flux.just("Pista"));
        StepVerifier.create(conversation.reply(7L, 1L, "x^2")).expectNext("Pista").verifyComplete();
        var prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(llm).stream(prompt.capture(),any());
        assertThat(prompt.getValue().userPrompt()).contains("Power(x,2)");
    }
    @Test void invalidStudentExpressionRequestsReformulationWithoutLlm() {
        active(); when(cas.parse("x^")).thenReturn(new CasEngine.Expr("x^", "", ""));
        StepVerifier.create(conversation.reply(7L, 1L, "x^"))
                .expectNext("No puedo interpretar la expresión. Reformúlala con una notación matemática válida.")
                .verifyComplete();
        verifyNoInteractions(llm);
    }
    @ParameterizedTest @ValueSource(strings = {"x+1", "x/2", "x-1", "x*2", "x=2", "sin(x)", "cos(x)"})
    void mathematicalFormsAreParsedWithoutAnotherLlmCall(String message) {
        active(); when(cas.parse(message)).thenReturn(new CasEngine.Expr(message, "AST", "AST"));
        when(llm.stream(any(),any())).thenReturn(Flux.just("Pista"));
        StepVerifier.create(conversation.reply(7L, 1L, message)).expectNext("Pista").verifyComplete();
        verify(cas).parse(message); verify(llm).stream(any(),any());
    }
    @Test void nullParserResultRequestsReformulation() {
        active();
        StepVerifier.create(conversation.reply(7L, 1L, "x+"))
                .expectNext("No puedo interpretar la expresión. Reformúlala con una notación matemática válida.")
                .verifyComplete();
        verifyNoInteractions(llm);
    }
    @Test void nullNormalizedFormRequestsReformulation() {
        active(); when(cas.parse("x+")).thenReturn(new CasEngine.Expr("x+", null, null));
        StepVerifier.create(conversation.reply(7L, 1L, "x+"))
                .expectNext("No puedo interpretar la expresión. Reformúlala con una notación matemática válida.")
                .verifyComplete();
        verifyNoInteractions(llm);
    }
    @Test void longQuestionRemainsNaturalLanguage() {
        active(); when(llm.stream(any(),any())).thenReturn(Flux.just("Pista"));
        StepVerifier.create(conversation.reply(7L, 1L, "a".repeat(201)))
                .expectNext("Pista").verifyComplete();
        verifyNoInteractions(cas);
    }
    @Test void aProfileWithoutCasKeepsOrdinaryConversationAvailable() {
        active(); when(llm.stream(any(),any())).thenReturn(Flux.just("Pista"));
        var withoutCas = new TutorConversation(sessions, llm, retriever, null, budget);
        StepVerifier.create(withoutCas.reply(7L, 1L, "x^2")).expectNext("Pista").verifyComplete();
    }
    @Test void streamsDeltasInOrderWithOneSubscription() {
        active(); when(llm.stream(any(),any())).thenReturn(Flux.just("Una ", "función."));
        var stream = conversation.reply(7L, 1L, "¿Qué es una función?");
        verifyNoInteractions(llm);
        StepVerifier.create(stream).expectNext("Una ", "función.").verifyComplete();
        var prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(llm).stream(prompt.capture(),any());
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
        active(); when(llm.stream(any(),any())).thenReturn(Flux.just("ok"));
        StepVerifier.create(conversation.reply(7L, 1L, "a".repeat(4000))).expectNext("ok").verifyComplete();
    }
    @Test void unauthorizedSessionsNeverCallTheModel() {
        when(sessions.active(7L, 1L)).thenThrow(new SessionUnavailableException());
        assertThatThrownBy(() -> conversation.reply(7L, 1L, "hola")).isInstanceOf(SessionUnavailableException.class);
        verifyNoInteractions(llm);
    }
    @Test void propagatesCancellationWithoutRetrying() {
        active(); var cancelled = new java.util.concurrent.atomic.AtomicBoolean();
        when(llm.stream(any(),any())).thenReturn(Flux.<String>never().doOnCancel(() -> cancelled.set(true)));
        StepVerifier.create(conversation.reply(7L, 1L, "hola")).thenCancel().verify();
        assertThat(cancelled).isTrue(); verify(llm).stream(any(),any());
    }
    @Test void errorsAreNotRetried() {
        active(); when(llm.stream(any(),any())).thenReturn(Flux.error(new IllegalStateException("proveedor")));
        StepVerifier.create(conversation.reply(7L, 1L, "hola"))
                .expectNextMatches(text -> text.contains("Modo solo apuntes") && text.contains("Definición de función"))
                .verifyComplete();
        verify(llm).stream(any(),any());
    }

    @Test void repeatedFailuresOpenTheCircuitWithoutReservingAnotherQuota() {
        active();
        when(llm.stream(any(), any())).thenReturn(Flux.empty());
        for (int turn = 0; turn < 4; turn++) {
            StepVerifier.create(conversation.reply(7L, 1L, "hola"))
                    .expectNextMatches(text -> text.contains("Modo solo apuntes")).verifyComplete();
        }
        verify(llm, times(3)).stream(any(), any());
        verify(budget, times(3)).reserve(anyLong(), anyInt(), anyInt());
    }

    @Test void quotaRefusalRemainsAFailureInsteadOfEnteringNotesMode() {
        active();
        when(budget.reserve(anyLong(), anyInt(), anyInt())).thenReturn(java.util.Optional.empty());
        StepVerifier.create(conversation.reply(7L, 1L, "hola"))
                .verifyError(com.mathtutor.common.dto.TutorQuotaExceededException.class);
        verifyNoInteractions(llm);
    }

    @Test void providerSilenceIsBoundedAndCancelsTheRequest() {
        active();
        var cancelled = new java.util.concurrent.atomic.AtomicBoolean();
        when(llm.stream(any(), any())).thenReturn(Flux.<String>never().doOnCancel(() -> cancelled.set(true)));
        StepVerifier.withVirtualTime(() -> conversation.reply(7L, 1L, "hola"))
                .thenAwait(java.time.Duration.ofSeconds(8))
                .expectNextMatches(text -> text.contains("Modo solo apuntes")).verifyComplete();
        assertThat(cancelled).isTrue();
        verify(llm).stream(any(), any());
    }

    @Test void aProviderFailureKeepsTheHintOnTheActiveDerivabilityExercise() {
        when(sessions.active(7L, 1L)).thenReturn(new TutorSessionInfo(1L, "derivadas-integrales", "ACTIVA"));
        when(llm.stream(any(), any())).thenReturn(Flux.error(new IllegalStateException("proveedor")));
        var run = new Run(1L, null, "DERIVADA: abs(x)", "sign(x)", null, "COMENZADO", 0, 0, List.of());
        var diagnosis = new ExerciseVerification.Diagnosis("NO_APLICA", ErrorType.OTRO, "Razonamiento", 0, false);
        var action = PedagogicalAction.conceptual("¿Qué propiedad aplicarías?");
        StepVerifier.create(conversation.prepareExercise(7L, 1L, "Me queda un cociente", List.of(), run, diagnosis, action).response())
                .expectNextMatches(text -> text.contains("Modo solo apuntes")
                        && text.contains("cociente incremental") && text.contains("por separado"))
                .verifyComplete();
        verify(llm).stream(any(), any());
    }

    @Test void totalDeadlineDiscardsDeltasEvenWhenTheProviderKeepsSending() {
        active();
        var cancelled = new java.util.concurrent.atomic.AtomicBoolean();
        when(llm.stream(any(), any())).thenReturn(Flux.defer(() -> Flux.interval(java.time.Duration.ofSeconds(5)))
                .map(index -> "delta pendiente").doOnCancel(() -> cancelled.set(true)));
        StepVerifier.withVirtualTime(() -> conversation.reply(7L, 1L, "hola"))
                .thenAwait(java.time.Duration.ofSeconds(20))
                .expectNextMatches(text -> text.contains("Modo solo apuntes") && !text.contains("delta pendiente"))
                .verifyComplete();
        assertThat(cancelled).isTrue();
        verify(llm).stream(any(), any());
    }

    @Test void embeddingOutageUsesLocallyIndexedNotesAndSkipsGeneration() {
        active();
        var note = new NoteChunk(1L, "Apunte local", null, "DEFINICION", "a.tex", "funciones", "intro", "1");
        when(retriever.retrieve("pregunta", "funciones", null)).thenThrow(new IllegalStateException("embeddings"));
        when(retriever.retrieveLocal("pregunta", "funciones", null)).thenReturn(List.of(note));
        StepVerifier.create(conversation.reply(7L, 1L, "pregunta"))
                .expectNextMatches(text -> text.contains("Modo solo apuntes") && text.contains("Apunte local"))
                .verifyComplete();
        verifyNoInteractions(llm, budget);
    }

    @Test void unavailableLocalIndexDoesNotPublishProviderErrorsOrCreditAnAttempt() {
        active();
        when(retriever.retrieve(anyString(), anyString(), isNull())).thenThrow(new IllegalStateException("embeddings"));
        when(retriever.retrieveLocal(anyString(), anyString(), isNull())).thenThrow(new IllegalStateException("PostgreSQL"));
        var turn = conversation.prepare(7L, 1L, "pregunta", List.of(), "");
        assertThat(turn.rejected()).isTrue();
        StepVerifier.create(turn.response()).expectNext("**Modo solo apuntes.** No hay un fragmento disponible para esta consulta.")
                .verifyComplete();
        verifyNoInteractions(llm, budget);
    }

    @Test void notesOnlySkipsWorkedSolutionsAndUsesThePedagogicalQuestion() {
        active();
        when(retriever.retrieve("3", "funciones", "derivadas")).thenReturn(List.of(
                new NoteChunk(1L, "Solución final: $2*x$", null, "DEFINICION", "a.tex", "funciones", "derivadas", "1"),
                new NoteChunk(2L, "Una regla del módulo", null, "EJEMPLO", "a.tex", "funciones", "derivadas", "1")));
        when(llm.stream(any(), any())).thenReturn(Flux.just(" "));
        var action = PedagogicalAction.conceptual("¿Qué propiedad se aplica?");
        StepVerifier.create(conversation.prepareExercise(7L, 1L, "3", List.of(), exercise(), incorrect(), action).response())
                .expectNextMatches(text -> text.contains("Modo solo apuntes") && text.endsWith(action.hint())
                        && !text.contains("2*x") && !text.contains("Una regla"))
                .verifyComplete();
    }

    @Test void trustedReferenceAndVerifiedCorrectnessNeverGoToTheProvider() {
        var run = exercise();
        var correct = new ExerciseVerification.Diagnosis("CORRECTO", ErrorType.NINGUNO, "Verificado", 0, true);
        var reveal = PedagogicalAction.revelation("Compara tu intento.");
        StepVerifier.create(conversation.prepareExercise(7L, 1L, "3", List.of(), run, incorrect(), reveal).response())
                .expectNext("Compara tu intento.\n\n$2*x$").verifyComplete();
        StepVerifier.create(conversation.prepareExercise(7L, 1L, "2x", List.of(), run, correct, reveal).response())
                .expectNextMatches(text -> text.contains("verificado") && !text.contains("2x")).verifyComplete();
        verifyNoInteractions(llm, retriever);
    }

    @Test void exerciseUsesItsTopicAndDegradesToALocalQuestionWhenNotesAreMissing() {
        active();
        var action = PedagogicalAction.conceptual("¿Qué propiedad se aplica?");
        when(retriever.retrieve("3", "funciones", "derivadas")).thenReturn(List.of());
        var reply = conversation.prepareExercise(7L, 1L, "3", List.of(), exercise(), incorrect(), action);
        StepVerifier.create(reply.response()).expectNext(action.hint()).verifyComplete();
        assertThat(reply.rejected()).isFalse();
        assertThat(reply.topic()).isEqualTo("derivadas");
        verifyNoInteractions(llm);
    }

    @Test void activeExerciseDoesNotBypassOutOfSyllabusCheck() {
        active();
        var diagnosis = new ExerciseVerification.Diagnosis("INDETERMINADO", ErrorType.OTRO, "Sin comparación", 0, false);
        var reply = conversation.prepareExercise(7L, 1L, "¿Quién ganó el partido?", List.of(),
                exercise(), diagnosis, PedagogicalAction.conceptual("¿Qué propiedad se aplica?"));
        StepVerifier.create(reply.response()).expectNextMatches(text -> text.contains("temario")).verifyComplete();
        assertThat(reply.rejected()).isTrue();
        verifyNoInteractions(llm, budget);
    }

    @Test void analogicalHintAndHistoryReachThePromptButTheTrustedReferenceDoesNot() {
        active();
        when(retriever.retrieve(eq("3"), eq("funciones"), eq("derivadas"))).thenReturn(List.of(
                new NoteChunk(1L, "Una regla del módulo", null, "DEFINICION", "a.tex", "funciones", "derivadas", "1")));
        when(llm.stream(any(), any())).thenReturn(Flux.just("", "¿Puedes aplicar esa regla?"));
        var reply = conversation.prepareExercise(7L, 1L, "3", List.of("Intento anterior"), exercise(), incorrect(),
                PedagogicalPolicyImpl.analogy());
        StepVerifier.create(reply.response()).expectNext("¿Puedes aplicar esa regla?").verifyComplete();
        var prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(llm).stream(prompt.capture(), any());
        assertThat(prompt.getValue().constraints()).contains("EJEMPLO_ANALOGO", "datos distintos");
        assertThat(prompt.getValue().contextChunks()).anyMatch(c -> c.contains("Intento anterior"))
                .anyMatch(c -> c.contains("Revisa el signo")).noneMatch(c -> c.contains("2*x"));
    }

    @Test void bufferedResponsesHaveABoundAndReplaceWorkedNotesBeforePublishing() {
        active();
        when(retriever.retrieve(anyString(), eq("funciones"), isNull())).thenReturn(List.of(
                new NoteChunk(1L, "Solución final: Se sustituye todo el polinomio en la expresión.", null,
                        "OTRO", "a.tex", "funciones", " ", "1")));
        when(llm.stream(any(), any())).thenReturn(Flux.just("Se sustituye todo ", "el polinomio en la expresión."),
                Flux.just("x".repeat(64000), "y"));
        StepVerifier.create(conversation.reply(7L, 1L, "hola"))
                .expectNextMatches(text -> text.startsWith("¿Qué definición")).verifyComplete();
        StepVerifier.create(conversation.reply(7L, 1L, "hola")).verifyError(IllegalStateException.class);
    }

    @Test void aPartialResponseIsNeverPublishedIfTheModelFails() {
        active();
        when(llm.stream(any(), any())).thenReturn(Flux.just("La posible solución empieza por...")
                .concatWith(Flux.error(new IllegalStateException("proveedor desconectado"))));
        StepVerifier.create(conversation.reply(7L, 1L, "hola"))
                .expectNextMatches(text -> text.contains("Modo solo apuntes") && !text.contains("posible solución"))
                .verifyComplete();
        verify(llm).stream(any(), any());
    }

    @Test void unavailableIndexKeepsUnverifiedQueriesAwayFromTheGenerator() {
        active();
        var withoutIndex = new TutorConversation(sessions, llm, null, cas, budget);
        StepVerifier.create(withoutIndex.reply(7L, 1L, "¿Qué es una función?"))
                .expectNext("Los apuntes del módulo no están disponibles.").verifyComplete();
        verifyNoInteractions(llm, budget);
    }

    private static Run exercise() {
        return new Run(1L, 2L, "Deriva x^2", "2*x", "derivadas", "COMENZADO", 0, 0, List.of());
    }

    private static ExerciseVerification.Diagnosis incorrect() {
        return new ExerciseVerification.Diagnosis("INCORRECTO", ErrorType.SIGNO, "Revisa el signo", 0, false);
    }
}
