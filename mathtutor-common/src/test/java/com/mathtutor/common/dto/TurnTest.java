package com.mathtutor.common.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de caja blanca para {@link Turn}.
 * Cobertura: campos, equals/hashCode, toString, GraphicSpec opcional.
 */
class TurnTest {

    private static final Instant AT = Instant.parse("2026-01-15T10:30:00Z");

    @Test
    void shouldCreateTurnWithAllFields() {
        GraphicSpec spec = new GraphicSpec(
            GraphicSpec.GraphicType.CURVA_2D,
            "x^2",
            new GraphicSpec.Domain(-1.0, 1.0, 10),
            List.of()
        );

        Turn turn = new Turn(
            1L,
            "¿Cómo derivar x^2?",
            "Usa la regla de la potencia",
            ErrorType.NINGUNO,
            HelpLevel.PROCEDIMENTAL,
            1200,
            15,
            25,
            AT,
            spec
        );

        assertThat(turn.id()).isEqualTo(1L);
        assertThat(turn.studentMessage()).isEqualTo("¿Cómo derivar x^2?");
        assertThat(turn.tutorResponse()).isEqualTo("Usa la regla de la potencia");
        assertThat(turn.diagnosis()).isEqualTo(ErrorType.NINGUNO);
        assertThat(turn.helpLevel()).isEqualTo(HelpLevel.PROCEDIMENTAL);
        assertThat(turn.latencyMs()).isEqualTo(1200);
        assertThat(turn.tokensIn()).isEqualTo(15);
        assertThat(turn.tokensOut()).isEqualTo(25);
        assertThat(turn.at()).isEqualTo(AT);
        assertThat(turn.graphicSpec()).isEqualTo(spec);
    }

    @Test
    void shouldCreateTurnWithoutGraphicSpec() {
        Turn turn = new Turn(
            1L,
            "msg",
            "resp",
            ErrorType.NINGUNO,
            HelpLevel.CONCEPTUAL,
            100,
            5,
            10,
            AT,
            null
        );

        assertThat(turn.graphicSpec()).isNull();
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        Turn t1 = new Turn(1L, "m", "r", ErrorType.NINGUNO, HelpLevel.CONCEPTUAL, 100, 5, 10, AT, null);
        Turn t2 = new Turn(1L, "m", "r", ErrorType.NINGUNO, HelpLevel.CONCEPTUAL, 100, 5, 10, AT, null);
        Turn t3 = new Turn(2L, "m", "r", ErrorType.NINGUNO, HelpLevel.CONCEPTUAL, 100, 5, 10, AT, null);

        assertThat(t1).isEqualTo(t2);
        assertThat(t1.hashCode()).isEqualTo(t2.hashCode());
        assertThat(t1).isNotEqualTo(t3);
    }
}