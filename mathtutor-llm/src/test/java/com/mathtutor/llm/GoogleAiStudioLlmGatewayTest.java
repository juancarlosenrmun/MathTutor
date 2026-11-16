package com.mathtutor.llm;

import com.mathtutor.common.dto.Prompt;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import reactor.core.publisher.Flux;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GoogleAiStudioLlmGatewayTest {
    @Test
    void googleProfileRegistersExactlyOneGatewayWithoutCallingNetwork() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withInitializer(context -> context.getEnvironment().setActiveProfiles("google-ai-studio"))
                .withBean(com.fasterxml.jackson.databind.ObjectMapper.class)
                .withPropertyValues("spring.ai.google.ai-studio.api-key=test-key",
                        "spring.ai.google.ai-studio.chat-model=configured-model")
                .withUserConfiguration(SpringAiGoogleAiStudioChatModel.class, GoogleAiStudioLlmGateway.class, StubLlmGateway.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(com.mathtutor.common.port.LlmGateway.class);
                    assertThat(context.getBean(com.mathtutor.common.port.LlmGateway.class).getModelName())
                            .isEqualTo("configured-model (Google AI Studio)");
                });
    }

    @Test
    void preservesSystemAndOptionsWithOneCallAndDeltas() {
        ChatModel model = mock(ChatModel.class);
        when(model.stream(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(Flux.just(
                new ChatResponse(List.of(new Generation(new AssistantMessage("uno")))),
                new ChatResponse(List.of(new Generation(new AssistantMessage(" dos"))))));
        var gateway = new GoogleAiStudioLlmGateway(model, "configured-model");
        var prompt = new Prompt("instrucción", "pregunta", List.of("contexto"), "restricción", 77, 0.2);
        assertThat(gateway.stream(prompt).collectList().block()).containsExactly("uno", " dos");
        var capture = ArgumentCaptor.forClass(org.springframework.ai.chat.prompt.Prompt.class);
        verify(model, times(1)).stream(capture.capture());
        assertThat(capture.getValue().getInstructions().get(0)).isInstanceOf(SystemMessage.class);
        assertThat(capture.getValue().getSystemMessage().getText()).contains("instrucción", "restricción");
        assertThat(capture.getValue().getUserMessage().getText()).contains("contexto", "pregunta");
        assertThat(capture.getValue().getOptions().getMaxTokens()).isEqualTo(77);
        assertThat(gateway.getModelName()).isEqualTo("configured-model (Google AI Studio)");
    }

    @Test
    void acceptsNoOptionalContextAndIgnoresEmptyResponses() {
        ChatModel model = mock(ChatModel.class);
        when(model.stream(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(Flux.just(new ChatResponse(List.of())));
        var gateway = new GoogleAiStudioLlmGateway(model, "model");
        assertThat(gateway.stream(new Prompt(null, "pregunta", null, null, 100, 0.2)).collectList().block()).isEmpty();
    }
}
