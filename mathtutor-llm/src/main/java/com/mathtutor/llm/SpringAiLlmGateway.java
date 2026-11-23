package com.mathtutor.llm;

import com.mathtutor.common.dto.Prompt;
import com.mathtutor.common.port.LlmGateway;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import reactor.core.publisher.Flux;
import java.util.List;
import java.util.function.Consumer;

/** @brief Convierte un turno al contrato com?n ChatModel y conserva deltas y uso declarado. */
class SpringAiLlmGateway implements LlmGateway {
    private final ChatModel chatModel;
    private final String modelName;

    SpringAiLlmGateway(ChatModel chatModel, String modelName) {
        this.chatModel = chatModel;
        this.modelName = modelName;
    }

    @Override
    public Flux<String> stream(Prompt prompt) {
        return stream(prompt, ignored -> {});
    }

    @Override
    public Flux<String> stream(Prompt prompt, Consumer<Usage> usage) {
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
                    var measured = response.getMetadata().getUsage();
                    if (measured != null && measured.getTotalTokens() != null && measured.getTotalTokens() > 0) {
                        usage.accept(new Usage(measured.getPromptTokens(), measured.getCompletionTokens()));
                    }
                })
                .filter(response -> response.getResult() != null && response.getResult().getOutput().getText() != null
                        && !response.getResult().getOutput().getText().isEmpty())
                .map(response -> response.getResult().getOutput().getText());
    }

    @Override
    public String getModelName() { return modelName; }
}
