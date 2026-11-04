package com.mathtutor.common.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de caja blanca para {@link ExerciseRun}.
 * Cobertura: campos, enums RunState y AbandonPhase, equals/hashCode, toString.
 */
class ExerciseRunTest {

    private static final Instant STARTED = Instant.parse("2026-01-15T10:00:00Z");
    private static final Instant ENDED = Instant.parse("2026-01-15T10:30:00Z");

    @Test
    void shouldCreateExerciseRunWithAllFields() {
        ExerciseRun run = new ExerciseRun(
            1L,
            "x^2",
            ExerciseRun.RunState.COMENZADO,
            0.5,
            HelpLevel.PROCEDIMENTAL,
            3,
            STARTED,
            ENDED,
            10L,
            5L
        );

        assertThat(run.id()).isEqualTo(1L);
        assertThat(run.statementLatex()).isEqualTo("x^2");
        assertThat(run.state()).isEqualTo(ExerciseRun.RunState.COMENZADO);
        assertThat(run.progress()).isEqualTo(0.5);
        assertThat(run.currentLevel()).isEqualTo(HelpLevel.PROCEDIMENTAL);
        assertThat(run.attempts()).isEqualTo(3);
        assertThat(run.startedAt()).isEqualTo(STARTED);
        assertThat(run.endedAt()).isEqualTo(ENDED);
        assertThat(run.exerciseId()).isEqualTo(10L);
        assertThat(run.topicId()).isEqualTo(5L);
    }

    @Test
    void shouldHaveThreeRunStates() {
        assertThat(ExerciseRun.RunState.values()).hasSize(3);
        assertThat(ExerciseRun.RunState.COMENZADO).isNotNull();
        assertThat(ExerciseRun.RunState.COMPLETADO).isNotNull();
        assertThat(ExerciseRun.RunState.ABANDONADO).isNotNull();
    }

    @Test
    void shouldHaveTwoAbandonPhases() {
        assertThat(ExerciseRun.AbandonPhase.values()).hasSize(2);
        assertThat(ExerciseRun.AbandonPhase.INICIO).isNotNull();
        assertThat(ExerciseRun.AbandonPhase.CERCA_SOLUCION).isNotNull();
    }

    @Test
    void shouldAllowNullExerciseAndTopicId() {
        ExerciseRun run = new ExerciseRun(
            1L,
            "x^2",
            ExerciseRun.RunState.COMENZADO,
            0.0,
            HelpLevel.CONCEPTUAL,
            0,
            STARTED,
            null,
            null,
            null
        );

        assertThat(run.exerciseId()).isNull();
        assertThat(run.topicId()).isNull();
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        ExerciseRun r1 = new ExerciseRun(1L, "x", ExerciseRun.RunState.COMENZADO, 0.0,
            HelpLevel.CONCEPTUAL, 0, STARTED, null, 1L, 1L);
        ExerciseRun r2 = new ExerciseRun(1L, "x", ExerciseRun.RunState.COMENZADO, 0.0,
            HelpLevel.CONCEPTUAL, 0, STARTED, null, 1L, 1L);
        ExerciseRun r3 = new ExerciseRun(2L, "x", ExerciseRun.RunState.COMENZADO, 0.0,
            HelpLevel.CONCEPTUAL, 0, STARTED, null, 1L, 1L);

        assertThat(r1).isEqualTo(r2);
        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
        assertThat(r1).isNotEqualTo(r3);
    }
}