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
 *
 * <p><strong>Limitación importante:</strong> Esta implementación es <strong>in-memory</strong>
 * y <strong>NO es adecuada para despliegues multi-instancia</strong> (clúster, Kubernetes, etc.)
 * porque cada instancia mantiene su propio contador independiente.</p>
 *
 * <p>Para producción con múltiples instancias, se debe usar una implementación distribuida
 * basada en Redis (ej. <code>RedisRateLimitAdapter</code>) o base de datos compartida.</p>
 *
 * <p>Límite por defecto ASVS L1: 5 intentos por 15 minutos por IP/email.</p>
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