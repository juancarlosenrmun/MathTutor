package com.mathtutor.llm;

import com.mathtutor.common.dto.Prompt;
import com.mathtutor.common.port.LlmGateway;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Implementación de LlmGateway que usa Google AI Studio (Gemini) via Spring AI ChatModel.
 *
 * Perfil: "google-ai-studio" para activar cuando se tiene API key de Google AI Studio.
 * Este gateway delega en SpringAiGoogleAiStudioChatModel que implementa ChatModel.
 */
@Component
@Profile("google-ai-studio")
public class GoogleAiStudioLlmGateway implements LlmGateway {

    private final ChatModel chatModel;

    public GoogleAiStudioLlmGateway(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public Flux<String> stream(Prompt prompt) {
        // Convert our Prompt DTO to Spring AI Prompt
        org.springframework.ai.chat.prompt.Prompt springPrompt = buildSpringPrompt(prompt);

        // Stream responses and extract text deltas
        return chatModel.stream(springPrompt)
                .map(this::extractDelta);
    }

    @Override
    public String getModelName() {
        return "gemini-1.5-flash (Google AI Studio)";
    }

    /**
     * Convierte nuestro DTO Prompt a Spring AI Prompt.
     * Combina systemPrompt + userPrompt + contextChunks + constraints.
     */
    private org.springframework.ai.chat.prompt.Prompt buildSpringPrompt(Prompt prompt) {
        StringBuilder fullPrompt = new StringBuilder();

        // System prompt
        if (prompt.systemPrompt() != null && !prompt.systemPrompt().isEmpty()) {
            fullPrompt.append(prompt.systemPrompt()).append("\n\n");
        }

        // Context chunks (RAG)
        if (prompt.contextChunks() != null && !prompt.contextChunks().isEmpty()) {
            fullPrompt.append("CONTEXTO RELEVANTE:\n");
            for (int i = 0; i < prompt.contextChunks().size(); i++) {
                fullPrompt.append("--- Fragmento ").append(i + 1).append(" ---\n");
                fullPrompt.append(prompt.contextChunks().get(i)).append("\n\n");
            }
        }

        // Constraints
        if (prompt.constraints() != null && !prompt.constraints().isEmpty()) {
            fullPrompt.append("RESTRICCIONES:\n").append(prompt.constraints()).append("\n\n");
        }

        // User prompt
        fullPrompt.append("USUARIO: ").append(prompt.userPrompt());

        // Create Spring AI Prompt with the full prompt as user message
        return new org.springframework.ai.chat.prompt.Prompt(
                new org.springframework.ai.chat.messages.UserMessage(fullPrompt.toString())
        );
    }

    /**
     * Extrae el delta de texto de cada ChatResponse.
     * Como acumulamos el buffer en el ChatModel, cada respuesta contiene el texto completo hasta ese punto.
     * Devolvemos solo el incremento (delta) para streaming eficiente.
     */
    private String extractDelta(ChatResponse response) {
        if (response.getResults() == null || response.getResults().isEmpty()) {
            return "";
        }
        String fullText = response.getResults().get(0).getOutput().getText();
        // Note: In a production implementation, we'd track the previous length to emit only deltas.
        // For simplicity, we return the full accumulated text each time.
        // The frontend can handle this by replacing the previous content.
        return fullText;
    }
}