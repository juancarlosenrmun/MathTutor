package com.mathtutor.llm;

import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import java.time.Duration;

/** Espaciado local configurable; la cuota real depende del proyecto y del modelo. */
final class GoogleAiStudioRateLimiter {
    private final long intervalNanos;
    private long nextRequest;

    GoogleAiStudioRateLimiter(int requestsPerMinute) {
        Assert.isTrue(requestsPerMinute > 0, "El límite de peticiones por minuto debe ser positivo");
        intervalNanos = Duration.ofMinutes(1).toNanos() / requestsPerMinute;
    }

    private synchronized Duration reserve() {
        long now = System.nanoTime();
        long delay = Math.max(0, nextRequest - now);
        nextRequest = now + delay + intervalNanos;
        return Duration.ofNanos(delay);
    }

    Mono<Void> acquireAsync() {
        return Mono.defer(() -> Mono.delay(reserve()).then());
    }

    void acquire() {
        try {
            java.util.concurrent.TimeUnit.NANOSECONDS.sleep(reserve().toNanos());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Espera de cuota de Google AI Studio interrumpida", e);
        }
    }
}
