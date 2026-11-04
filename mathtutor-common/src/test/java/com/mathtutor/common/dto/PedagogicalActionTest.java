package com.mathtutor.common.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de caja blanca para {@link PedagogicalAction}.
 * Cobertura: factory methods, campos, equals/hashCode, toString.
 */
class PedagogicalActionTest {

    @Test
    void shouldCreateConceptualAction() {
        PedagogicalAction action = PedagogicalAction.conceptual("Piensa en la definición de derivada");

        assertThat(action.helpLevel()).isEqualTo(HelpLevel.CONCEPTUAL);
        assertThat(action.hint()).isEqualTo("Piensa en la definición de derivada");
        assertThat(action.shouldRevealSolution()).isFalse();
        assertThat(action.analogyExample()).isNull();
    }

    @Test
    void shouldCreateProceduralAction() {
        PedagogicalAction action = PedagogicalAction.procedural("Aplica la regla de la cadena");

        assertThat(action.helpLevel()).isEqualTo(HelpLevel.PROCEDIMENTAL);
        assertThat(action.hint()).isEqualTo("Aplica la regla de la cadena");
        assertThat(action.shouldRevealSolution()).isFalse();
        assertThat(action.analogyExample()).isNull();
    }

    @Test
    void shouldCreateAnalogyAction() {
        PedagogicalAction action = PedagogicalAction.analogy("Similar a derivar sin(x)", "d/dx sin(x) = cos(x)");

        assertThat(action.helpLevel()).isEqualTo(HelpLevel.EJEMPLO_ANALOGO);
        assertThat(action.hint()).isEqualTo("Similar a derivar sin(x)");
        assertThat(action.shouldRevealSolution()).isFalse();
        assertThat(action.analogyExample()).isEqualTo("d/dx sin(x) = cos(x)");
    }

    @Test
    void shouldCreateRevelationAction() {
        PedagogicalAction action = PedagogicalAction.revelation("La solución es 2x");

        assertThat(action.helpLevel()).isEqualTo(HelpLevel.REVELACION);
        assertThat(action.hint()).isEqualTo("La solución es 2x");
        assertThat(action.shouldRevealSolution()).isTrue();
        assertThat(action.analogyExample()).isNull();
    }

    @ParameterizedTest
    @MethodSource("equalityCases")
    void shouldImplementEqualsAndHashCode(PedagogicalAction a, PedagogicalAction b, boolean expectedEqual) {
        assertThat(a.equals(b)).isEqualTo(expectedEqual);
        if (expectedEqual) {
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
        }
    }

    static Stream<Arguments> equalityCases() {
        return Stream.of(
            Arguments.of(
                PedagogicalAction.conceptual("hint"),
                PedagogicalAction.conceptual("hint"),
                true
            ),
            Arguments.of(
                PedagogicalAction.conceptual("hint1"),
                PedagogicalAction.conceptual("hint2"),
                false
            ),
            Arguments.of(
                PedagogicalAction.revelation("sol"),
                PedagogicalAction.conceptual("sol"),
                false
            ),
            Arguments.of(
                PedagogicalAction.analogy("hint", "example"),
                PedagogicalAction.analogy("hint", "example"),
                true
            ),
            Arguments.of(
                PedagogicalAction.analogy("hint", "example1"),
                PedagogicalAction.analogy("hint", "example2"),
                false
            )
        );
    }

    @Test
    void shouldHaveMeaningfulToString() {
        PedagogicalAction action = PedagogicalAction.conceptual("test hint");

        String toString = action.toString();

        assertThat(toString).contains("CONCEPTUAL");
        assertThat(toString).contains("test hint");
        assertThat(toString).contains("shouldRevealSolution=false");
    }
}