package com.mathtutor.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;

/** Contratos reales sobre encabezados y errores, sin claves ni API externa. */
class SecurityNoApiKeyInLogsTest {
    @Test
    void credentialsOnlyTravelInHeaderAndProviderErrorsAreRedacted() throws Exception {
        String secret = "secret-fixture-not-a-real-key";
        var requestUri = new AtomicReference<String>();
        var requestBody = new AtomicReference<String>();
        var header = new AtomicReference<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requestUri.set(exchange.getRequestURI().toString());
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            header.set(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
            byte[] error = ("{\"error\":\"invalid key " + secret + "\"}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(401, error.length);
            exchange.getResponseBody().write(error);
            exchange.close();
        });
        server.start();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/";
            var chat = new SpringAiGoogleAiStudioChatModel(secret, "model", 0.2, 100, 0, 60000,
                    new ObjectMapper(), WebClient.builder().baseUrl(url).build());
            var embedding = new SpringAiGoogleAiStudioEmbeddingModel(secret, "model", 3, 0, 60000,
                    RestClient.builder().baseUrl(url).build());
            assertThatThrownBy(() -> chat.call(new Prompt("pregunta"))).hasMessageNotContaining(secret).hasNoCause();
            assertThat(header.get()).isEqualTo(secret);
            assertThat(requestUri.get()).doesNotContain(secret);
            assertThat(requestBody.get()).doesNotContain(secret);
            assertThatThrownBy(() -> chat.stream(new Prompt("pregunta")).collectList().block())
                    .hasMessageNotContaining(secret).hasNoCause();
            assertThatThrownBy(() -> embedding.embed("consulta")).hasMessageNotContaining(secret).hasNoCause();
            assertThat(header.get()).isEqualTo(secret);
            assertThat(requestUri.get()).doesNotContain(secret);
            assertThat(requestBody.get()).doesNotContain(secret);
            assertThat(chat.toString()).doesNotContain(secret);
            assertThat(embedding.toString()).doesNotContain(secret);
        } finally {
            server.stop(0);
        }
    }
}