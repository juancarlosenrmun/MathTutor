package com.mathtutor.common.dto;

import java.util.List;

/**
 * Ejercicio del banco de ejercicios.
 */
public record Exercise(
    Long id,
    String externalId,        // ID estable entre versiones
    ExerciseType type,
    int difficulty,
    String statementLatex,
    String referenceSolutionLatex,
    Long moduleId,
    Long topicId,
    List<CanonicalStep> canonicalSteps,
    List<TypicalError> typicalErrors
) {
    public enum ExerciseType {
        TEORICO,
        PROCEDIMENTAL,
        INTERPRETACION
    }
}