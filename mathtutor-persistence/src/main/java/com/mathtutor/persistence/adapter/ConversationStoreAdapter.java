package com.mathtutor.persistence.adapter;

import com.mathtutor.common.dto.ConversationConflictException;
import com.mathtutor.common.dto.ConversationUnavailableException;
import com.mathtutor.common.dto.ErrorType;
import com.mathtutor.common.port.ConversationStore;
import com.mathtutor.persistence.entity.*;
import com.mathtutor.persistence.repository.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.time.Instant;
import java.util.*;

/** UC-11/20/25: instantáneas independientes de JPA y reservas bajo bloqueo corto. */
@Component @Transactional(readOnly = true)
public class ConversationStoreAdapter implements ConversationStore {
    private final TutorSessionRepository sessions;
    private final ConversationTurnRepository turns;
    private final ExerciseRunRepository runs;
    private final ExerciseRepository exercises;
    public ConversationStoreAdapter(TutorSessionRepository sessions, ConversationTurnRepository turns,
                                    ExerciseRunRepository runs, ExerciseRepository exercises) {
        this.sessions=sessions; this.turns=turns; this.runs=runs; this.exercises=exercises;
    }
    private TutorSession owned(Long user, Long session) {
        return sessions.findByIdAndUserId(session,user).orElseThrow(ConversationUnavailableException::new);
    }
    private TutorSession locked(Long user, Long session) {
        var s = sessions.lockOwned(session,user).orElseThrow(ConversationUnavailableException::new);
        if (s.getStatus()!=TutorSession.SessionStatus.ACTIVA) throw new ConversationUnavailableException();
        return s;
    }
    private void available(Long session) {
        var now = Instant.now();
        for (var t: turns.findBySessionIdAndStatus(session,"EN_CURSO")) {
            if (t.getCreatedAt().isAfter(now.minusSeconds(120))) throw new ConversationConflictException();
            t.setStatus("ERROR"); t.setResponse("La petición expiró. Inicia un turno nuevo.");
        }
        turns.flush();
    }
    @Override @Transactional public Entry reserve(Long user, Long session, UUID request, String message) {
        var s=locked(user,session);
        var existing=turns.findBySessionIdAndRequestId(session,request);
        if (existing.isPresent()) {
            var t=existing.get();
            if (!t.getMessage().equals(message) || t.getStatus().equals("EN_CURSO"))
                throw new ConversationConflictException();
            return entry(t);
        }
        available(session);
        var t=new ConversationTurn(); t.setSession(s); t.setRequestId(request); t.setMessage(message);
        t.setStatus("EN_CURSO"); t.setCreatedAt(Instant.now()); t.setHelpLevel("CONCEPTUAL");
        t.setExerciseRun(activeRun(session).orElse(null));
        return entry(turns.saveAndFlush(t));
    }
    @Override @Transactional public void finish(Long user, Long session, UUID request, Result result) {
        var s=locked(user,session);
        var t=turns.findBySessionIdAndRequestId(session,request).orElseThrow(ConversationConflictException::new);
        if (!t.getStatus().equals("EN_CURSO")) return;
        t.setResponse(result.response()); t.setTopic(result.topic()); t.setDiagnosis(result.diagnosis().name());
        t.setVerdict(result.verdict());
        t.setHelpLevel(result.helpLevel().name());
        t.setStatus(result.status()); t.setLatencyMs(result.latencyMs());
        t.setTokensIn(result.tokensIn()); t.setTokensOut(result.tokensOut());
        s.setLastActivityAt(Instant.now());
        var run=t.getExerciseRun();
        if (run!=null && result.status().equals("COMPLETADO")) {
            if (result.attempted()) run.setAttempts(run.getAttempts() + 1);
            run.setCurrentLevel(ExerciseRun.HelpLevel.valueOf(result.helpLevel().name()));
            run.setVerifiedSteps(Math.max(run.getVerifiedSteps(), result.verifiedSteps()));
            int count=run.getExercise()==null ? 0 : run.getExercise().getCanonicalSteps().size();
            run.setProgress(result.completed() ? 1 : (count==0 ? 0 : (double)run.getVerifiedSteps()/count));
            if (result.completed()) { run.setState(ExerciseRun.RunState.COMPLETADO); run.setEndedAt(Instant.now()); }
        }
    }
    @Override public List<Entry> context(Long user, Long session) {
        owned(user,session);
        var recent=new ArrayList<>(turns.findBySessionIdAndStatus(session,"COMPLETADO",page(0,10,"createdAt")).stream().map(this::entry).toList());
        Collections.reverse(recent); return recent;
    }
    @Override public List<Entry> history(Long user, Long session,int page,int size) {
        owned(user,session); return turns.findBySessionId(session,page(page,size,"createdAt")).stream().map(this::entry).toList();
    }
    @Override public List<Session> sessions(Long user,int page,int size) {
        return sessions.findByUserId(user,page(page,size,"startedAt")).stream().map(s -> new Session(s.getId(),s.getModule().getCode(),s.getStatus().name(),s.getStartedAt(),s.getLastActivityAt())).toList();
    }
    @Override public List<Run> runs(Long user,Long session,int page,int size) {
        owned(user,session); return runs.findBySessionId(session,page(page,size,"startedAt")).stream().map(this::run).toList();
    }
    @Override public Run current(Long user,Long session) {
        owned(user,session); return activeRun(session).map(this::run).orElse(null);
    }
    @Override public List<BankExercise> bank(Long user,Long session,int page,int size) {
        var s=owned(user,session);
        return exercises.findByModule(s.getModule(),page(page,size,"id")).stream()
                .map(e -> new BankExercise(e.getId(),e.getStatementLatex(),e.getTopic()==null ? null : e.getTopic().getCode(),
                        e.getType().name(),e.getDifficulty(),e.getExternalId())).toList();
    }
    @Override @Transactional public Run select(Long user,Long session,Long exerciseId,String statement,String reference) {
        var s=locked(user,session); available(session);
        var e=exerciseId==null ? null : exercises.findById(exerciseId).orElseThrow(() -> new IllegalArgumentException("Ejercicio no disponible"));
        if (e!=null && !Objects.equals(e.getModule().getId(),s.getModule().getId())) throw new IllegalArgumentException("Ejercicio de otro módulo");
        activeRun(session).ifPresent(old -> { old.setState(ExerciseRun.RunState.ABANDONADO); old.setEndedAt(Instant.now()); });
        return run(createRun(s, e, statement, reference));
    }

