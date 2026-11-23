package com.mathtutor.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Adaptador Gemini con una única petición de generación por invocación, sin reintentos. */
@Component
@Profile("google-ai-studio & !openai-chat")
public class SpringAiGoogleAiStudioChatModel implements ChatModel {
    static final String DEFAULT_CHAT_MODEL = "gemini-3.5-flash-lite";
    private final WebClient webClient;
    private final String chatModel;
    private final double temperature;
    private final int maxOutputTokens;
    private final ObjectMapper objectMapper;
    private final GoogleAiStudioRateLimiter rateLimiter;

    @Autowired
    public SpringAiGoogleAiStudioChatModel(
            @Value("${spring.ai.google.ai-studio.api-key:}") String apiKey,
            @Value("${spring.ai.google.ai-studio.chat-model:" + DEFAULT_CHAT_MODEL + "}") String chatModel,
            @Value("${spring.ai.google.ai-studio.temperature:0.7}") double temperature,
            @Value("${spring.ai.google.ai-studio.max-output-tokens:8192}") int maxOutputTokens,
            @Value("${spring.ai.google.ai-studio.max-retries:0}") int maxRetries,
            @Value("${spring.ai.google.ai-studio.max-requests-per-minute:5}") int maxRequestsPerMinute,
            ObjectMapper objectMapper) {
        this(apiKey, chatModel, temperature, maxOutputTokens, maxRetries, maxRequestsPerMinute,
                objectMapper, WebClient.builder()
                        .baseUrl("https://generativelanguage.googleapis.com/v1beta/")
                        .defaultHeader("x-goog-api-key", apiKey == null ? "" : apiKey)
                        .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
                                .disableRetry(true)
                                .responseTimeout(Duration.ofSeconds(30))
                                .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)))
                        .build());
    }

    SpringAiGoogleAiStudioChatModel(String apiKey, String chatModel, double temperature,
            int maxOutputTokens, int maxRetries, int maxRequestsPerMinute,
            ObjectMapper objectMapper, WebClient webClient) {
        Assert.hasText(apiKey, "Google AI Studio API key must be set");
        Assert.hasText(chatModel, "Debe configurarse el modelo de Google AI Studio");
        Assert.isTrue(Double.isFinite(temperature) && temperature >= 0 && temperature <= 2,
                "La temperatura debe estar entre 0 y 2");
        Assert.isTrue(maxOutputTokens > 0, "El límite de tokens debe ser positivo");
        Assert.isTrue(maxRetries >= 0, "El número de reintentos no puede ser negativo");
        this.chatModel = chatModel;
        this.temperature = temperature;
        this.maxOutputTokens = maxOutputTokens;
        this.objectMapper = objectMapper;
        this.webClient = webClient.mutate().defaultHeader("x-goog-api-key", apiKey).build();
        this.rateLimiter = new GoogleAiStudioRateLimiter(maxRequestsPerMinute);
        // maxRetries se conserva por compatibilidad; nunca se reintenta una generación.
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        Map<String, Object> request = buildRequest(prompt);
        Assert.state(!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive(),
                "No se permite llamar a Google AI Studio dentro de una transacción");
        rateLimiter.acquire();
        try {
            String response = webClient.post().uri("models/" + chatModel + ":generateContent")
                    .bodyValue(request).retrieve().bodyToMono(String.class)
                    .block(Duration.ofSeconds(30));
            return parseResponse(response, false);
        } catch (Exception e) {
            // El cuerpo del error remoto podría repetir credenciales o datos del prompt.
            throw sanitizedError(e);
        }
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        return Flux.defer(() -> {
            Assert.state(!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive(),
                    "No se permite llamar a Google AI Studio dentro de una transacción");
            return rateLimiter.acquireAsync().thenMany(webClient.post()
                .uri("models/" + chatModel + ":streamGenerateContent?alt=sse")
                .bodyValue(buildRequest(prompt)).retrieve()
                // WebClient decodifica SSE y entrega data sin el prefijo data:.
                .bodyToFlux(String.class)
                .filter(data -> !"[DONE]".equals(data.trim()))
                .map(data -> parseResponse(data, true))
                .filter(response -> !response.getResult().getOutput().getText().isEmpty()
                        || response.getMetadata().getUsage().getTotalTokens()>0)
                .switchIfEmpty(Flux.error(new IllegalStateException("Respuesta textual vacía")))
                .timeout(Duration.ofSeconds(30)));
        })
                .onErrorMap(this::sanitizedError);
    }

    private IllegalStateException sanitizedError(Throwable error) {
        String status = error instanceof org.springframework.web.reactive.function.client.WebClientResponseException http
                ? " (HTTP " + http.getStatusCode().value() + ")" : "";
        return new IllegalStateException("Error de respuesta de Google AI Studio" + status);
    }

    private Map<String, Object> buildRequest(Prompt prompt) {
        List<Map<String, Object>> contents = new ArrayList<>();
        List<Map<String, Object>> systemParts = new ArrayList<>();
        for (var message : prompt.getInstructions()) {
            if (message.getText() == null || message.getText().isBlank()) continue;
            Map<String, Object> part = Map.of("text", message.getText());
            if (message instanceof SystemMessage) systemParts.add(part);
            else contents.add(Map.of("role", message instanceof AssistantMessage ? "model" : "user", "parts", List.of(part)));
        }
        Assert.notEmpty(contents, "El prompt debe contener un mensaje de usuario");
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("contents", contents);
        if (!systemParts.isEmpty()) request.put("systemInstruction", Map.of("parts", systemParts));
        double effectiveTemperature = prompt.getOptions() != null && prompt.getOptions().getTemperature() != null
                ? prompt.getOptions().getTemperature() : temperature;
        int effectiveTokens = prompt.getOptions() != null && prompt.getOptions().getMaxTokens() != null
                ? Math.min(maxOutputTokens, prompt.getOptions().getMaxTokens()) : maxOutputTokens;
        Assert.isTrue(Double.isFinite(effectiveTemperature) && effectiveTemperature >= 0 && effectiveTemperature <= 2,
                "La temperatura debe estar entre 0 y 2");
        Assert.isTrue(effectiveTokens > 0, "El límite de tokens debe ser positivo");
        // Gemini 3.5 Flash-Lite y posteriores deprecian los parámetros de muestreo.
        // Se validan opciones legacy, pero no se envía temperature/topP/topK a la API.
        request.put("generationConfig", Map.of("maxOutputTokens", effectiveTokens));
        return request;
    }

    private ChatResponse parseResponse(String json, boolean streaming) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode candidate = root.path("candidates").path(0);
            if (candidate.isMissingNode() || root.has("promptFeedback") && root.path("promptFeedback").has("blockReason"))
                throw new IllegalStateException("El proveedor no ha devuelto una respuesta utilizable");
            String finishReason = candidate.path("finishReason").asText();
            if (!finishReason.isEmpty() && !List.of("STOP", "MAX_TOKENS").contains(finishReason))
                throw new IllegalStateException("El proveedor ha bloqueado la respuesta");
            StringBuilder text = new StringBuilder();
            for (JsonNode part : candidate.path("content").path("parts")) {
                if (!part.path("thought").asBoolean(false) && part.has("text")) text.append(part.get("text").asText());
            }
            if (!streaming && text.isEmpty()) throw new IllegalStateException("El proveedor no ha devuelto una respuesta textual");
            var metadata=org.springframework.ai.chat.metadata.ChatResponseMetadata.builder();
            var usage=root.path("usageMetadata");
            if (usage.path("promptTokenCount").canConvertToInt() && usage.path("candidatesTokenCount").canConvertToInt()) {
                int input=usage.path("promptTokenCount").asInt(), output=usage.path("candidatesTokenCount").asInt();
                metadata.usage(new org.springframework.ai.chat.metadata.DefaultUsage(input,output));
            }
            return new ChatResponse(List.of(new Generation(new AssistantMessage(text.toString()))),metadata.build());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Respuesta JSON inválida de Google AI Studio", e);
        }
    }

    public int getMaxTokens() { return maxOutputTokens; }
    public String getModelName() { return chatModel; }
}
