package com.mathtutor.common.auth.domain;

import java.time.Instant;

/**
 * Interfaz para eventos de dominio.
 * Permite auditoría y trazabilidad de acciones importantes (RNF-13).
 */
public interface DomainEvent {

    /**
     * Momento en que ocurrió el evento.
     *
     * @return instante de ocurrencia
     */
    Instant occurredAt();
}