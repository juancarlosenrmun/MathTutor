package com.mathtutor.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mathtutor.common.port.LlmGateway;
import com.mathtutor.llm.GoogleAiStudioLlmGateway;
import com.mathtutor.llm.SpringAiGoogleAiStudioChatModel;
import com.mathtutor.llm.SpringAiGoogleAiStudioEmbeddingModel;
import com.mathtutor.llm.StubLlmGateway;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/** Configuración real del perfil Google sin leer .env ni acceder a la red. */
class GoogleAiStudioConfigurationTest {
    @Test
    void googleConfigurationBindsEnvironmentWithoutDevAndRegistersSingleClients() throws Exception {
        var sources = new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml"));
        var google = sources.stream().filter(source -> "google-ai-studio".equals(
                source.getProperty("spring.config.activate.on-profile"))).findFirst().orElseThrow();
        new ApplicationContextRunner()
                .withInitializer(context -> {
                    context.getEnvironment().setActiveProfiles("google-ai-studio");
                    context.getEnvironment().getPropertySources().addLast(google);
                })
                .withPropertyValues("GOOGLE_AI_API_KEY=fixture-key", "GOOGLE_AI_CHAT_MODEL=configured-model",
                        "GOOGLE_AI_MAX_REQUESTS_PER_MINUTE=7")
                .withBean(ObjectMapper.class)
                .withUserConfiguration(SpringAiGoogleAiStudioChatModel.class, SpringAiGoogleAiStudioEmbeddingModel.class,
                        GoogleAiStudioLlmGateway.class, StubLlmGateway.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(ChatModel.class).hasSingleBean(EmbeddingModel.class).hasSingleBean(LlmGateway.class);
                    assertThat(context.getBean(LlmGateway.class).getModelName()).isEqualTo("configured-model (Google AI Studio)");
                    assertThat(context.getEnvironment().getProperty("spring.ai.google.ai-studio.api-key")).isEqualTo("fixture-key");
                    assertThat(context.getEnvironment().getProperty("spring.ai.google.ai-studio.max-requests-per-minute")).isEqualTo("7");
                    assertThat(context.getEnvironment().getProperty("spring.ai.google.ai-studio.max-retries")).isEqualTo("0");
                });
    }
}
