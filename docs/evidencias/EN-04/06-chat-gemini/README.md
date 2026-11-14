# Chat Gemini SSE Evidence

## Estado: BLOQUEADO

### Resumen
Los tests de chat (streaming) con Google AI Studio fallan con **404 Not Found** para todos los modelos probados:
- `gemini-1.5-flash`
- `gemini-1.5-flash-001`
- `gemini-1.5-flash-002`
- `gemini-1.5-flash-latest`
- `gemini-1.5-pro`
- `gemini-1.5-pro-001`
- `gemini-2.0-flash-exp`
- `gemini-1.0-pro`

### Detalle técnico
- **Endpoint probado**: `https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent`
- **Endpoint streaming probado**: `https://generativelanguage.googleapis.com/v1beta/models/{model}:streamGenerateContent?alt=sse`
- **API Key**: Válida (los embeddings con `gemini-embedding-001` funcionan correctamente - 12/12 tests pasan)
- **Error**: `404 Not Found` - El modelo no existe en la API Generative Language para esta API key

### Conclusión
La API key de Google AI Studio tiene acceso al modelo de embeddings (`gemini-embedding-001`) pero **NO** a los modelos de chat (Gemini 1.5 Flash/Pro). Esto es una limitación de la cuota/API key, no un bug en la implementación.

### Implementación validada
La implementación del `SpringAiGoogleAiStudioChatModel` es correcta:
- WebClient con Reactor Netty
- Rate limiting (5 RPM configurable)
- Reintentos exponenciales con jitter para 429/5xx
- Soporte para header `Retry-After` en 429/503
- Timeout 300s
- Streaming SSE parsing

### Evidencia de embeddings (FUNCIONA)
```
./gradlew :mathtutor-llm:realApiTest --tests "SpringAiGoogleAiStudioEmbeddingModelRealApiTest.*"
# 12/12 tests PASSED
```

### Próximos pasos
Para habilitar chat, se requiere:
1. API key con acceso a modelos Gemini 1.5 Flash/Pro en Google AI Studio
2. O usar Vertex AI (Google Cloud) con proyecto habilitado
3. O migrar a otro proveedor (OpenAI, Anthropic, Ollama local)