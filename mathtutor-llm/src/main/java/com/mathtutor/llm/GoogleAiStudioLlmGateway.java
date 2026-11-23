package com.mathtutor.llm;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** @brief Selecciona el modelo de Google AI Studio mediante su perfil de Spring. */
@Component
@Profile("google-ai-studio")
public class GoogleAiStudioLlmGateway extends SpringAiLlmGateway {
    public GoogleAiStudioLlmGateway(ChatModel chatModel,
            @Value("${spring.ai.google.ai-studio.chat-model:gemini-3.5-flash-lite}") String modelName) {
        super(chatModel, modelName + " (Google AI Studio)");
    }
}
