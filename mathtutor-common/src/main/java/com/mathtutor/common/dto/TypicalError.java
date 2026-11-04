package com.mathtutor.common.dto;

/**
 * Error típico asociado a un paso canónico.
 */
public record TypicalError(
    String code,
    String seedHint
) {}