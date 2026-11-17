package com.mathtutor.common.dto;

/** No distingue recursos ajenos de inexistentes. */
public class ConversationUnavailableException extends RuntimeException {
    public ConversationUnavailableException() { super("Sesión no disponible"); }
}
