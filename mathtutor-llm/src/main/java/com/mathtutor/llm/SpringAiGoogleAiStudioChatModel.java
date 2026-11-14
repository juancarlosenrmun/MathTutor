package com.mathtutor.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Adaptador de Spring AI ChatModel que usa Google AI Studio (Gemini) API directamente.
 * Modelo: gemini-1.5-flash / gemini-1.5-pro / gemini-2.0-flash - multilingüe.
 *
 * Perfil: "google-ai-studio" para activar cuando se tiene API key de Google AI Studio.
 *
 * API Reference: https://ai.google.dev/gemini-api/docs/text-generation
 */
@Component
@Profile("google-ai-studio")
public class SpringAiGoogleAiStudioChatModel implements ChatModel {

    private static final String DEFAULT_CHAT_MODEL = "gemini-1.5-flash";
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/";

    private final WebClient webClient;
    private final String apiKey;
    private final String chatModel;
    private final double temperature;
    private final int maxOutputTokens;
    private final int maxRetries;
    private final int maxRequestsPerMinute;
    private final ObjectMapper objectMapper;

    // Rate limiting
    private long lastRequestTime = 0;
    private final Object rateLimitLock = new Object();

    public SpringAiGoogleAiStudioChatModel(
            @Value("${spring.ai.google.ai-studio.api-key:}") String apiKey,
            @Value("${spring.ai.google.ai-studio.chat-model:" + DEFAULT_CHAT_MODEL + "}") String chatModel,
            @Value("${spring.ai.google.ai-studio.temperature:0.7}") double temperature,
            @Value("${spring.ai.google.ai-studio.max-output-tokens:8192}") int maxOutputTokens,
            @Value("${spring.ai.google.ai-studio.max-retries:3}") int maxRetries,
            @Value("${spring.ai.google.ai-studio.max-requests-per-minute:60}") int maxRequestsPerMinute,
            ObjectMapper objectMapper) {
        Assert.hasText(apiKey, "Google AI Studio API key must be set via spring.ai.google.ai-studio.api-key or GOOGLE_AI_API_KEY env var");
        Assert.hasText(chatModel, "Chat model name must be set");
        this.apiKey = apiKey;
        this.chatModel = chatModel;
        this.temperature = temperature;
        this.maxOutputTokens = maxOutputTokens;
        this.maxRetries = maxRetries;
        this.maxRequestsPerMinute = maxRequestsPerMinute;
        this.objectMapper = objectMapper;

        // Use x-goog-api-key header as per Google AI Studio API documentation
        // Configure 300s timeout for chat requests (longer than embedding due to generation time)
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(300))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(300)));

        this.webClient = WebClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("x-goog-api-key", apiKey)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        List<Map<String, Object>> contents = buildContents(prompt);

        Map<String, Object> request = Map.of(
                "contents", contents,
                "generationConfig", Map.of(
                        "temperature", temperature,
                        "maxOutputTokens", maxOutputTokens
                )
        );

        // Use WebClient for non-streaming call with proper timeout
        Map<String, Object> response = executeWithRetry(() -> webClient.post()
                .uri(uriBuilder -> uriBuilder.path("models/" + chatModel + ":generateContent").build())
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block());

        return parseResponse(response);
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        List<Map<String, Object>> contents = buildContents(prompt);

        Map<String, Object> request = Map.of(
                "contents", contents,
                "generationConfig", Map.of(
                        "temperature", temperature,
                        "maxOutputTokens", maxOutputTokens
                )
        );

        // Google AI Studio supports streaming via Server-Sent Events
        return webClient.post()
                .uri(uriBuilder -> uriBuilder.path("models/" + chatModel + ":streamGenerateContent").queryParam("alt", "sse").build())
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap(this::parseStreamingResponse)
                .onErrorResume(e -> Flux.error(new IllegalStateException("Streaming error: " + e.getMessage(), e)));
    }

    private List<Map<String, Object>> buildContents(Prompt prompt) {
        List<Map<String, Object>> contents = new ArrayList<>();

        // Get system message if present
        String systemPrompt = prompt.getSystemMessage().getText();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            contents.add(Map.of(
                    "role", "user",
                    "parts", List.of(Map.of("text", systemPrompt))
            ));
            contents.add(Map.of(
                    "role", "model",
                    "parts", List.of(Map.of("text", "Entendido. ¿En qué puedo ayudarte?"))
            ));
        }

        // Get user message
        String userPrompt = prompt.getUserMessage().getText();
        if (userPrompt != null && !userPrompt.isEmpty()) {
            contents.add(Map.of(
                    "role", "user",
                    "parts", List.of(Map.of("text", userPrompt))
            ));
        }

        return contents;
    }

    private ChatResponse parseResponse(Map<String, Object> response) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");

        if (candidates == null || candidates.isEmpty()) {
            return new ChatResponse(List.of(new Generation(new AssistantMessage(""))), new ChatResponseMetadata());
        }

        Map<String, Object> candidate = candidates.get(0);
        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) candidate.get("content");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");

        StringBuilder text = new StringBuilder();
        for (Map<String, Object> part : parts) {
            Object textObj = part.get("text");
            if (textObj != null) {
                text.append(textObj);
            }
        }

        Generation generation = new Generation(new AssistantMessage(text.toString()));
        return new ChatResponse(List.of(generation), new ChatResponseMetadata());
    }

    private Flux<ChatResponse> parseStreamingResponse(String sseData) {
        // Parse SSE format: each line starts with "data: "
        String[] lines = sseData.split("\n");
        StringBuilder buffer = new StringBuilder();
        List<ChatResponse> responses = new ArrayList<>();

        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("data: ")) {
                String data = line.substring(6).trim();
                if ("[DONE]".equals(data)) {
                    continue;
                }
                try {
                    // Parse the JSON chunk
                    // Google AI Studio streaming returns chunks with candidates[0].content.parts[0].text
                    // We accumulate and emit each delta
                    Map<String, Object> chunk = parseJson(data);
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> candidates = (List<Map<String, Object>>) chunk.get("candidates");
                    if (candidates != null && !candidates.isEmpty()) {
                        Map<String, Object> candidate = candidates.get(0);
                        @SuppressWarnings("unchecked")
                        Map<String, Object> content = (Map<String, Object>) candidate.get("content");
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                        for (Map<String, Object> part : parts) {
                            Object textObj = part.get("text");
                            if (textObj != null) {
                                String delta = textObj.toString();
                                buffer.append(delta);
                                Generation generation = new Generation(new AssistantMessage(buffer.toString()));
                                responses.add(new ChatResponse(List.of(generation), new ChatResponseMetadata()));
                            }
                        }
                    }
                } catch (Exception e) {
                    // Ignore parse errors for malformed chunks
                }
            }
        }
        return Flux.fromIterable(responses);
    }

    private Map<String, Object> parseJson(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            throw new IllegalStateException("Error parsing JSON: " + e.getMessage(), e);
        }
    }

    public int getMaxTokens() {
        return maxOutputTokens;
    }

    /**
     * Ejecuta una operación con reintentos exponenciales y jitter para 429/5xx.
     * Incluye rate limiting para respetar maxRequestsPerMinute.
     * Respeta el header Retry-After en respuestas 429.
     */
    private <T> T executeWithRetry(java.util.function.Supplier<T> operation) {
        int attempt = 0;
        while (true) {
            // Rate limiting: wait if necessary to respect maxRequestsPerMinute
            long minIntervalMs = 60_000L / Math.max(1, maxRequestsPerMinute);
            synchronized (rateLimitLock) {
                long now = System.currentTimeMillis();
                long timeSinceLastRequest = now - lastRequestTime;
                if (timeSinceLastRequest < minIntervalMs) {
                    long waitTime = minIntervalMs - timeSinceLastRequest;
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("Interrupted during rate limiting", ie);
                    }
                }
                lastRequestTime = System.currentTimeMillis();
            }

            try {
                return operation.get();
            } catch (WebClientResponseException.TooManyRequests e) {
                // Handle 429 with Retry-After header support
                if (attempt >= maxRetries) {
                    throw e;
                }
                attempt++;
                long delay = extractRetryAfterDelay(e);
                if (delay <= 0) {
                    // Exponential backoff with jitter if no Retry-After header
                    long baseDelay = (long) Math.pow(2, attempt) * 1000L; // 2s, 4s, 8s...
                    long jitter = ThreadLocalRandom.current().nextLong(0, 1000);
                    delay = baseDelay + jitter;
                }
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted during retry", ie);
                }
            } catch (WebClientResponseException.ServiceUnavailable e) {
                // Handle 503 with Retry-After header support
                if (attempt >= maxRetries) {
                    throw e;
                }
                attempt++;
                long delay = extractRetryAfterDelay(e);
                if (delay <= 0) {
                    // Exponential backoff with jitter if no Retry-After header
                    long baseDelay = (long) Math.pow(2, attempt) * 1000L; // 2s, 4s, 8s...
                    long jitter = ThreadLocalRandom.current().nextLong(0, 1000);
                    delay = baseDelay + jitter;
                }
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted during retry", ie);
                }
            } catch (WebClientResponseException e) {
                int status = e.getRawStatusCode();
                boolean retryable = status >= 500;
                if (!retryable || attempt >= maxRetries) {
                    throw e;
                }
                attempt++;
                long baseDelay = (long) Math.pow(2, attempt) * 1000L; // 2s, 4s, 8s...
                long jitter = ThreadLocalRandom.current().nextLong(0, 1000);
                long delay = baseDelay + jitter;
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted during retry", ie);
                }
            } catch (RestClientResponseException e) {
                int status = e.getRawStatusCode();
                boolean retryable = status == 429 || status >= 500;
                if (!retryable || attempt >= maxRetries) {
                    throw e;
                }
                attempt++;
                long baseDelay = (long) Math.pow(2, attempt) * 1000L; // 2s, 4s, 8s...
                long jitter = ThreadLocalRandom.current().nextLong(0, 1000);
                long delay = baseDelay + jitter;
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted during retry", ie);
                }
            }
        }
    }

    /**
     * Extrae el delay del header Retry-After de una respuesta 429/503.
     * Retry-After puede ser segundos (entero) o una fecha HTTP.
     * Devuelve -1 si no se puede determinar.
     */
    private long extractRetryAfterDelay(WebClientResponseException e) {
        try {
            String retryAfter = e.getHeaders().getFirst("Retry-After");
            if (retryAfter != null && !retryAfter.isEmpty()) {
                // Try parsing as seconds (integer)
                try {
                    long seconds = Long.parseLong(retryAfter.trim());
                    return seconds * 1000L;
                } catch (NumberFormatException nfe) {
                    // Try parsing as HTTP date
                    try {
                        java.time.format.DateTimeFormatter formatter =
                            java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
                        java.time.ZonedDateTime date = java.time.ZonedDateTime.parse(retryAfter.trim(), formatter);
                        long now = System.currentTimeMillis();
                        long delay = date.toInstant().toEpochMilli() - now;
                        return Math.max(0, delay);
                    } catch (Exception dateEx) {
                        // Ignore, return -1
                    }
                }
            }
        } catch (Exception ex) {
            // Ignore header parsing errors
        }
        return -1;
    }
}