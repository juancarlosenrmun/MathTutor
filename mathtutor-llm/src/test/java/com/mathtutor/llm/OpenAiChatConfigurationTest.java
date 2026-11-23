package com.mathtutor.llm;

import com.mathtutor.common.dto.Prompt;
import com.mathtutor.common.port.LlmGateway;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiChatConfigurationTest {
    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withUserConfiguration(OpenAiChatConfiguration.class, StubLlmGateway.class);

    @Test void absentProfileKeepsTheStubAndActiveProfileRequiresCredentials() {
        context.run(app -> assertThat(app.getBean(LlmGateway.class)).isInstanceOf(StubLlmGateway.class));
        context.withPropertyValues("spring.profiles.active=openai-chat")
                .run(app -> assertThat(app).hasFailed());
    }

    @Test void configuredProviderStreamsFromItsHttpEndpointAndReportsUsage() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var calls = new AtomicInteger();
        var request = new AtomicReference<String>();
        server.createContext("/v1/chat/completions", exchange -> {
            calls.incrementAndGet();
            request.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = """
                    data: {"id":"mock","object":"chat.completion.chunk","created":1,"model":"configured","choices":[{"index":0,"delta":{"role":"assistant","content":"Pista"},"finish_reason":"stop"}]}

                    data: {"id":"mock","object":"chat.completion.chunk","created":1,"model":"configured","choices":[],"usage":{"prompt_tokens":11,"completion_tokens":3,"total_tokens":14}}

                    data: [DONE]

                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, response.length);
            try (var output = exchange.getResponseBody()) { output.write(response); }
        });
        server.start();
        try {
            context.withPropertyValues("spring.profiles.active=openai-chat", "spring.ai.openai.api-key=local-test",
                            "spring.ai.openai.chat-model=configured", "spring.ai.openai.base-url=http://127.0.0.1:" + server.getAddress().getPort())
                    .run(app -> {
                        assertThat(app).hasSingleBean(LlmGateway.class);
                        var gateway = app.getBean(LlmGateway.class);
                        var usage = new ArrayList<LlmGateway.Usage>();
                        assertThat(gateway.getModelName()).isEqualTo("configured (OpenAI)");
                        var prompt = new Prompt("Instrucción", "Pregunta", List.of("Apuntes"), "Reglas", 45, 0.3);
                        assertThat(gateway.stream(prompt, usage::add).collectList().block(java.time.Duration.ofSeconds(5)))
                                .containsExactly("Pista");
                        assertThat(usage).contains(new LlmGateway.Usage(11, 3));
                        assertThat(calls.get()).isOne();
                        assertThat(request.get()).contains("configured", "Instrucción", "Apuntes", "Pregunta", "45", "include_usage");
                    });
        } finally {
            server.stop(0);
        }
    }
}
