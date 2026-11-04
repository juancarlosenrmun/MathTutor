package com.mathtutor.common.port;

import com.mathtutor.common.dto.Turn;

/**
 * Puerto para registro de interacciones (auditoría y trazabilidad).
 */
public interface InteractionLogger {

    /**
     * Registra un turno de interacción.
     *
     * @param turn turno a registrar
     */
    void record(Turn turn);
}