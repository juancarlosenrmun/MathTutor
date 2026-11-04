package com.mathtutor.llm;

import com.mathtutor.common.port.LlmGateway;
import com.mathtutor.common.dto.Prompt;
import reactor.core.publisher.Flux;

/**
 * Esqueleto de implementación del puerto de acceso al LLM.
 * Implementación vacía preparada para EN-04 (Spring AI + pgvector).
 * No usa Spring AI - solo define la estructura.
 */
public class LlmGatewayImpl implements LlmGateway {

    @Override
    public Flux<String> stream(Prompt prompt) {
        // TODO: Implementar en EN-04 con Spring AI
        // Por ahora devuelve flujo vacío
        return Flux.empty();
    }

    @Override
    public String getModelName() {
        // TODO: Configurar en EN-04
        return "skeleton-llm";
    }
}