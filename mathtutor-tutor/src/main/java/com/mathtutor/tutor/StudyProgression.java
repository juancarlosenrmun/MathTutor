package com.mathtutor.tutor;

import com.mathtutor.common.dto.ConversationConflictException;
import com.mathtutor.common.port.ConversationStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** @brief Inicia la siguiente práctica guiada utilizando el historial del alumno en el módulo. */
@Service
public class StudyProgression {
    private final ConversationStore store;
    private final ProgressionPlanner planner;

    public StudyProgression(ConversationStore store, ProgressionPlanner planner) {
        this.store = store;
        this.planner = planner;
    }

    /**
     * @brief Activa la siguiente etapa o recupera su ejercicio si ya está en curso.
     * @param user Alumno autenticado.
     * @param session Conversación activa del módulo.
     * @return Enunciado y tipo, sin referencias internas.
     */
    @Transactional
    public Optional<Practice> advance(Long user, Long session) {
        var progress = store.progression(user, session);
        var active = progress.active();
        if (active != null) {
            return progress.candidates().stream()
                    .filter(exercise -> exercise.id().equals(active.exerciseId()))
                    .findFirst().map(exercise -> new Practice(exercise.statement(), exercise.type()))
                    .map(Optional::of).orElseThrow(ConversationConflictException::new);
        }
        return planner.next(progress.candidates(), progress.practicedTypes()).map(exercise -> {
            store.select(user, session, exercise.id(), null, null);
            return new Practice(exercise.statement(), exercise.type());
        });
    }

    public record Practice(String statement, String type) {}
}
