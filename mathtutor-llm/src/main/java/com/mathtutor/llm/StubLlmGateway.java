package com.mathtutor.llm;

import com.mathtutor.common.dto.Prompt;
import com.mathtutor.common.port.LlmGateway;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Implementación stub de LlmGateway para tests sin perfil Spring AI.
 * Devuelve una respuesta fija para permitir que el contexto arranque.
 */
@Component
@Profile("!spring-ai-pgvector")
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