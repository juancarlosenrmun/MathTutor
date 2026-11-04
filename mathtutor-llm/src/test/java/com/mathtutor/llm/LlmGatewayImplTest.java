package com.mathtutor.llm;

import com.mathtutor.common.dto.Prompt;
import com.mathtutor.common.port.LlmGateway;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de caja blanca para LlmGatewayImpl.
 * Verifica el comportamiento del esqueleto.
 */
class LlmGatewayImplTest {

    private final LlmGateway gateway = new LlmGatewayImpl();

    @Test
    void streamReturnsEmptyFlux() {
        // Given
        Prompt prompt = new Prompt(
            "System prompt",
            "User message",
            Collections.emptyList(),
            "",
            1000,
            0.7
        );

        // When
        Flux<String> result = gateway.stream(prompt);

        // Then - should return empty flux (skeleton implementation)
        StepVerifier.create(result)
            .verifyComplete();
    }

    @Test
    void getModelNameReturnsSkeletonName() {
        // When
        String modelName = gateway.getModelName();

        // Then
        assertThat(modelName).isEqualTo("skeleton-llm");
    }

    @Test
    void implementsLlmGatewayInterface() {
        // Then - verifies it implements the port
        assertThat(gateway).isInstanceOf(LlmGateway.class);
    }
}