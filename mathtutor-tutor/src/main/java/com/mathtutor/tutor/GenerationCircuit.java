package com.mathtutor.tutor;

import java.time.Duration;
import java.util.function.LongSupplier;

/** @brief Abre tras tres fallos consecutivos y admite una sola prueba de recuperación. */
final class GenerationCircuit {
    private static final long COOLDOWN = Duration.ofSeconds(30).toNanos();
    private final LongSupplier clock;
    private int failures;
    private long openedAt;
    private long generation;
    private boolean probing;

    GenerationCircuit(LongSupplier clock) {
        this.clock = clock;
    }

    synchronized Permit acquire() {
        if (failures < 3) return new Permit(generation, false);
        if (probing || clock.getAsLong() - openedAt < COOLDOWN) throw new Unavailable();
        probing = true;
        return new Permit(generation, true);
    }

    /** @brief Ignora resultados de solicitudes iniciadas antes de abrir el circuito. */
    synchronized void succeeded(Permit permit) {
        if (permit.generation() != generation) return;
        failures = 0;
        if (permit.probe()) {
            probing = false;
            generation++;
        }
    }

    synchronized void failed(Permit permit) {
        if (permit.generation() != generation) return;
        if (++failures >= 3) {
            openedAt = clock.getAsLong();
            probing = false;
            generation++;
        }
    }

    synchronized void release(Permit permit) {
        if (permit.generation() == generation && permit.probe()) probing = false;
    }

    record Permit(long generation, boolean probe) {}

    /** @brief Señal interna para continuar el turno con los apuntes ya recuperados. */
    static final class Unavailable extends RuntimeException {}
}
