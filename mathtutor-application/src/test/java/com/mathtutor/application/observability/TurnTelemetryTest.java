package com.mathtutor.application.observability;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.mathtutor.persistence.adapter.TurnObserved;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class TurnTelemetryTest {
    @Test void logsOnlyOperationalFieldsAndRecordsReportedTokensByDirection() {
        var registry = new SimpleMeterRegistry();
        var telemetry = new TurnTelemetry(registry);
        var logger = (Logger) LoggerFactory.getLogger(TurnTelemetry.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            telemetry.record(new TurnObserved("COMPLETADO", 125, 11, 3));
            telemetry.record(new TurnObserved("RECHAZADO", 5, null, null));
            telemetry.record(new TurnObserved("ERROR", 15, 0, -1));
            assertThat(registry.get("mathtutor.turn.latency").tag("status", "COMPLETADO")
                    .timer().totalTime(TimeUnit.MILLISECONDS)).isEqualTo(125);
            assertThat(registry.get("mathtutor.turn.tokens").tag("direction", "input").counter().count()).isEqualTo(11);
            assertThat(registry.get("mathtutor.turn.tokens").tag("direction", "output").counter().count()).isEqualTo(3);
            assertThat(appender.list.getFirst().getKeyValuePairs()).extracting(entry -> entry.key)
                    .containsExactly("event", "status", "latency_ms", "input_tokens", "output_tokens");
            assertThat(appender.list.get(1).getKeyValuePairs()).extracting(entry -> entry.key)
                    .containsExactly("event", "status", "latency_ms");
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
