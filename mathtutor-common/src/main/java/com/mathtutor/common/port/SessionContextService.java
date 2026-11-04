package com.mathtutor.common.port;

import com.mathtutor.common.dto.SessionState;
import com.mathtutor.common.dto.Turn;

/**
 * Puerto para gestionar el contexto de la sesión de tutoría.
 */
public interface SessionContextService {

    /**
     * Carga el estado de una sesión.
     *
     * @param sessionId ID de la sesión
     * @return estado de la sesión
     */
    SessionState load(String sessionId);

    /**
     * Actualiza el estado de la sesión con un nuevo turno.
     *
     * @param sessionId ID de la sesión
     * @param turn      turno a añadir
     */
    void update(String sessionId, Turn turn);

    /**
     * Crea una nueva sesión.
     *
     * @param userId    ID del usuario
     * @param moduleId  ID del módulo
     * @return ID de la nueva sesión
     */
    String create(String userId, Long moduleId);

    /**
     * Cierra una sesión.
     *
     * @param sessionId ID de la sesión
     */
    void close(String sessionId);
}