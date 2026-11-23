package com.mathtutor.tutor;

import com.mathtutor.common.port.CasEngine;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OutputGuardTest {
    @Test
    void notesStillProtectWorkedAnswersWhenBankHasAReference() {
        var guard = new OutputGuard(null);
        String solution = "Se aplica la continuidad primero a la función interior";
        assertThat(guard.safe(solution, "referencia del banco",
                List.of("\\begin{solution}\n" + solution + "\n\\end{solution}"))).isFalse();
    }

    @Test
    void symbolicReferenceWithAFunctionLabelProtectsItsRightHandSide() {
        var guard = new OutputGuard(null);
        assertThat(guard.safe("Pista: escribe $2x$.", "f'(x)=2x", List.of())).isFalse();
        assertThat(guard.safe("Pista: escribe $12x$.", "f'(x)=2x", List.of())).isTrue();
        assertThat(guard.safe("¿Qué parte de 142 usarías?", "42", List.of())).isTrue();
    }
    private final CasEngine cas = mock(CasEngine.class);
    private final OutputGuard guard = new OutputGuard(cas);

    @Test
    void doesNotSendNarrativeWordsToSymbolicSimplification() {
        assertThat(guard.safe("El determinante es una propiedad de la matriz. ¿Qué entradas usarías?",
                "35", List.of())).isTrue();
        assertThat(guard.safe("Compara $x+1$ y $x-1$.", "Una matriz invertible tiene determinante no nulo.",
                List.of())).isTrue();
        verifyNoInteractions(cas);
    }

    @Test
    void blocksEquivalentMathAndKeepsAnIntermediateHint() {
        when(cas.compare("x+x", "2*x")).thenReturn(CasEngine.Comparison.EQUIVALENT);
        assertThat(guard.safe("Por tanto, \\(f'(x)=x+x\\).", "2*x", List.of())).isFalse();
        assertThat(guard.safe("La pendiente es 2x.", "2*x", List.of())).isFalse();
        assertThat(guard.safe("¿Qué regla aplicarías a $x^2$?", "2*x", List.of())).isTrue();
    }

    @Test
    void blocksWorkedNoteAndExplicitFinalAnswerWithoutReference() {
        var notes = List.of("Introducción\n\\begin{solution}\nSe separan las variables y se integra cada miembro.\n\\end{solution}");
        assertThat(guard.safe("Se separan las variables y se integra cada miembro.", null, notes)).isFalse();
        assertThat(guard.safe("**Solución final**: 42.", null, notes)).isFalse();
        assertThat(guard.safe("¿Qué variable puedes aislar primero?", null, notes)).isTrue();
    }

    @Test
    void numericReferenceMatchesAWholeNumberAndNotDigitsOfAnotherNumber() {
        assertThat(guard.safe("Primero identifica el término 142.", "42", List.of())).isTrue();
        assertThat(guard.safe("El valor se obtiene y es 42.", "42", List.of())).isFalse();
    }

    @Test
    void checksUnwrappedEquationsAndBoundsSymbolicWork() {
        when(cas.compare("x+x", "2*x")).thenReturn(CasEngine.Comparison.EQUIVALENT);
        assertThat(guard.safe("Se obtiene f'(x) = x+x.", "2*x", List.of())).isFalse();
        assertThat(guard.safe("La pendiente es x+x.", "2*x", List.of())).isFalse();
        assertThat(new OutputGuard(null).safe("Considera $x+x$.", "2*x", List.of())).isTrue();
        assertThat(guard.safe("$x$ ".repeat(12), "2*x", List.of())).isTrue();
        assertThat(guard.safe("$x$ ".repeat(13), "2*x", List.of())).isFalse();
    }

    @Test
    void recognizesLabelledWorkedNotesAndIgnoresIncidentalShortLines() {
        var notes = List.of("Párrafo teórico.\nSolución final: Se despeja la incógnita del segundo miembro.",
                "\\begin{example}\n$x=1$\nEjemplo suficientemente largo que no se repite.\n\\end{example}",
                "Solución final: 4");
        assertThat(guard.safe("Se despeja la incógnita del segundo miembro.", "", notes)).isFalse();
        assertThat(guard.safe("¿Cómo se aplica el método a $x=1$?", null, notes)).isTrue();
    }
}
