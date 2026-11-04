package com.mathtutor.common.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de caja blanca para {@link Exercise}.
 * Cobertura: campos, enum ExerciseType, equals/hashCode, toString.
 */
class ExerciseTest {

    @Test
    void shouldCreateExerciseWithAllFields() {
        CanonicalStep step = new CanonicalStep(1, "2x", "[\"2*x\", \"2x\"]");
        TypicalError error = new TypicalError("SIGN_ERROR", "Revisa el signo");

        Exercise exercise = new Exercise(
            1L,
            "EX-001",
            Exercise.ExerciseType.PROCEDIMENTAL,
            3,
            "x^2",
            "2x",
            1L,
            2L,
            List.of(step),
            List.of(error)
        );

        assertThat(exercise.id()).isEqualTo(1L);
        assertThat(exercise.externalId()).isEqualTo("EX-001");
        assertThat(exercise.type()).isEqualTo(Exercise.ExerciseType.PROCEDIMENTAL);
        assertThat(exercise.difficulty()).isEqualTo(3);
        assertThat(exercise.statementLatex()).isEqualTo("x^2");
        assertThat(exercise.referenceSolutionLatex()).isEqualTo("2x");
        assertThat(exercise.moduleId()).isEqualTo(1L);
        assertThat(exercise.topicId()).isEqualTo(2L);
        assertThat(exercise.canonicalSteps()).containsExactly(step);
        assertThat(exercise.typicalErrors()).containsExactly(error);
    }

    @Test
    void shouldHaveThreeExerciseTypes() {
        assertThat(Exercise.ExerciseType.values()).hasSize(3);
        assertThat(Exercise.ExerciseType.TEORICO).isNotNull();
        assertThat(Exercise.ExerciseType.PROCEDIMENTAL).isNotNull();
        assertThat(Exercise.ExerciseType.INTERPRETACION).isNotNull();
    }

    @Test
    void shouldAllowEmptyStepsAndErrors() {
        Exercise exercise = new Exercise(
            1L,
            "EX-001",
            Exercise.ExerciseType.TEORICO,
            1,
            "¿Qué es una derivada?",
            "La derivada mide la tasa de cambio",
            1L,
            2L,
            List.of(),
            List.of()
        );

        assertThat(exercise.canonicalSteps()).isEmpty();
        assertThat(exercise.typicalErrors()).isEmpty();
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        CanonicalStep step = new CanonicalStep(1, "2x", "[\"2*x\"]");
        Exercise e1 = new Exercise(1L, "EX-001", Exercise.ExerciseType.PROCEDIMENTAL, 3,
            "x^2", "2x", 1L, 2L, List.of(step), List.of());
        Exercise e2 = new Exercise(1L, "EX-001", Exercise.ExerciseType.PROCEDIMENTAL, 3,
            "x^2", "2x", 1L, 2L, List.of(step), List.of());
        Exercise e3 = new Exercise(2L, "EX-001", Exercise.ExerciseType.PROCEDIMENTAL, 3,
            "x^2", "2x", 1L, 2L, List.of(step), List.of());

        assertThat(e1).isEqualTo(e2);
        assertThat(e1.hashCode()).isEqualTo(e2.hashCode());
        assertThat(e1).isNotEqualTo(e3);
    }
}