package com.mathtutor.common.port;

import com.mathtutor.common.dto.Prompt;
import reactor.core.publisher.Flux;

/**
 * Puerto para el acceso al LLM.
 * Permite cambiar de proveedor/modelo solo por configuración (RNF-14).
 */
public interface LlmGateway {
    record Usage(Integer inputTokens, Integer outputTokens) {}

    /** Conserva ausencia de métrica cuando el proveedor no la publica. */
    default Flux<String> stream(Prompt prompt, java.util.function.Consumer<Usage> usage) { return stream(prompt); }
    /**
     * Envía un prompt al LLM y recibe la respuesta en streaming.
     *
     * @param prompt el prompt a enviar
     * @return flujo de tokens de la respuesta
     */
    Flux<String> stream(Prompt prompt);

    /**
     * Obtiene el nombre del modelo/proveedor actual.
     *
     * @return nombre identificativo
     */
    String getModelName();
}
