package com.mathtutor.persistence.adapter;

import com.mathtutor.common.auth.port.RateLimitPort;
import org.springframework.stereotype.Component;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** Ventanas fijas por clave para el monolito; los intentos denegados no prolongan el bloqueo. */
@Component
public class RateLimitAdapter implements RateLimitPort {
    private record Entry(int count, Instant expiresAt) {}
    private final Map<String, Entry> store = new ConcurrentHashMap<>();
    private final Clock clock;

    public RateLimitAdapter() { this(Clock.systemUTC()); }
    RateLimitAdapter(Clock clock) { this.clock = clock; }

    public void clear() { store.clear(); }

    @Override
    public boolean tryConsume(String key, int maxAttempts, Duration window) {
        if (key == null || key.isBlank() || maxAttempts <= 0 || window == null
                || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("La clave, los intentos y la ventana deben ser válidos");
        }
        Instant now = clock.instant();
        if (store.size() > 10000) {
            store.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
        }
        AtomicBoolean allowed = new AtomicBoolean();
        store.compute(key, (ignored, previous) -> {
            Entry current = previous == null || !previous.expiresAt().isAfter(now)
                    ? new Entry(0, now.plus(window)) : previous;
            if (current.count() >= maxAttempts) return current;
            allowed.set(true);
            return new Entry(current.count() + 1, current.expiresAt());
        });
        return allowed.get();
    }

    @Override
    public void reset(String key) { store.remove(key); }
}
