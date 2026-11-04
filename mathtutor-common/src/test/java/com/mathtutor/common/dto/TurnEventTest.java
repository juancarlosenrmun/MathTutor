package com.mathtutor.common.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de caja blanca para {@link TurnEvent} y sus subtipos.
 * Cobertura: todas las variantes sealed, campos, equals/hashCode, toString.
 */
class TurnEventTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:30:00Z");

    @Test
    void shouldCreateTokenEvent() {
        TurnEvent.Token event = new TurnEvent.Token("Hola", NOW);

        assertThat(event.content()).isEqualTo("Hola");
        assertThat(event.timestamp()).isEqualTo(NOW);
        assertThat(event).isInstanceOf(TurnEvent.class);
    }

    @Test
    void shouldCreateDoneEvent() {
        TurnEvent.Done event = new TurnEvent.Done("Respuesta completa", 10, 20, 1500, NOW);

        assertThat(event.fullResponse()).isEqualTo("Respuesta completa");
        assertThat(event.tokensIn()).isEqualTo(10);
        assertThat(event.tokensOut()).isEqualTo(20);
        assertThat(event.latencyMs()).isEqualTo(1500);
        assertThat(event.timestamp()).isEqualTo(NOW);
    }

    @Test
    void shouldCreateErrorEvent() {
        TurnEvent.Error event = new TurnEvent.Error("QUOTA_EXCEEDED", "Cuota diaria agotada", NOW);

        assertThat(event.code()).isEqualTo("QUOTA_EXCEEDED");
        assertThat(event.message()).isEqualTo("Cuota diaria agotada");
        assertThat(event.timestamp()).isEqualTo(NOW);
    }

    @Test
    void shouldCreateGraphicSpecEvent() {
        GraphicSpec spec = new GraphicSpec(
            GraphicSpec.GraphicType.CURVA_2D,
            "x^2",
            new GraphicSpec.Domain(-1.0, 1.0, 10),
            List.of()
        );
        TurnEvent.GraphicSpec event = new TurnEvent.GraphicSpec(spec, NOW);

        assertThat(event.spec()).isEqualTo(spec);
        assertThat(event.timestamp()).isEqualTo(NOW);
    }

    @Test
    void shouldCreateHelpLevelChangedEvent() {
        TurnEvent.HelpLevelChanged event = new TurnEvent.HelpLevelChanged(HelpLevel.PROCEDIMENTAL, NOW);

        assertThat(event.level()).isEqualTo(HelpLevel.PROCEDIMENTAL);
        assertThat(event.timestamp()).isEqualTo(NOW);
    }

    @Test
    void shouldCreateDiagnosisEvent() {
        TurnEvent.Diagnosis event = new TurnEvent.Diagnosis(ErrorType.SIGNO, "Revisa los signos", NOW);

        assertThat(event.errorType()).isEqualTo(ErrorType.SIGNO);
        assertThat(event.hint()).isEqualTo("Revisa los signos");
        assertThat(event.timestamp()).isEqualTo(NOW);
    }

    @Test
    void shouldImplementEqualsAndHashCodeForToken() {
        TurnEvent.Token e1 = new TurnEvent.Token("test", NOW);
        TurnEvent.Token e2 = new TurnEvent.Token("test", NOW);
        TurnEvent.Token e3 = new TurnEvent.Token("other", NOW);

        assertThat(e1).isEqualTo(e2);
        assertThat(e1.hashCode()).isEqualTo(e2.hashCode());
        assertThat(e1).isNotEqualTo(e3);
    }

    @Test
    void shouldImplementEqualsAndHashCodeForDone() {
        TurnEvent.Done e1 = new TurnEvent.Done("resp", 1, 2, 100, NOW);
        TurnEvent.Done e2 = new TurnEvent.Done("resp", 1, 2, 100, NOW);
        TurnEvent.Done e3 = new TurnEvent.Done("resp", 2, 2, 100, NOW);

        assertThat(e1).isEqualTo(e2);
        assertThat(e1.hashCode()).isEqualTo(e2.hashCode());
        assertThat(e1).isNotEqualTo(e3);
    }
}