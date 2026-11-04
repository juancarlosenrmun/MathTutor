package com.mathtutor.common.dto;

import java.time.Instant;
import java.util.List;

/**
 * Estado de una sesión de tutoría.
 */
public record SessionState(
    String sessionId,
    String userId,
    Long moduleId,
    Instant startedAt,
    Instant lastActivityAt,
    SessionStatus status,
    List<Turn> turns,
    ExerciseRun currentExerciseRun
) {
    public enum SessionStatus {
        ACTIVA,
        CERRADA
    }
}