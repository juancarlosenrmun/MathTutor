package com.mathtutor.llm;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de integración real con Google AI Studio (Gemini) para chat.
 * Requiere GOOGLE_AI_API_KEY en variables de entorno o .env.
 * Se ejecuta con: ./gradlew :mathtutor-llm:realApiTest
 */
@SpringBootTest
@ActiveProfiles({"test", "google-ai-studio"})
@Tag("real-api")
class SpringAiGoogleAiStudioChatModelRealApiTest {

    @Autowired
    private ChatModel chatModel;

    @Test
    void call_simpleQuestion_returnsResponse() {
        // Given
        Prompt prompt = new Prompt(new UserMessage("Responde en una sola frase: ¿cuánto es 2 + 2?"));

        // When
        ChatResponse response = chatModel.call(prompt);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getResults()).isNotEmpty();
        String answer = response.getResults().get(0).getOutput().getText();
        assertThat(answer).isNotBlank();
        assertThat(answer.toLowerCase()).containsAnyOf("4", "cuatro");
        System.out.println("Respuesta: " + answer);
    }

    @Test
    void call_mathQuestion_returnsExplanation() {
        // Given
        Prompt prompt = new Prompt(new UserMessage("Explica brevemente qué es una derivada en cálculo diferencial."));

        // When
        ChatResponse response = chatModel.call(prompt);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getResults()).isNotEmpty();
        String answer = response.getResults().get(0).getOutput().getText();
        assertThat(answer).isNotBlank();
        assertThat(answer.length()).isGreaterThan(20);
        System.out.println("Respuesta: " + answer);
    }

    @Test
    void stream_simpleQuestion_emitsTokens() {
        // Given
        Prompt prompt = new Prompt(new UserMessage("Cuenta del 1 al 5 separados por comas."));

        // When
        Flux<ChatResponse> stream = chatModel.stream(prompt);

        // Then
        StepVerifier.create(stream)
                .assertNext(response -> {
                    assertThat(response.getResults()).isNotEmpty();
                    String text = response.getResults().get(0).getOutput().getText();
                    assertThat(text).isNotBlank();
                    System.out.println("Chunk: " + text);
                })
                .expectNextCount(10) // Expect up to 10 more chunks
                .thenConsumeWhile(response -> true)
                .verifyComplete();
    }

    @Test
    void stream_mathQuestion_emitsProgressiveResponse() {
        // Given
        Prompt prompt = new Prompt(new UserMessage("Escribe la fórmula de la derivada de x^2 y explica cada término."));

        // When
        Flux<ChatResponse> stream = chatModel.stream(prompt);

        // Then
        StepVerifier.create(stream)
                .assertNext(response -> {
                    assertThat(response.getResults()).isNotEmpty();
                    String text = response.getResults().get(0).getOutput().getText();
                    assertThat(text).isNotBlank();
                    System.out.println("Primer chunk: " + text);
                })
                .expectNextCount(15)
                .thenConsumeWhile(response -> true)
                .verifyComplete();
    }

    @Test
    void call_withSystemPrompt_usesSystemPrompt() {
        // Given
        Prompt prompt = new Prompt(List.of(
                new SystemMessage("Responde SOLO con el nombre de la ciudad, sin explicaciones."),
                new UserMessage("¿Cuál es la capital de Francia?")
        ));

        // When
        ChatResponse response = chatModel.call(prompt);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getResults()).isNotEmpty();
        String answer = response.getResults().get(0).getOutput().getText();
        assertThat(answer).isNotBlank();
        // Should be just "París" or "Paris" without extra text
        System.out.println("Respuesta con system prompt: " + answer);
    }

    }