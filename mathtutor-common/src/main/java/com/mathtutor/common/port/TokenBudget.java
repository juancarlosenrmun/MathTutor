package com.mathtutor.common.port;

import java.time.LocalDate;
import java.util.Optional;

/** @brief Cuota diaria compartida por todas las sesiones del usuario. */
public interface TokenBudget {
    record Reservation(long user, LocalDate day, int input, int output) {
        public int total() {
            return Math.addExact(input, output);
        }
    }

    /** @brief Reserva capacidad antes de llamar al proveedor. El día se fija en UTC. */
    Optional<Reservation> reserve(long user, int input, int output);

    /** @brief Liquida la reserva con el uso publicado o conserva la estimación que falte. */
    void settle(Reservation reservation, LlmGateway.Usage usage);
}
