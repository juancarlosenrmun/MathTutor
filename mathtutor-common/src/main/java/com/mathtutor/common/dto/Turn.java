package com.mathtutor.common.dto;

import java.time.Instant;

/**
 * Un turno de interacción en la sesión de tutoría.
 */
public record Turn(
    Long id,
    String studentMessage,
    String tutorResponse,
    ErrorType diagnosis,
    HelpLevel helpLevel,
    long latencyMs,
    int tokensIn,
    int tokensOut,
    Instant at,
    GraphicSpec graphicSpec
) {}