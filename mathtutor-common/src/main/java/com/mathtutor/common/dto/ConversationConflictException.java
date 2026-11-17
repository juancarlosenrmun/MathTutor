package com.mathtutor.common.dto;

/** Conflicto de petición o turno ya en curso; mensaje seguro para el alumno. */
public class ConversationConflictException extends RuntimeException {
    public ConversationConflictException() { super("Hay un turno en curso o la petición ya terminó. Consulta el historial."); }
}
