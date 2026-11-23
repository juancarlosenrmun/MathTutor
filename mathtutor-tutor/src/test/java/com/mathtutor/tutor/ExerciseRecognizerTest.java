package com.mathtutor.tutor;

import com.mathtutor.common.port.CasEngine;
import com.mathtutor.common.port.ConversationStore.BankExercise;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ExerciseRecognizerTest {
    private final CasEngine cas = mock(CasEngine.class);
    private final ExerciseRecognizer recognizer = new ExerciseRecognizer(cas);
    private final BankExercise derivative = new BankExercise(7L, "Calcula la derivada de f(x)=x^2.",
            "calculo", "PROCEDIMENTAL", 2, "CAL-PROC-001");
    private final BankExercise theory = new BankExercise(8L, "Define continuidad de f en a.",
            "continuidad", "TEORICO", 2, "FUN-TEO-001");

    @Test
    void recognizesStableIdsAndNormalizesText() {
        assertThat(recognizer.recognize("Ejercicio cal-proc-001", List.of(derivative, theory))).contains(7L);
        assertThat(recognizer.recognize("  DEFINE   continuidad de f en a ", List.of(theory))).contains(8L);
        assertThat(recognizer.recognize("¿Qué es la continuidad?", List.of(theory))).isEmpty();
        verifyNoInteractions(cas);
    }

    @Test
    void comparesOnlyExpressionsWithTheSameOperation() {
        when(cas.compare("x*x", "x^2")).thenReturn(CasEngine.Comparison.EQUIVALENT);
        assertThat(recognizer.recognize("Calcula la derivada de f(x)=x*x", List.of(derivative))).contains(7L);
        assertThat(recognizer.recognize("Calcula la integral de f(x)=x*x", List.of(derivative))).isEmpty();
        assertThat(recognizer.recognize("Calcula la derivada de f(x)=x^3", List.of(derivative))).isEmpty();
        verify(cas, never()).compare("x*x", "x*x");
    }

    @Test
    void leavesAmbiguousStatementsInTheFreeFlow() {
        var duplicate = new BankExercise(9L, theory.statement(), theory.topic(), theory.type(), 2, "FUN-TEO-002");
        assertThat(recognizer.recognize(theory.statement(), List.of(theory, duplicate))).isEmpty();
        assertThat(recognizer.recognize("FUN-TEO-002", List.of(theory, duplicate))).contains(9L);
    }
}
