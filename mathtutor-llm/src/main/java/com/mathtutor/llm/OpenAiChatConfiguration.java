package com.mathtutor.llm;

import com.mathtutor.common.port.LlmGateway;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;

/** @brief Selecciona el proveedor de generación por perfil, independientemente de los embeddings. */
@Configuration(proxyBeanMethods = false)
@Profile("openai-chat")
public class OpenAiChatConfiguration {
    @Bean
    LlmGateway openAiChatGateway(
            @Value("${spring.ai.openai.api-key:}") String apiKey,
            @Value("${spring.ai.openai.base-url:https://api.openai.com}") String baseUrl,
            @Value("${spring.ai.openai.chat-model:gpt-4o-mini}") String model) {
        Assert.hasText(apiKey, "Configura la clave del proveedor OpenAI.");
        Assert.hasText(model, "Configura el modelo de chat OpenAI.");
        var options = OpenAiChatOptions.builder().model(model).streamUsage(true)
                .internalToolExecutionEnabled(false).build();
        var chat = OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder().baseUrl(baseUrl).apiKey(apiKey).build())
                .defaultOptions(options).retryTemplate(RetryTemplate.builder().maxAttempts(1).build()).build();
        return new SpringAiLlmGateway(chat, model + " (OpenAI)");
    }
}
