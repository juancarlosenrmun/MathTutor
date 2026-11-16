package com.mathtutor.persistence.adapter;

import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimitAdapterTest {
    private final MutableClock clock = new MutableClock();
    private final RateLimitAdapter limiter = new RateLimitAdapter(clock);

    @Test
    void intentosDenegadosNoProlonganLaVentana() {
        assertThat(limiter.tryConsume("ip", 1, Duration.ofMinutes(15))).isTrue();
        clock.now = clock.now.plus(Duration.ofMinutes(14));
        assertThat(limiter.tryConsume("ip", 1, Duration.ofMinutes(15))).isFalse();
        clock.now = clock.now.plus(Duration.ofMinutes(1));
        assertThat(limiter.tryConsume("ip", 1, Duration.ofMinutes(15))).isTrue();
    }

    @Test
    void clavesYReinicioSonIndependientes() {
        assertThat(limiter.tryConsume("a", 1, Duration.ofMinutes(15))).isTrue();
        assertThat(limiter.tryConsume("a", 1, Duration.ofMinutes(15))).isFalse();
        assertThat(limiter.tryConsume("b", 1, Duration.ofMinutes(15))).isTrue();
        limiter.reset("a");
        assertThat(limiter.tryConsume("a", 1, Duration.ofMinutes(15))).isTrue();
        limiter.clear();
        assertThat(limiter.tryConsume("b", 1, Duration.ofMinutes(15))).isTrue();
    }

    @Test
    void configuracionInvalidaNoCreaVentanas() {
        assertThatThrownBy(() -> limiter.tryConsume("ip", 0, Duration.ofSeconds(1)))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> limiter.tryConsume("ip", 1, Duration.ZERO))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> limiter.tryConsume("ip", 1, Duration.ofSeconds(-1)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return now; }
    }
}
