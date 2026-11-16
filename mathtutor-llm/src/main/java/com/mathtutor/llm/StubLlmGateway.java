package com.mathtutor.llm;

import com.mathtutor.common.dto.Prompt;
import com.mathtutor.common.port.LlmGateway;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import reactor.core.publisher.Flux;

/**
 * Implementación stub de LlmGateway para tests.
 * Devuelve una respuesta fija para permitir que el contexto arranque.
 */
@Component
@Profile("!google-ai-studio")
public class StubLlmGateway implements LlmGateway {

    @Override
    public Flux<String> stream(Prompt prompt) {
        return Flux.just("Respuesta stub del LLM");
    }

    @Override
    public String getModelName() {
        return "stub-llm";
    }
}
