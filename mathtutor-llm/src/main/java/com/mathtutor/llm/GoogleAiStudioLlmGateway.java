package com.mathtutor.llm;

import com.mathtutor.common.dto.Prompt;
import com.mathtutor.common.port.LlmGateway;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import java.util.List;

/** Gateway de Google AI Studio; conserva instrucciones del sistema y emite deltas. */
@Component
@Profile("google-ai-studio")
public class GoogleAiStudioLlmGateway implements LlmGateway {
    private final ChatModel chatModel;
    private final String modelName;

    public GoogleAiStudioLlmGateway(ChatModel chatModel,
            @Value("${spring.ai.google.ai-studio.chat-model:gemini-3.5-flash-lite}") String modelName) {
        this.chatModel = chatModel;
        this.modelName = modelName;
    }

    @Override
    public Flux<String> stream(Prompt prompt) {
        return stream(prompt, ignored -> {});
    }

    @Override
    public Flux<String> stream(Prompt prompt, java.util.function.Consumer<LlmGateway.Usage> usage) {
        StringBuilder user = new StringBuilder();
        if (prompt.contextChunks() != null && !prompt.contextChunks().isEmpty()) {
            user.append("CONTEXTO RELEVANTE:\n");
            prompt.contextChunks().forEach(chunk -> user.append(chunk).append("\n\n"));
        }
        user.append("USUARIO: ").append(prompt.userPrompt());
        String system = prompt.systemPrompt() == null ? "" : prompt.systemPrompt();
        if (prompt.constraints() != null && !prompt.constraints().isBlank())
            system += "\nRESTRICCIONES:\n" + prompt.constraints();
        var options = ChatOptions.builder().temperature(prompt.temperature()).maxTokens(prompt.maxTokens()).build();
        var springPrompt = new org.springframework.ai.chat.prompt.Prompt(
                List.of(new SystemMessage(system), new UserMessage(user.toString())), options);
        return chatModel.stream(springPrompt)
                .doOnNext(response -> {
                    var measured=response.getMetadata().getUsage();
                    if (measured!=null && measured.getTotalTokens()>0)
                        usage.accept(new LlmGateway.Usage(measured.getPromptTokens(),measured.getCompletionTokens()));
                })
                .filter(response -> response.getResult() != null && response.getResult().getOutput().getText() != null
                        && !response.getResult().getOutput().getText().isEmpty())
                .map(response -> response.getResult().getOutput().getText());
    }

    @Override
    public String getModelName() { return modelName + " (Google AI Studio)"; }
}
