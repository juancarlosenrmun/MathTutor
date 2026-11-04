package com.mathtutor.common.dto;

/**
 * Paso canónico en la resolución de un ejercicio.
 */
public record CanonicalStep(
    int position,
    String expressionLatex,
    String equivalentForms  // JSON array de formas equivalentes
) {}