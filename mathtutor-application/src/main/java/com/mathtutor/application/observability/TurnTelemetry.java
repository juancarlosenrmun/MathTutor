package com.mathtutor.application.observability;

import com.mathtutor.persistence.adapter.TurnObserved;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.TimeUnit;

/** @brief Publica un registro y medidas de baja cardinalidad después de confirmar el turno. */
@Component
public class TurnTelemetry {
    private final MeterRegistry metrics;

    public TurnTelemetry(MeterRegistry metrics) {
        this.metrics = metrics;
    }

    /** @param turn Medidas registradas en el cierre del turno. El uso puede estar ausente. */
    @TransactionalEventListener
    public void record(TurnObserved turn) {
        var event = LoggerFactory.getLogger(TurnTelemetry.class).atInfo()
                .addKeyValue("event", "tutor_turn_finished").addKeyValue("status", turn.status())
                .addKeyValue("latency_ms", turn.latencyMs());
        metrics.timer("mathtutor.turn.latency", "status", turn.status())
                .record(turn.latencyMs(), TimeUnit.MILLISECONDS);
        recordTokens(event, "input", turn.inputTokens());
        recordTokens(event, "output", turn.outputTokens());
        event.log("Turno del tutor finalizado");
    }

    private void recordTokens(LoggingEventBuilder event, String direction, Integer tokens) {
        if (tokens == null || tokens < 0) return;
        event.addKeyValue(direction + "_tokens", tokens);
        metrics.counter("mathtutor.turn.tokens", "direction", direction).increment(tokens);
    }
}