    /** @brief Valida módulo y propiedad dentro de la misma transacción que vincula el turno. */
    @Override @Transactional
    public Run attachRecognized(Long user, Long session, UUID request, Long exerciseId) {
        var s = locked(user, session);
        var turn = turns.findBySessionIdAndRequestId(session, request)
                .filter(t -> "EN_CURSO".equals(t.getStatus()) && t.getExerciseRun() == null)
                .orElseThrow(ConversationConflictException::new);
        var exercise = exercises.findById(exerciseId)
                .filter(e -> Objects.equals(e.getModule().getId(), s.getModule().getId()))
                .orElseThrow(() -> new IllegalArgumentException("Ejercicio no disponible en el módulo"));
        var selected = createRun(s, exercise, null, null);
        turn.setExerciseRun(selected);
        return run(selected);
    }

    private ExerciseRun createRun(TutorSession s, Exercise e, String statement, String reference) {
        var r=ExerciseRun.builder().session(s).exercise(e).topic(e==null ? null : e.getTopic())
                .statementLatex(e==null ? statement : e.getStatementLatex())
                .referenceSolution(e==null ? reference : (e.getType()==Exercise.ExerciseType.PROCEDIMENTAL ? e.getReferenceSolutionLatex() : null))
                .state(ExerciseRun.RunState.COMENZADO).currentLevel(ExerciseRun.HelpLevel.CONCEPTUAL).build();
        return runs.saveAndFlush(r);
    }
    private Optional<ExerciseRun> activeRun(Long session) { return runs.findFirstBySessionIdAndStateOrderByStartedAtDescIdDesc(session,ExerciseRun.RunState.COMENZADO); }
    private PageRequest page(int number,int size,String date) {
        if (number<0 || size<1 || size>100) throw new IllegalArgumentException("Paginación no válida (1–100)");
        return PageRequest.of(number,size,Sort.by(Sort.Direction.DESC,date,"id"));
    }
    private Entry entry(ConversationTurn t) {
        return new Entry(t.getId(),t.getRequestId(),t.getMessage(),t.getResponse(),t.getSession().getModule().getCode(),t.getTopic(),
                t.getDiagnosis()==null ? null : ErrorType.valueOf(t.getDiagnosis()),t.getHelpLevel(),t.getLatencyMs(),t.getTokensIn(),t.getTokensOut(),t.getStatus(),t.getCreatedAt(),t.getExerciseRun()==null ? null : t.getExerciseRun().getId(),t.getVerdict());
    }
    private Run run(ExerciseRun r) {
        var e=r.getExercise();
        var steps=e==null || r.getReferenceSolution()==null ? List.<String>of() : e.getCanonicalSteps().stream()
                .sorted(Comparator.comparingInt(CanonicalStep::getPosition)).map(CanonicalStep::getExpressionLatex).toList();
        var hints = e == null ? List.<String>of() : e.getCanonicalSteps().stream()
                .sorted(Comparator.comparingInt(CanonicalStep::getPosition))
                .flatMap(step -> step.getTypicalErrors().stream().sorted(Comparator.comparing(TypicalError::getCode)))
                .map(TypicalError::getSeedHint).filter(Objects::nonNull).distinct().toList();
        return new Run(r.getId(),e==null ? null : e.getId(),r.getStatementLatex(),r.getReferenceSolution(),r.getTopic()==null ? null : r.getTopic().getCode(),r.getState().name(),r.getProgress(),r.getVerifiedSteps(),steps,r.getAttempts(),
                e == null ? r.getReferenceSolution() : e.getReferenceSolutionLatex(), hints);
    }
}
