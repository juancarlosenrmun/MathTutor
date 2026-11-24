package com.mathtutor.tutor;

import com.mathtutor.common.port.ConversationStore;
import com.mathtutor.common.dto.PedagogicalAction;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** @brief UC-18/20/25. Coordina la política, la reserva y la escritura fuera de la llamada al LLM. */
@Service
public class TrackedConversation {
    private final ConversationStore store;
    private final TutorConversation conversation;
    private final ExerciseVerification verification;
    private final PedagogicalPolicyImpl policy;
    private final ExerciseRecognizer recognizer;

    public TrackedConversation(ConversationStore store, TutorConversation conversation,
                               ExerciseVerification verification, PedagogicalPolicyImpl policy,
                               ExerciseRecognizer recognizer) {
        this.store=store; this.conversation=conversation; this.verification=verification;
        this.policy = policy;
        this.recognizer = recognizer;
    }
    public Flux<String> reply(Long user,Long session,UUID request,String message) {
        if (message==null || message.isBlank() || message.length()>4000 || request==null) throw new IllegalArgumentException("Mensaje o petición no válidos");
        var entry=store.reserve(user,session,request,message);
        if (entry.status().equals("ERROR") || entry.status().equals("CANCELADO"))
            return Flux.error(new com.mathtutor.common.dto.ConversationTerminalException(entry.status()));
        if (!entry.status().equals("EN_CURSO")) return Flux.just(entry.response());
        long started=System.nanoTime();
        var response=new StringBuilder();
        var saved=new AtomicBoolean();
        try {
            if (TutorInputGuard.rejected(message)) {
                store.finish(user, session, request, new ConversationStore.Result(TutorInputGuard.REPLY, null,
                        com.mathtutor.common.dto.ErrorType.OTRO, "RECHAZADO", 0, null, null, 0, false,
                        "NO_APLICA", com.mathtutor.common.dto.HelpLevel.CONCEPTUAL, false));
                return Flux.just(TutorInputGuard.REPLY);
            }
            var current = store.current(user, session);
            var run = current == null ? recognize(user, session, request, message) : current;
            boolean recognized = current == null && run != null;
            var diagnosis=verification.verify(message, recognized ? null : run);
            var action = action(run, diagnosis);
            var recent = store.context(user, session);
            var history = boundedHistory(recent);
            TutorConversation.Prepared prepared;
            if (recognized) {
                prepared = conversation.prepareRecognized(user, session, history, run, diagnosis, action);
            } else if (run == null) {
                String previousTopic = history.isEmpty() ? null : recent.getLast().topic();
                prepared = conversation.prepare(user, session, message, history, "", previousTopic);
            } else {
                prepared = conversation.prepareExercise(user, session, message, history, run, diagnosis, action);
            }
            boolean attempted = run != null && !recognized && isAttempt(message, run, diagnosis);
            java.util.function.Consumer<String> finish=status -> {
                if (saved.get()) return;
                var usage=prepared.usage().get();
                store.finish(user,session,request,new ConversationStore.Result(response.toString(),
                        run!=null && run.topic()!=null ? run.topic() : prepared.topic(),diagnosis.diagnosis(),status,
                        (System.nanoTime()-started)/1_000_000,usage==null ? null : usage.inputTokens(),usage==null ? null : usage.outputTokens(),diagnosis.verifiedSteps(),diagnosis.completed(),diagnosis.verdict(),
                        action.helpLevel(), attempted));
                saved.set(true);
            };
            var subscribed=new AtomicBoolean();
            Flux<String> output=prepared.response().takeUntilOther(reactor.core.publisher.Mono.delay(java.time.Duration.ofSeconds(30))
                    .flatMap(ignored -> reactor.core.publisher.Mono.error(new IllegalStateException("Tiempo de respuesta agotado")))).doOnNext(token -> {
                        if (response.length()+token.length()>64000) throw new IllegalStateException("Respuesta demasiado extensa");
                        response.append(token);
                    })
                    .concatWith(Flux.defer(() -> {
                        if (response.toString().isBlank()) throw new IllegalStateException("Respuesta textual vacía");
                        finish.accept(prepared.rejected() ? "RECHAZADO" : "COMPLETADO"); return Flux.empty();
                    }))
                    .doOnError(error -> finish.accept("ERROR"))
                    .doOnCancel(() -> finish.accept("CANCELADO"));
            return Flux.defer(() -> subscribed.compareAndSet(false,true) ? output
                    : Flux.error(new com.mathtutor.common.dto.ConversationConflictException()));
        } catch (RuntimeException error) {
            store.finish(user,session,request,new ConversationStore.Result("",null,com.mathtutor.common.dto.ErrorType.OTRO,"ERROR",0,null,null,0,false));
            throw error;
        }
    }

    /** @brief Para las etapas de razonamiento, repetir el enunciado no acredita práctica. */
    private boolean isAttempt(String message, ConversationStore.Run run, ExerciseVerification.Diagnosis diagnosis) {
        if (run.reference() == null) {
            return !run.statement().strip().equalsIgnoreCase(message.strip());
        }
        return diagnosis.verdict().equals("CORRECTO") || diagnosis.verdict().equals("INCORRECTO");
    }

    private ConversationStore.Run recognize(Long user, Long session, UUID request, String message) {
        var bank = new ArrayList<ConversationStore.BankExercise>();
        int pageSize = 100;
        List<ConversationStore.BankExercise> batch;
        int page = 0;
        do {
            batch = store.bank(user, session, page++, pageSize);
            bank.addAll(batch);
        } while (batch.size() == pageSize);
        return recognizer.recognize(message, bank)
                .map(id -> store.attachRecognized(user, session, request, id)).orElse(null);
    }

    /** @brief Solo permite mostrar la referencia tras un nuevo intento incorrecto verificable. */
    private PedagogicalAction action(ConversationStore.Run run, ExerciseVerification.Diagnosis diagnosis) {
        if (run == null || diagnosis.completed()) {
            return policy.decide(false, diagnosis.diagnosis(), 0);
        }
        boolean eligible = run.reference() != null && diagnosis.verdict().equals("INCORRECTO");
        return policy.decide(eligible, diagnosis.diagnosis(), run.attempts());
    }
    /** Últimos diez turnos completos; límite adicional de caracteres sin resumir con IA. */
    static List<String> boundedHistory(List<ConversationStore.Entry> entries) {
        var result=new LinkedList<String>(); int remaining=16000;
        for (int i=entries.size()-1;i>=Math.max(0,entries.size()-10);i--) {
            var e=entries.get(i);
            var text="Alumno: "+e.message()+"\nTutor: "+e.response();
            if (text.length()>remaining) break;
            result.addFirst(text); remaining-=text.length();
        }
        return List.copyOf(result);
    }
}
