package com.mathtutor.common.dto;

/** Reintento de petición terminal; no inicia una nueva llamada. */
public class ConversationTerminalException extends RuntimeException {
    public ConversationTerminalException(String status) { super("La petición anterior terminó con estado " + status + ". Inicia un turno nuevo."); }
}
