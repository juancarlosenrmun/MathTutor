package com.mathtutor.persistence.adapter;

/** @brief Medidas de un turno finalizado, sin identificadores ni contenido de la conversación. */
public record TurnObserved(String status, long latencyMs, Integer inputTokens, Integer outputTokens) {}
