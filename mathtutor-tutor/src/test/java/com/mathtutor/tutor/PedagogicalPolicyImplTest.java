package com.mathtutor.tutor;

import com.mathtutor.common.dto.ErrorType;
import com.mathtutor.common.dto.ExerciseRun;
import com.mathtutor.common.dto.HelpLevel;
import com.mathtutor.common.dto.PedagogicalAction;
import com.mathtutor.common.port.PedagogicalPolicy;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de caja blanca para PedagogicalPolicyImpl.
 * Verifica el comportamiento del esqueleto.
 */
class PedagogicalPolicyImplTest {

    private final PedagogicalPolicy policy = new PedagogicalPolicyImpl();

    @Test
    void firstAttemptReturnsConceptualHint() {
        // Given
        ExerciseRun exerciseRun = new ExerciseRun(
            1L, "x^2", ExerciseRun.RunState.COMENZADO, 0.0, HelpLevel.CONCEPTUAL, 0,
            Instant.now(), null, 1L, 1L
        );
        ErrorType diagnosis = ErrorType.NINGUNO;
        int attempts = 0;

        // When
        PedagogicalAction action = policy.decide(exerciseRun, diagnosis, attempts);

        // Then
        assertThat(action).isNotNull();
        assertThat(action.helpLevel()).isEqualTo(HelpLevel.CONCEPTUAL);
        assertThat(action.hint()).contains("paso a paso");
    }

    @Test
    void secondAttemptReturnsProceduralGuidance() {
        // Given
        ExerciseRun exerciseRun = new ExerciseRun(
            1L, "x^2", ExerciseRun.RunState.COMENZADO, 0.0, HelpLevel.CONCEPTUAL, 0,
            Instant.now(), null, 1L, 1L
        );
        ErrorType diagnosis = ErrorType.NINGUNO;
        int attempts = 1;

        // When
        PedagogicalAction action = policy.decide(exerciseRun, diagnosis, attempts);

        // Then
        assertThat(action).isNotNull();
        assertThat(action.helpLevel()).isEqualTo(HelpLevel.PROCEDIMENTAL);
        assertThat(action.hint()).contains("primer paso");
    }

    @Test
    void thirdAttemptReturnsRevelation() {
        // Given
        ExerciseRun exerciseRun = new ExerciseRun(
            1L, "x^2", ExerciseRun.RunState.COMENZADO, 0.0, HelpLevel.CONCEPTUAL, 0,
            Instant.now(), null, 1L, 1L
        );
        ErrorType diagnosis = ErrorType.NINGUNO;
        int attempts = 2;

        // When
        PedagogicalAction action = policy.decide(exerciseRun, diagnosis, attempts);

        // Then
        assertThat(action).isNotNull();
        assertThat(action.helpLevel()).isEqualTo(HelpLevel.REVELACION);
        assertThat(action.hint()).contains("solución completa");
    }

    @Test
    void implementsPedagogicalPolicyInterface() {
        // Then
        assertThat(policy).isInstanceOf(PedagogicalPolicy.class);
    }
}