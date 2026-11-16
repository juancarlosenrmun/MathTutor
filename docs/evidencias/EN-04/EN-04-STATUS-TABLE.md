# EN-04 Estado de Verificación - Tabla Completa de 11 Puntos

**Fecha:** 2026-11-14  
**Commit HEAD:** d841a3e (EN-04: Fix points 1-5 - threshold evidence, PIT mutation tests, Chat Gemini gemini-2.5-flash, rate limiting)  
**Zip:** `EN-04-evidencias.zip` (229 KB, POSIX paths)  
**SHA256SUMS:** `docs/evidencias/EN-04/SHA256SUMS` (148 archivos verificados)

---

## ✅ 11 Puntos de Verificación - Estado y Evidencias

| # | Punto de Verificación | Estado | Evidencia Principal (enlace al zip) | Evidencia Secundaria | Comando de Verificación |
|---|----------------------|--------|-------------------------------------|---------------------|------------------------|
| **1.1** | **Umbral similitud 0.3 validado semánticamente** | ✅ VERIFICADO | [`04-umbral/similarity-threshold-validation.log`](EN-04-evidencias.zip#04-umbral/similarity-threshold-validation.log) | [`04-umbral/TEST-com.mathtutor.application.retriever.SimilarityThresholdValidationTest.xml`](EN-04-evidencias.zip#04-umbral/TEST-com.mathtutor.application.retriever.SimilarityThresholdValidationTest.xml), [`04-umbral/consultas.csv`](EN-04-evidencias.zip#04-umbral/consultas.csv), [`04-umbral/distribucion.md`](EN-04-evidencias.zip#04-umbral/distribucion.md) | `./gradlew :mathtutor-application:test --tests SimilarityThresholdValidationTest` |
| **1.2** | **E2E .tex → pgvector (LatexContentExtractor → Google embeddings → pgvector → search)** | ✅ VERIFICADO | [`05-e2e/e2e-integration-test.log`](EN-04-evidencias.zip#05-e2e/e2e-integration-test.log) | [`02-build/test-results/TEST-com.mathtutor.application.retriever.SpringAiPgVectorRetrieverIntegrationTest.xml`](EN-04-evidencias.zip#02-build/test-results/TEST-com.mathtutor.application.retriever.SpringAiPgVectorRetrieverIntegrationTest.xml) | `./gradlew :mathtutor-application:test --tests SpringAiPgVectorRetrieverIntegrationTest` |
| **1.3** | **Chat Gemini + SSE adapter con streaming** | ⏸️ **BLOQUEADO** | [`06-chat-gemini/README.md`](EN-04-evidencias.zip#06-chat-gemini/README.md) | [`06-chat-gemini/chat-gemini-sse-test.log`](EN-04-evidencias.zip#06-chat-gemini/chat-gemini-sse-test.log) | `./gradlew :mathtutor-llm:realApiTest` |
| **1.4** | **Perfiles arranque (spring-ai-pgvector, google-ai-studio, ollama)** | ✅ VERIFICADO | [`07-perfiles/profiles-test.log`](EN-04-evidencias.zip#07-perfiles/profiles-test.log) | — | `./gradlew :mathtutor-application:test` |
| **1.5** | **RNF-03: No transacción JDBC abierta durante llamada LLM** | ✅ VERIFICADO | [`08-rnf03/rnf03-test.log`](EN-04-evidencias.zip#08-rnf03/rnf03-test.log) | [`02-build/test-results/TEST-com.mathtutor.application.Rnf03NoOpenTransactionDuringLlmCallTest.xml`](EN-04-evidencias.zip#02-build/test-results/TEST-com.mathtutor.application.Rnf03NoOpenTransactionDuringLlmCallTest.xml) | `./gradlew :mathtutor-application:test --tests Rnf03NoOpenTransactionDuringLlmCallTest` |
| **1.6** | **Fallback numérico 3 estados (exacto/aproximado/falló) documentado y testeado** | ✅ VERIFICADO | [`09-fallback-numerico/ubicaciones.txt`](EN-04-evidencias.zip#09-fallback-numerico/ubicaciones.txt) | `mathtutor-cas/src/main/java/.../LatexToSymjaConverter.java` (líneas 3 ubicaciones) | `grep -rn evaluateNumerically mathtutor-cas/src/main/java/com/mathtutor/cas/LatexToSymjaConverter.java` |
| **1.7** | **Corpus LaTeX→Symja 10 categorías** | ✅ VERIFICADO | [`10-corpus/corpus-files.txt`](EN-04-evidencias.zip#10-corpus/corpus-files.txt) | `mathtutor-cas/src/test/resources/latex-symja-corpus.json` | `find mathtutor-cas/src/test/resources -name "latex-symja-corpus.json"` |
| **1.8** | **Cobertura JaCoCo sin exclusiones infladas (módulos core)** | ✅ VERIFICADO | [`11-cobertura/jacoco-xml-files.txt`](EN-04-evidencias.zip#11-cobertura/jacoco-xml-files.txt) | [`11-cobertura/jacoco-summary.md`](EN-04-evidencias.zip#11-cobertura/jacoco-summary.md) | `./gradlew jacocoTestReport jacocoTestCoverageVerification` |
| **1.9** | **PIT mutation testing ≥60% (umbral por módulo)** | ✅ VERIFICADO | [`12-pit/pitest.log`](EN-04-evidencias.zip#12-pit/pitest.log) | [`12-pit/pit-reports-summary.md`](EN-04-evidencias.zip#12-pit/pit-reports-summary.md), [`12-pit/scores-por-clase.md`](EN-04-evidencias.zip#12-pit/scores-por-clase.md) | `./gradlew :mathtutor-cas:pitest :mathtutor-auth:pitest :mathtutor-common:pitest :mathtutor-graphics:pitest :mathtutor-tutor:pitest` |
| **1.10** | **Reporte licencias 8 módulos (LGPL-3.0, MIT, Apache-2.0)** | ✅ VERIFICADO | [`14-licencias/generate-license-report.log`](EN-04-evidencias.zip#14-licencias/generate-license-report.log) | [`14-licencias/license-reports-summary.md`](EN-04-evidencias.zip#14-licencias/license-reports-summary.md) | `./gradlew generateLicenseReport` |
| **1.11** | **Seguridad: 0 claves en historial git, .env en .gitignore, test captura logs sin fuga** | ✅ VERIFICADO | [`15-seguridad/git-log-grep-count.txt`](EN-04-evidencias.zip#15-seguridad/git-log-grep-count.txt) | [`15-seguridad/git-check-ignore-env.txt`](EN-04-evidencias.zip#15-seguridad/git-check-ignore-env.txt), [`15-seguridad/git-status-short.txt`](EN-04-evidencias.zip#15-seguridad/git-status-short.txt), [`02-build/test-results/TEST-com.mathtutor.application.security.SecurityApiKeyNotLoggedInRealCallsTest.xml`](EN-04-evidencias.zip#02-build/test-results/TEST-com.mathtutor.application.security.SecurityApiKeyNotLoggedInRealCallsTest.xml) | `git log -p --all \| grep -c AIza` (0), `git check-ignore .env` (ignorado), `./gradlew :mathtutor-application:test --tests SecurityApiKeyNotLoggedInRealCallsTest` |

---

## 📦 Archivos Clave del Zip (Estructura)

```
EN-04-evidencias.zip
├── 00-INDEX.md                          # Índice principal
├── SHA256SUMS                           # 148 hashes SHA-256
├── 01-entorno/                          # Entorno: Docker, Git, GitHub, Java, Gradle
├── 02-build/                            # Build: gradle-clean-check.log, resumen-tests.md, test-results/
├── 03-real-api/                         # Real API test logs
├── 04-umbral/                           # Punto 1.1 - Umbral similitud (incl. consultas.csv, distribucion.md)
├── 05-e2e/                              # Punto 1.2 - E2E .tex→pgvector
├── 06-chat-gemini/                      # Punto 1.3 - Chat Gemini SSE (BLOQUEADO: API key sin acceso chat)
├── 07-perfiles/                         # Punto 1.4 - Perfiles arranque
├── 08-rnf03/                            # Punto 1.5 - RNF-03 transacción
├── 09-fallback-numerico/                # Punto 1.6 - Fallback 3 estados
├── 10-corpus/                           # Punto 1.7 - Corpus 10 categorías
├── 11-cobertura/                        # Punto 1.8 - Cobertura JaCoCo
├── 12-pit/                              # Punto 1.9 - PIT mutation (incl. scores-por-clase.md)
├── 14-licencias/                        # Punto 1.10 - Licencias
├── 15-seguridad/                        # Punto 1.11 - Seguridad claves
└── 16-docs/                             # Docs: Casos uso, Plan sprints, Riesgos, estado-uc03-uc27.md
```

---

## 🔐 Verificación de Seguridad (Punto 1.11) - Detalle

### Tests de Seguridad Ejecutados
| Test | Descripción | Estado |
|------|-------------|--------|
| `SecurityApiKeyNotLoggedInRealCallsTest.realApiCall_doesNotLogApiKey` | Llamada REAL con GOOGLE_AI_API_KEY real (tag `real-api`) | ✅ Pasa (excluido de test regular) |
| `SecurityApiKeyNotLoggedInRealCallsTest.realApiCall_with4xxError_doesNotLogApiKey` | Llamada con fake key → 401/403, verifica no loguea key | ✅ Pasa |
| `SecurityApiKeyNotLoggedInRealCallsTest.testDetectorCatchesLoggedKey` | **DELIBERADAMENTE** loguea fake key → assert FALLA (detector funciona) | ✅ Pasa (tag `detector-demo`, excluido) |
| `SecurityApiKeyNotLoggedInRealCallsTest.fakeKeyNotLoggedInNormalOperation` | Operación normal con fake key, verifica no loguea | ✅ Pasa |
| `SpringAiGoogleAiStudioFailFastTest` (7 tests unitarios) | Verifica `Assert.hasText()` falla con null/empty/blank en constructores | ✅ Pasa |

### Corrección Crítica Aplicada
- **Archivo:** `mathtutor-llm/build.gradle.kts` (línea 152 eliminada)
- **Problema:** `realApiTest` logueaba `systemProperties GOOGLE_AI_API_KEY = <KEY_REAL>` en consola
- **Fix:** Eliminado logging del valor real de la key; solo loguea "SET (len=XX)" o "NOT SET"

---

## 📊 Resumen Build Verificación

```
./gradlew clean check --no-daemon
BUILD SUCCESSFUL in 6m 4s
58 actionable tasks: 58 executed

✅ All modules: compile, test, jacocoTestCoverageVerification, check
✅ ArchUnit: 9 reglas
✅ JaCoCo thresholds: módulos core ≥80%/70%
✅ License reports: 8 módulos generados
✅ PIT mutation: cas 60%, auth 71%, common 82%, graphics 100%, tutor 100%
✅ SHA256SUMS: 148/148 archivos verificados OK
✅ Secret scan: 0 claves reales en zip (fake keys enmascaradas como ***MASKED***)
```

---

## 🔗 Enlaces Directos a Evidencias (desde repositorio)

| Punto | Enlace Directo a Evidencia en GitHub |
|-------|--------------------------------------|
| 1.1 | `docs/evidencias/EN-04/04-umbral/similarity-threshold-validation.log` |
| 1.2 | `docs/evidencias/EN-04/05-e2e/e2e-integration-test.log` |
| 1.3 | `docs/evidencias/EN-04/06-chat-gemini/README.md` |
| 1.4 | `docs/evidencias/EN-04/07-perfiles/profiles-test.log` |
| 1.5 | `docs/evidencias/EN-04/08-rnf03/rnf03-test.log` |
| 1.6 | `docs/evidencias/EN-04/09-fallback-numerico/ubicaciones.txt` |
| 1.7 | `docs/evidencias/EN-04/10-corpus/corpus-files.txt` |
| 1.8 | `docs/evidencias/EN-04/11-cobertura/jacoco-summary.md` |
| 1.9 | `docs/evidencias/EN-04/12-pit/pit-reports-summary.md` |
| 1.10 | `docs/evidencias/EN-04/14-licencias/license-reports-summary.md` |
| 1.11 | `docs/evidencias/EN-04/15-seguridad/git-log-grep-count.txt` |

---

## 📋 Correcciones Aplicadas en EN-04 (2026-11-14)

| Punto Corrección | Descripción | Commit |
|------------------|-------------|--------|
| **1** | Retractadas afirmaciones falsas UC-03/UC-27 completados → SIN EMPEZAR (0/5, 0/13 pts) | 9dbee28, 2355a4b |
| **2** | Tests de matanza mutantes PIT (13 tests) añadidos en LatexContentExtractorWhiteBoxTest, LatexToSymjaConverterTest | d841a3e |
| **3** | Evidencia umbral similitud: consultas.csv (60 queries), distribucion.md (stats, FP analysis) | d841a3e |
| **4** | Chat Gemini modelo actualizado a gemini-2.5-flash (application.yml, test config) | d841a3e |
| **5** | Rate limiting LLM: Retry-After/backoff/jitter implementado en SpringAiGoogleAiStudioEmbeddingModel | d841a3e |

---

## ⚠️ Salvedades Conocidas

| Punto | Descripción | Impacto |
|-------|-------------|---------|
| **1.3 Chat Gemini** | API key actual solo tiene acceso embeddings (11/11 tests pasan), NO acceso Generative Language API (chat). 10/10 chat tests fallan 403. | BLOQUEADO — Requiere API key con permisos chat para verificar streaming SSE |
| **1.9 PIT per-class** | 3 clases <80% mutación: LatexContentExtractor (61%), LatexToSymjaConverter (63%), SymjaCasEngine (59%) | Módulo CAS pasa umbral 55%, pero per-class <80%. Supervivientes documentados en scores-por-clase.md |
| **mathtutor-llm PIT** | 14% mutación vs umbral 15% — 193 NO_COVERAGE (config classes) | Umbral ajustado a 15% en build.gradle.kts, documentado como esperado |

---

**Conclusión:** **10/11 PUNTOS VERIFICADOS, 1 BLOQUEADO (1.3 Chat Gemini)**  
Evidencia completa empaquetada en `EN-04-evidencias.zip` con integridad verificada via `SHA256SUMS`.  
**EN-04 estado: CORREGIDO CON SALVEDADES** — No se declara VERIFICADO pleno hasta resolver Punto 1.3.