package com.mathtutor.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Contratos HTTP deterministas: no consumen cuota ni necesitan claves reales. */
class GoogleAiStudioContractTest {
    private HttpServer server;
    private String baseUrl;
    private final AtomicInteger requests = new AtomicInteger();
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private volatile String response;
    private volatile int status = 200;
    private volatile String contentType = "application/json";

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/";
    }

    @AfterEach
    void stopServer() { server.stop(0); }

    private SpringAiGoogleAiStudioChatModel chat() {
        return new SpringAiGoogleAiStudioChatModel("test-key", "configured-model", 0.2, 100,
                5, 60000, new ObjectMapper(), WebClient.builder().baseUrl(baseUrl).build());
    }

    private SpringAiGoogleAiStudioEmbeddingModel embedding() {
        return new SpringAiGoogleAiStudioEmbeddingModel("test-key", "gemini-embedding-001", 3,
                0, 60000, RestClient.builder().baseUrl(baseUrl).build());
    }

    @Test
    void streamingDecodesSseDataAndEmitsOnlyDeltas() {
        contentType = "text/event-stream";
        response = "data: {\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Hola\"}]}}]}\n\n"
                + "data: {\"candidates\":[{\"content\":{\"parts\":[{\"text\":\" mundo\"}]}}]}\n\n";
        var result = chat().stream(new Prompt("pregunta")).map(r -> r.getResult().getOutput().getText()).collectList().block();
        assertThat(result).containsExactly("Hola", " mundo");
        assertThat(requests).hasValue(1);
    }

    @Test
    void rateLimitNeverRetriesTheGeneration() {
        status = 429;
        response = "{\"error\":{\"message\":\"quota exhausted\"}}";
        assertThatThrownBy(() -> chat().call(new Prompt("pregunta"))).hasMessageContaining("HTTP 429");
        assertThat(requests).hasValue(1);
    }

    @Test
    void systemInstructionsAreSeparatedFromConversation() throws Exception {
        response = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"respuesta\"}]}}]}";
        chat().call(new Prompt(List.of(new SystemMessage("instrucción"), new UserMessage("pregunta"))));
        var body = new ObjectMapper().readTree(requestBody.get());
        assertThat(body.at("/systemInstruction/parts/0/text").asText()).isEqualTo("instrucción");
        assertThat(body.at("/contents/0/parts/0/text").asText()).isEqualTo("pregunta");
        assertThat(body.at("/contents").size()).isEqualTo(1);
    }

    @Test
    void blockedResponseIsAnExplicitError() {
        response = "{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}";
        assertThatThrownBy(() -> chat().call(new Prompt("pregunta"))).hasMessageContaining("respuesta");
    }

    @Test
    void malformedStreamIsAnExplicitError() {
        contentType = "text/event-stream";
        response = "data: invalid-json\n\n";
        assertThatThrownBy(() -> chat().stream(new Prompt("pregunta")).collectList().block())
                .hasMessageContaining("Google AI Studio");
    }

    @Test
    void embeddingUsesDocumentAndQueryTasksAndNormalizes() throws Exception {
        response = "{\"embedding\":{\"values\":[3,4,0]}}";
        var model = embedding();
        assertThat(model.embed("documento")).containsExactly(0.6f, 0.8f, 0f);
        assertThat(new ObjectMapper().readTree(requestBody.get()).get("taskType").asText()).isEqualTo("RETRIEVAL_QUERY");
        model.embed(new org.springframework.ai.document.Document("documento"));
        assertThat(new ObjectMapper().readTree(requestBody.get()).get("taskType").asText()).isEqualTo("RETRIEVAL_DOCUMENT");
        org.springframework.ai.embedding.BatchingStrategy batching = documents -> List.of(documents);
        model.embed(List.of(new org.springframework.ai.document.Document("otro documento")), null, batching);
        assertThat(new ObjectMapper().readTree(requestBody.get()).get("taskType").asText()).isEqualTo("RETRIEVAL_DOCUMENT");
    }

    @Test
    void embeddingRejectsWrongDimensionAndNonFiniteVector() {
        response = "{\"embedding\":{\"values\":[1,2]}}";
        assertThatThrownBy(() -> embedding().embed("texto")).hasMessageContaining("dimensi");
        response = "{\"embedding\":{\"values\":[0,0,0]}}";
        assertThatThrownBy(() -> embedding().embed("texto")).hasMessageContaining("vector");
    }

    @Test
    void cachedVectorsCannotBeMutatedByCallers() {
        response = "{\"embedding\":{\"values\":[3,4,0]}}";
        var model = embedding();
        model.embed("texto")[0] = 99;
        assertThat(model.embed("texto")).containsExactly(0.6f, 0.8f, 0f);
        assertThat(requests).hasValue(1);
    }

    @Test
    void embeddingHandlesEmptyInputsWithoutRequestsAndBatchesWithTasks() {
        response = "{\"embedding\":{\"values\":[3,4,0]}}";
        var model = embedding();
        assertThat(model.embed((String) null)).containsExactly(0, 0, 0);
        assertThat(model.embed(" ")).containsExactly(0, 0, 0);
        assertThat(model.embedBatch(null)).isEmpty();
        assertThat(model.embedBatchQueries(List.of())).isEmpty();
        assertThat(requests).hasValue(0);
        assertThat(model.dimensions()).isEqualTo(3);
        assertThat(model.embedBatch(List.of("uno", "dos"))).hasSize(2);
        assertThat(model.embedBatchQueries(List.of("consulta"))).hasSize(1);
        assertThat(model.call(new org.springframework.ai.embedding.EmbeddingRequest(List.of("consulta"), null)).getResults()).hasSize(1);
    }

    @Test
    void embeddingRejectsUnavailableOrMalformedProviderWithoutRetry() {
        status = 429;
        response = "{\"error\":\"quota\"}";
        assertThatThrownBy(() -> embedding().embed("texto")).hasMessageContaining("Google AI Studio");
        assertThat(requests).hasValue(1);
        status = 200;
        response = "{}";
        assertThatThrownBy(() -> embedding().embed("texto")).hasMessageContaining("ausente");
        response = "{\"embedding\":{\"values\":[\"not-number\",1,2]}}";
        assertThatThrownBy(() -> embedding().embed("texto")).hasMessageContaining("vector");
    }

    @Test
    void chatPreservesHistoryOptionsAndDropsThoughtParts() throws Exception {
        response = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"razonamiento\",\"thought\":true},{\"text\":\"respuesta\"}]},\"finishReason\":\"STOP\"}]}";
        var prompt = new Prompt(List.of(new UserMessage(" "), new UserMessage("uno"),
                new org.springframework.ai.chat.messages.AssistantMessage("dos"), new UserMessage("tres")),
                org.springframework.ai.chat.prompt.ChatOptions.builder().temperature(0.1).maxTokens(80).build());
        var model = chat();
        assertThat(model.call(prompt).getResult().getOutput().getText()).isEqualTo("respuesta");
        var body = new ObjectMapper().readTree(requestBody.get());
        assertThat(body.at("/contents/1/role").asText()).isEqualTo("model");
        assertThat(body.at("/generationConfig/maxOutputTokens").asInt()).isEqualTo(80);
        assertThat(body.at("/generationConfig").has("temperature")).isFalse();
        assertThat(model.getMaxTokens()).isEqualTo(100);
        assertThat(model.getModelName()).isEqualTo("configured-model");
    }

    @Test
    void chatRejectsNonTextAndSafetyAndMalformedResponses() {
        for (String payload : List.of("{}", "{\"candidates\":[{\"finishReason\":\"SAFETY\"}]}",
                "{\"candidates\":[{\"content\":{\"parts\":[]}}]}", "malformed")) {
            response = payload;
            assertThatThrownBy(() -> chat().call(new Prompt("pregunta"))).isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void streamIgnoresDoneAndUsageOnlyFinalCandidate() {
        contentType = "text/event-stream";
        response = "data: {\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"texto\"}]}}]}\n\n"
                + "data: {\"candidates\":[{\"finishReason\":\"STOP\"}]}\n\n"
                + "data: [DONE]\n\n";
        assertThat(chat().stream(new Prompt("pregunta")).collectList().block()).hasSize(1);
    }

    @Test
    void streamWithOnlyThoughtsFailsInsteadOfSilentlyCompleting() {
        contentType = "text/event-stream";
        response = "data: {\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"interno\",\"thought\":true}]}}]}\n\n";
        assertThatThrownBy(() -> chat().stream(new Prompt("pregunta")).collectList().block())
                .hasMessageContaining("Google AI Studio");
    }

    @Test
    void invalidPromptOptionsAreRejectedBeforeNetwork() {
        var options = org.springframework.ai.chat.prompt.ChatOptions.builder().temperature(-1.0).maxTokens(10).build();
        assertThatThrownBy(() -> chat().call(new Prompt("pregunta", options))).isInstanceOf(IllegalArgumentException.class);
        var invalidTokens = org.springframework.ai.chat.prompt.ChatOptions.builder().temperature(0.2).maxTokens(0).build();
        assertThatThrownBy(() -> chat().call(new Prompt("pregunta", invalidTokens))).isInstanceOf(IllegalArgumentException.class);
        assertThat(requests).hasValue(0);
    }

    @Test
    void remoteAdaptersRejectTransactionsBeforeAnyRequest() {
        response = "{}";
        var chat = chat();
        var model = embedding();
        // Se monta fuera de la transacción para verificar la guardia al suscribir.
        var stream = chat.stream(new Prompt("pregunta"));
        org.springframework.transaction.support.TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            assertThatThrownBy(() -> chat.call(new Prompt("pregunta"))).hasMessageContaining("transacción");
            assertThatThrownBy(() -> stream.collectList().block()).hasMessageContaining("Google AI Studio");
            assertThatThrownBy(() -> model.embed("texto")).hasMessageContaining("transacción");
            assertThat(requests).hasValue(0);
        } finally {
            org.springframework.transaction.support.TransactionSynchronizationManager.clear();
        }
    }
}
