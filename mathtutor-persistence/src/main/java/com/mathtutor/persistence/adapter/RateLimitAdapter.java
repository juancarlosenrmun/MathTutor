package com.mathtutor.persistence.adapter;

import com.mathtutor.common.auth.port.RateLimitPort;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementación en memoria de RateLimitPort usando ConcurrentHashMap.
 * Para producción considerar Redis o base de datos distribuida.
 *
 * Límite por defecto ASVS L1: 5 intentos por 15 minutos por IP/email.
 */
@Component
public class RateLimitAdapter implements RateLimitPort {

    /**
     * Limpia todo el store de rate limiting (útil para tests).
     */
    public void clear() {
        store.clear();
    }

    /**
     * Entrada de rate limiting con contador y timestamp de expiración.
     */
    private static class RateLimitEntry {
        final AtomicInteger count = new AtomicInteger(0);
        Instant expiresAt;

        RateLimitEntry(Duration window) {
            this.expiresAt = Instant.now().plus(window);
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }

        void resetExpiration(Duration window) {
            this.expiresAt = Instant.now().plus(window);
        }
    }

    private final Map<String, RateLimitEntry> store = new ConcurrentHashMap<>();

    @Override
    public boolean tryConsume(String key, int maxAttempts, Duration window) {
        // Limpiar entradas expiradas periódicamente (simple cleanup)
        if (store.size() > 10000) {
            cleanupExpired();
        }

        RateLimitEntry entry = store.compute(key, (k, existing) -> {
            if (existing == null || existing.isExpired()) {
                return new RateLimitEntry(window);
            }
            existing.resetExpiration(window);
            return existing;
        });

        int currentCount = entry.count.incrementAndGet();
        return currentCount <= maxAttempts;
    }

    @Override
    public void reset(String key) {
        store.remove(key);
    }

    /**
     * Limpia entradas expiradas del store.
     */
    private void cleanupExpired() {
        Instant now = Instant.now();
        store.entrySet().removeIf(entry -> entry.getValue().expiresAt.isBefore(now));
    }
}