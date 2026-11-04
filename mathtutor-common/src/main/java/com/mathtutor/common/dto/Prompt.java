package com.mathtutor.common.dto;

import java.util.List;

/**
 * Prompt construido para enviar al LLM.
 */
public record Prompt(
    String systemPrompt,
    String userPrompt,
    List<String> contextChunks,
    String constraints,
    int maxTokens,
    double temperature
) {}