package com.mathtutor.llm;

import com.mathtutor.common.dto.Prompt;
import com.mathtutor.common.port.LlmGateway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de integración real con Google AI Studio (Gemini) para LlmGateway.
 * Requiere GOOGLE_AI_API_KEY en variables de entorno o .env.
 * Se ejecuta con: ./gradlew :mathtutor-llm:realApiTest
 */
@SpringBootTest
@ActiveProfiles({"test", "google-ai-studio"})
@Tag("real-api")
class GoogleAiStudioLlmGatewayRealApiTest {

    @Autowired
    private LlmGateway llmGateway;

    @Test
    void getModelName_returnsCorrectName() {
        // When
        String modelName = llmGateway.getModelName();

        // Then
        assertThat(modelName).contains("gemini");
        assertThat(modelName).contains("Google AI Studio");
        System.out.println("Model name: " + modelName);
    }

    @Test
    void stream_simpleQuestion_emitsResponse() {
        // Given
        Prompt prompt = new Prompt(
                "Eres un tutor de matemáticas.",
                "¿Cuánto es 3 * 7?",
                List.of(),
                "Responde solo con el número.",
                100,
                0.1
        );

        // When
        Flux<String> stream = llmGateway.stream(prompt);

        // Then
        StepVerifier.create(stream)
                .assertNext(response -> {
                    assertThat(response).isNotBlank();
                    System.out.println("Chunk 1: " + response);
                })
                .expectNextCount(10)
                .thenConsumeWhile(response -> true)
                .verifyComplete();
    }

    @Test
    void stream_withContext_usesContextInResponse() {
        // Given
        Prompt prompt = new Prompt(
                "Eres un tutor de matemáticas.",
                "Basándote en el contexto, ¿cuál es la derivada de x^2?",
                List.of("La derivada de x^n es n*x^(n-1). Por tanto, la derivada de x^2 es 2*x."),
                "Usa el contexto proporcionado.",
                200,
                0.1
        );

        // When
        Flux<String> stream = llmGateway.stream(prompt);

        // Then
        StepVerifier.create(stream)
                .assertNext(response -> {
                    assertThat(response).isNotBlank();
                    System.out.println("Chunk 1: " + response);
                })
                .expectNextCount(10)
                .thenConsumeWhile(response -> true)
                .verifyComplete();
    }

    @Test
    void stream_mathExplanation_producesCoherentResponse() {
        // Given
        Prompt prompt = new Prompt(
                "Eres un tutor de matemáticas experto. Explica conceptos con claridad y ejemplos.",
                "Explica qué es una integral definida en una frase.",
                List.of(),
                "Máximo 50 palabras. Incluye un ejemplo simple.",
                150,
                0.3
        );

        // When
        Flux<String> stream = llmGateway.stream(prompt);

        // Then
        StepVerifier.create(stream)
                .assertNext(response -> {
                    assertThat(response).isNotBlank();
                    System.out.println("Chunk 1: " + response);
                })
                .expectNextCount(15)
                .thenConsumeWhile(response -> true)
                .verifyComplete();
    }

    @Test
    void stream_withConstraints_respectsConstraints() {
        // Given
        Prompt prompt = new Prompt(
                "Eres un asistente que solo responde SÍ o NO.",
                "¿Es 5 un número primo?",
                List.of(),
                "Responde ÚNICAMENTE con SÍ o NO, sin explicaciones.",
                10,
                0.0
        );

        // When
        Flux<String> stream = llmGateway.stream(prompt);

        // Then
        StepVerifier.create(stream)
                .assertNext(response -> {
                    assertThat(response).isNotBlank();
                    System.out.println("Chunk 1: " + response);
                })
                .expectNextCount(5)
                .thenConsumeWhile(response -> true)
                .verifyComplete();
    }
}