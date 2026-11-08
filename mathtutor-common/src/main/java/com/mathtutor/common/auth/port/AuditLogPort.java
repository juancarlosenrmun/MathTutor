package com.mathtutor.common.auth.port;

import com.mathtutor.common.auth.domain.DomainEvent;

/**
 * Puerto para persistencia de logs de auditoría.
 * Permite guardar eventos de dominio para trazabilidad (RNF-13).
 */
public interface AuditLogPort {

    /**
     * Guarda un evento de dominio como log de auditoría.
     *
     * @param event evento a persistir
     */
    void save(DomainEvent event);
}