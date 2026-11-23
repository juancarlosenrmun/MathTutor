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
 * Comprueba los límites de la progresión y las ramas de diagnóstico.
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
        assertThat(action.shouldRevealSolution()).isFalse();
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
        assertThat(action.shouldRevealSolution()).isFalse();
    }

    @Test
    void thirdAttemptReturnsAnalogy() {
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
        assertThat(action.helpLevel()).isEqualTo(HelpLevel.EJEMPLO_ANALOGO);
        assertThat(action.shouldRevealSolution()).isFalse();
    }

    @Test
    void revelationRequiresAnActiveExerciseAndThreePriorAttempts() {
        var active = new ExerciseRun(1L, "x^2", ExerciseRun.RunState.COMENZADO, 0,
                HelpLevel.EJEMPLO_ANALOGO, 3, Instant.EPOCH, null, null, null);
        assertThat(policy.decide(active, ErrorType.OTRO, 3).shouldRevealSolution()).isTrue();
        assertThat(policy.decide(null, ErrorType.OTRO, 3).shouldRevealSolution()).isFalse();
        var completed = new ExerciseRun(1L, "x^2", ExerciseRun.RunState.COMPLETADO, 1,
                HelpLevel.REVELACION, 4, Instant.EPOCH, Instant.EPOCH, null, null);
        assertThat(policy.decide(completed, ErrorType.NINGUNO, 4).shouldRevealSolution()).isFalse();
    }

    @Test
    void signProductAndChainDiagnosesGuideTheNextStep() {
        assertThat(policy.decide(null, ErrorType.SIGNO, 1).hint()).contains("signo");
        assertThat(policy.decide(null, ErrorType.REGLA_CADENA, 1).hint()).contains("interior");
        assertThat(policy.decide(null, ErrorType.REGLA_PRODUCTO, 1).hint()).contains("factores");
        assertThat(policy.decide(null, ErrorType.OTRO, -1).helpLevel()).isEqualTo(HelpLevel.CONCEPTUAL);
    }

    @Test
    void implementsPedagogicalPolicyInterface() {
        // Then
        assertThat(policy).isInstanceOf(PedagogicalPolicy.class);
    }
}
