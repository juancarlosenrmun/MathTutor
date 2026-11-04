package com.mathtutor.common.dto;

import java.time.Instant;

/**
 * Ejecución de un ejercicio dentro de una sesión.
 */
public record ExerciseRun(
    Long id,
    String statementLatex,
    RunState state,
    double progress,
    HelpLevel currentLevel,
    int attempts,
    Instant startedAt,
    Instant endedAt,
    Long exerciseId,      // Opcional: si es del banco
    Long topicId          // Tema del ejercicio
) {
    public enum RunState {
        COMENZADO,
        COMPLETADO,
        ABANDONADO
    }

    public enum AbandonPhase {
        INICIO,
        CERCA_SOLUCION
    }
}