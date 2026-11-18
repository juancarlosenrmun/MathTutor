package com.mathtutor.application.controller;

import com.mathtutor.auth.filter.JwtAuthenticationFilter.AuthenticatedUser;
import com.mathtutor.common.port.ConversationStore;
import com.mathtutor.common.port.GraphicSpecService;
import com.mathtutor.common.dto.*;
import com.mathtutor.tutor.StudyExercises;
import com.mathtutor.tutor.TutorSessions;
import com.mathtutor.tutor.SessionUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import java.util.List;

/** UC-11/17/35: consultas propias y selección explícita; nunca expone soluciones. */
@RestController @RequestMapping("/api/tutor") @PreAuthorize("hasAnyRole('ALUMNO', 'PROFESOR')")
public class StudyProgressController {
    private final ConversationStore store;
    private final StudyExercises exercises;
    private final TutorSessions sessions;
    private final GraphicSpecService graphics;
    public StudyProgressController(ConversationStore store,StudyExercises exercises,TutorSessions sessions,GraphicSpecService graphics) {
        this.store=store; this.exercises=exercises; this.sessions=sessions; this.graphics=graphics;
    }
    @GetMapping("/progress") public List<ConversationStore.Session> progress(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return store.sessions(user.id(),page,size); }
    @GetMapping("/sessions/{id}/history") public List<ConversationStore.Entry> history(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return store.history(user.id(),id,page,size); }
    @GetMapping("/sessions/{id}/exercises") public List<RunView> runs(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return store.runs(user.id(),id,page,size).stream().map(RunView::from).toList();
    }
    @GetMapping("/sessions/{id}/bank") public List<ConversationStore.BankExercise> bank(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return store.bank(user.id(),id,page,size);
    }
    @PostMapping("/sessions/{id}/exercises") @ResponseStatus(HttpStatus.CREATED)
    public RunView select(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable Long id,@RequestBody Selection selection) {
        return RunView.from(exercises.select(user.id(),id,selection.exerciseId(),selection.operation(),selection.expression()));
    }
    @PostMapping(value="/sessions/{id}/graphics",produces=MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<GraphicEvent> graphics(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable Long id,@RequestBody GraphicSpec spec) {
        sessions.active(user.id(),id);
        var validated=graphics.build(spec.type(),spec.function(),spec.domain(),spec.points());
        return Flux.just(new GraphicEvent("GRAPHIC_SPEC",validated),new GraphicEvent("DONE",null));
    }
    @ExceptionHandler({ConversationUnavailableException.class,SessionUnavailableException.class}) @ResponseStatus(HttpStatus.NOT_FOUND)
    public String unavailable() { return "Sesión no disponible"; }
    @ExceptionHandler(ConversationConflictException.class) @ResponseStatus(HttpStatus.CONFLICT)
    public String conflict() { return "Hay un turno en curso. Consulta el historial antes de reintentar."; }
    public record Selection(Long exerciseId,String operation,String expression) {}
    public record GraphicEvent(String type,GraphicSpec spec) {}
    public record RunView(Long id,Long exerciseId,String statement,String topic,String state,double progress,int verifiedSteps) {
        static RunView from(ConversationStore.Run r) { return new RunView(r.id(),r.exerciseId(),r.statement(),r.topic(),r.state(),r.progress(),r.verifiedSteps()); }
    }
}
