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
    void firstAttemptReturnsHint() {
        // Given
        ExerciseRun exerciseRun = new ExerciseRun(
            1L, "x^2", ExerciseRun.RunState.COMENZADO, 0.0, HelpLevel.NONE, 0,
            Instant.now(), null, 1L, 1L
        );
        ErrorType diagnosis = ErrorType.SYNTAX_ERROR;
        int attempts = 0;

        // When
        PedagogicalAction action = policy.decide(exerciseRun, diagnosis, attempts);

        // Then
        assertThat(action).isNotNull();
        assertThat(action.getHelpLevel()).isEqualTo(HelpLevel.HINT);
        assertThat(action.getMessage()).contains("paso a paso");
    }

    @Test
    void secondAttemptReturnsGuidedStep() {
        // Given
        ExerciseRun exerciseRun = new ExerciseRun(
            1L, "x^2", ExerciseRun.RunState.COMENZADO, 0.0, HelpLevel.NONE, 0,
            Instant.now(), null, 1L, 1L
        );
        ErrorType diagnosis = ErrorType.SYNTAX_ERROR;
        int attempts = 1;

        // When
        PedagogicalAction action = policy.decide(exerciseRun, diagnosis, attempts);

        // Then
        assertThat(action).isNotNull();
        assertThat(action.getHelpLevel()).isEqualTo(HelpLevel.GUIDED_STEP);
        assertThat(action.getMessage()).contains("primer paso");
    }

    @Test
    void thirdAttemptReturnsSolution() {
        // Given
        ExerciseRun exerciseRun = new ExerciseRun(
            1L, "x^2", ExerciseRun.RunState.COMENZADO, 0.0, HelpLevel.NONE, 0,
            Instant.now(), null, 1L, 1L
        );
        ErrorType diagnosis = ErrorType.SYNTAX_ERROR;
        int attempts = 2;

        // When
        PedagogicalAction action = policy.decide(exerciseRun, diagnosis, attempts);

        // Then
        assertThat(action).isNotNull();
        assertThat(action.getHelpLevel()).isEqualTo(HelpLevel.SOLUTION);
        assertThat(action.getMessage()).contains("solución completa");
    }

    @Test
    void implementsPedagogicalPolicyInterface() {
        // Then
        assertThat(policy).isInstanceOf(PedagogicalPolicy.class);
    }
}