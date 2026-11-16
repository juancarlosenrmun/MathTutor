package com.mathtutor.tutor;

/** @brief Error uniforme para una sesión ajena, cerrada o inexistente. */
public final class SessionUnavailableException extends RuntimeException {
    public SessionUnavailableException() { super("Sesión no disponible"); }
}
