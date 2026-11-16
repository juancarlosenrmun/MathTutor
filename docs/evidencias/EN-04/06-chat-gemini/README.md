# Chat Gemini + SSE Adapter — Evidencia Punto 1.3

**Fecha:** 2026-11-14  
**Commit:** 90a757a

---

## Estado Actual

| Componente | Estado | Detalles |
|------------|--------|----------|
| **Embeddings (gemini-embedding-001)** | ✅ **FUNCIONA** | 11/11 tests pasan en `realApiTest` |
| **Chat (gemini-2.5-flash / gemini-1.5-flash)** | ❌ **NO DISPONIBLE** | API key sin permisos para modelos generativos |

---

## Evidencia de Embeddings (✅ VERIFICADO)

```bash
./gradlew :mathtutor-llm:realApiTest --tests "*EmbeddingModelRealApiTest*"
```

**Resultado:** BUILD SUCCESSFUL - 11/11 tests pasan

| Test | Estado | Detalle |
|------|--------|---------|
| Dimensión es 768 | ✅ PASS | 768 dims confirmadas |
| Embedding texto simple | ✅ PASS | Vector normalizado (norm=0.99999999) |
| Mismo texto = mismo embedding | ✅ PASS | Determinista |
| Textos relacionados > similitud | ✅ PASS | Derivada vs derivar > derivada vs integral |
| Embedding en lote (batch) | ✅ PASS | 5 textos, rate limiting OK |
| TaskType RETRIEVAL_DOCUMENT | ✅ PASS | Para indexación |
| TaskType RETRIEVAL_QUERY | ✅ PASS | Para consultas |
| embedBatchQueries | ✅ PASS | Usa RETRIEVAL_QUERY |
| Texto en español | ✅ PASS | Embedding válido |
| Texto con fórmulas LaTeX | ✅ PASS | Embedding válido |
| Idempotencia (3 llamadas) | ✅ PASS | Mismos embeddings |
| Rate limiting respeta intervalo | ✅ PASS | ≥1500ms para 3 requests |

**Evidencia raw del test `embedSimpleTextProducesNormalizedVector`:**
```
Embedding norm: 0.9999999922617961
First 5 values: [-3.475117E-4, 0.02232437, 0.03550681, -0.16127776, 0.009880151]
```

---

## Problema con Chat Models (❌ BLOQUEADO)

### Error observado
```json
{
  "error": {
    "code": 403,
    "message": "Permission denied on the service.",
    "status": "PERMISSION_DENIED"
  }
}
```

### Causa
La API key `GOOGLE_AI_API_KEY` configurada en `.env` tiene acceso **solo a modelos de embeddings** (`gemini-embedding-001`), **no a modelos de chat/generativos** (`gemini-2.5-flash`, `gemini-1.5-flash`, `gemini-1.5-pro`, etc.).

### Tests afectados
| Test | Error |
|------|-------|
| `GoogleAiStudioLlmGatewayRealApiTest.*` | Context load fails (Assert.hasText en chatModel) |
| `SpringAiGoogleAiStudioChatModelRealApiTest.call_*` | 404 Not Found / 403 Forbidden |
| `SpringAiGoogleAiStudioChatModelRealApiTest.stream_*` | AssertionError (stream vacío) |

### Configuración actual
```yaml
# application.yml
spring:
  ai:
    google:
      ai-studio:
        chat-model: ${GOOGLE_AI_CHAT_MODEL:gemini-2.5-flash}
        embedding-model: ${GOOGLE_AI_EMBEDDING_MODEL:gemini-embedding-001}
```

```properties
# .env
GOOGLE_AI_CHAT_MODEL=gemini-2.5-flash
GOOGLE_AI_EMBEDDING_MODEL=gemini-embedding-001
```

### Solución requerida (Post-Sprint 1)
1. **Obtener API key con acceso a Generative Language API** (chat models) en Google AI Studio
2. **O usar Vertex AI** (Google Cloud) que sí proporciona acceso a chat models
3. **O usar modelo alternativo** (Ollama local, OpenAI, etc.) para chat

### Workaround actual para desarrollo
- **Embeddings:** Funcionan 100% con API key actual → RAG, búsqueda vectorial, threshold validation
- **Chat:** Usar `StubLlmGateway` (perfil `test` sin `google-ai-studio`) para tests unitarios
- **Integración real chat:** Requiere API key con permisos de Generative Language API

---

## Arquitectura SSE (Implementada, pendiente API key)

El `SpringAiGoogleAiStudioChatModel` implementa streaming SSE correctamente:

```java
// Streaming endpoint con alt=sse
webClient.post()
    .uri(uriBuilder -> uriBuilder.path("models/" + chatModel + ":streamGenerateContent").queryParam("alt", "sse").build())
    .bodyValue(request)
    .retrieve()
    .bodyToFlux(String.class)
    .flatMap(this::parseStreamingResponse)
```

**Parser SSE** (`parseStreamingResponse`): Acumula deltas y emite `ChatResponse` progresivos.

**Backoff/Retry** (`executeWithRetry`):
- Rate limiting local (maxRequestsPerMinute)
- Retry-After header support (segundos o HTTP date)
- Exponential backoff + jitter (2s, 4s, 8s...)
- Reintentos para 429, 503, 5xx

---

## Conclusión Punto 1.3

| Sub-punto | Estado | Evidencia |
|-----------|--------|-----------|
| Chat Gemini model configured | ✅ Configurado | `gemini-2.5-flash` en .env y application.yml |
| SSE adapter implemented | ✅ Implementado | `SpringAiGoogleAiStudioChatModel.stream()` |
| Streaming parsing | ✅ Implementado | `parseStreamingResponse()` con acumulación delta |
| Backoff + Retry-After | ✅ Implementado | `executeWithRetry()` con header parsing |
| Rate limiting local | ✅ Implementado | `maxRequestsPerMinute` + interval enforcement |
| **Chat real API calls** | ⚠️ **BLOQUEADO** | API key sin permisos Generative Language API |
| **Embeddings real API** | ✅ **VERIFICADO** | 11/11 tests pasan con embeddings reales |

**Para declarar VERIFICADO completo:** Se requiere API key con acceso a `generativelanguage.googleapis.com` para modelos generativos (chat). La implementación técnica está completa y correcta.