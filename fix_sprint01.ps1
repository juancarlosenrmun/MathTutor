$content = Get-Content "C:\Users\Juank\Documents\Ingenieria_Informatica\TFG\MathTutor\brain\04 Sprints\Sprint 01.md" -Raw

# Fix UC-03 checklist
$content = $content -replace '### UC-03 . Controlar el acceso por rol y recurso\s+-\s+\[ \] Implementación . . \*\*Movido a Sprint 02\*\*\s+-\s+\[ \] Pruebas de caja blanca . . \*\*Movido a Sprint 02\*\*\s+-\s+\[ \] Pruebas de caja negra . . \*\*Movido a Sprint 02\*\*\s+-\s+\[ \] Cobertura en umbral \(. 80 % líneas, . 70 % ramas\) . . \*\*Movido a Sprint 02\*\*\s+-\s+\[ \] Integrado en la rama principal . . \*\*Movido a Sprint 02\*\*\s+\*\*Estado UC-03: MOVIDO A S2 \(no completado en S1\)\*\*', '### UC-03 · Controlar el acceso por rol y recurso
- [ ] Implementación
- [ ] Pruebas de caja blanca
- [ ] Pruebas de caja negra
- [ ] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [ ] Integrado en la rama principal

**Estado UC-03: 🔴 Pendiente en S1 (reversión de replanificación 2026-11-12)**'

# Fix UC-27 checklist
$content = $content -replace '### UC-27 . Abrir el visor WebGL con pipeline y cámara\s+-\s+\[ \] Implementación . . \*\*Movido a Sprint 02\*\*\s+-\s+\[ \] Pruebas de caja blanca . . \*\*Movido a Sprint 02\*\*\s+-\s+\[ \] Pruebas de caja negra . . \*\*Movido a Sprint 02\*\*\s+-\s+\[ \] Cobertura en umbral \(. 80 % líneas, . 70 % ramas\) . . \*\*Movido a Sprint 02\*\*\s+-\s+\[ \] Integrado en la rama principal . . \*\*Movido a Sprint 02\*\*\s+\*\*Estado UC-27: MOVIDO A S2 \(no completado en S1\)\*\*', '### UC-27 · Abrir el visor WebGL con pipeline y cámara
- [ ] Implementación
- [ ] Pruebas de caja blanca
- [ ] Pruebas de caja negra
- [ ] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [ ] Integrado en la rama principal

**Estado UC-27: 🔴 Pendiente en S1 (reversión de replanificación 2026-11-12)**'

# Fix Definición de hecho
$content = $content -replace '## Definición de hecho del sprint\s+-\s+\[x\] EN-01 completo\s+-\s+\[x\] \*\*EN-04 COMPLETO Y VERIFICADO \(RIGUROSO\)\*\* . LaTeX extraction ., Symja ., pgvector JDBC ., Spring AI pgvector-store ., \*\*Google AI Studio EmbeddingModel .\*\*, \*\*License report .\*\*, \*\*PIT mutation testing .\*\*, \*\*Clean build .\*\*\s+-\s+\[x\] UC-01 \*\*COMPLETO \(D1.D10 cerrados\)\*\*\s+-\s+\[ \] UC-03 . . \*\*Movido a S2\*\*\s+-\s+\[ \] UC-27 . . \*\*Movido a S2\*\*\s+-\s+\[x\] Build y CI en verde \(local: BUILD SUCCESSFUL; CI: VERDE en sprint-1 . https://github.com/juancarlosenrmun/MathTutor/actions/runs/42535665406\)\s+-\s+\[x\] Cobertura en umbral verificada \(JaCoCo verification pasa con exclusiones documentadas; \*\*llm core 100% líneas / 100% ramas sin exclusiones\*\*; \*\*cas 89% líneas / 82% ramas SIN excluir SymjaCasEngine\*\*; \*\*mathtutor-llm total 51.7% líneas / 51.8% ramas EXCLUIDO de verificación\*\*; \*\*mathtutor-persistence 46.2% líneas EXCLUIDO\*\*; \*\*mathtutor-application 0% EXCLUIDO\*\*\)\s+-\s+\[x\] Velocidad real medida y anotada en \[\[Registro de iteraciones\]\]\s+-\s+\[x\] \[\[Progreso global\]\] actualizado', '## Definición de hecho del sprint
- [x] EN-01 completo
- [ ] **EN-04 EN VERIFICACIÓN** — LaTeX extraction ✅, Symja ✅, pgvector JDBC ✅, Spring AI pgvector-store ✅, Google AI Studio EmbeddingModel ✅, License report ✅, PIT mutation testing ✅, Clean build ✅. Falta: umbral con datos (E), E2E real (D), Chat Gemini (F), perfiles seguros (H), RNF-03 demo fallo (G), fallback 3 estados (I), corpus 10 cat + negativos (J), cobertura retriever/adapters (L), PIT cas ≥80% (K), licencias consolidadas (M), test seguridad clave (A)
- [x] UC-01 **COMPLETO (D1–D10 cerrados)**
- [ ] UC-03 — **Pendiente en S1**
- [ ] UC-27 — **Pendiente en S1**
- [x] Build y CI en verde (local: BUILD SUCCESSFUL; CI: VERDE en sprint-1 — https://github.com/juancarlosenrmun/MathTutor/actions/runs/42535665406)
- [x] Cobertura en umbral verificada (JaCoCo verification pasa con exclusiones documentadas; **llm core 100% líneas / 100% ramas sin exclusiones**; **cas 89% líneas / 82% ramas SIN excluir SymjaCasEngine**; **mathtutor-llm total 51.7% líneas / 51.8% ramas EXCLUIDO de verificación**; **mathtutor-persistence 46.2% líneas EXCLUIDO**; **mathtutor-application 0% EXCLUIDO**)
- [x] Velocidad real medida y anotada en [[Registro de iteraciones]]
- [x] [[Progreso global]] actualizado'

# Fix Auditoría EN-04 - items 1, 2, 5, 7, 10
$content = $content -replace '\| 1 \| Proveedor embedding real \(Google AI Studio / OpenAI / Ollama\) \| . \*\*COMPLETO\*\* \| `SpringAiGoogleAiStudioEmbeddingModel` implementado \(gemini-embedding-001, 768 dim, multilingual, REST client, L2 norm, taskType=RETRIEVAL_DOCUMENT/RETRIEVAL_QUERY, rate limiting, retry exponencial\). Perfil `google-ai-studio` con @Primary. \*\*Integración real VERIFICADA\*\* - 12 tests `realApiTest` pasan con API key real. Spring AI config tiene placeholders OpenAI/Ollama en `application.yml`. \|', '| 1 | Proveedor embedding real (Google AI Studio / OpenAI / Ollama) | ✅ **COMPLETO** | `SpringAiGoogleAiStudioEmbeddingModel` implementado (gemini-embedding-001, 768 dim, multilingual, REST client, L2 norm, taskType=RETRIEVAL_DOCUMENT/RETRIEVAL_QUERY, rate limiting, retry exponencial). Perfil `google-ai-studio` con @Primary. **Integración real VERIFICADA** - 12 tests `realApiTest` pasan con API key real. Spring AI config tiene placeholders OpenAI/Ollama en `application.yml`. |'

$content = $content -replace '\| 2 \| Validación umbral similitud 0.3 \| . \*\*COMPLETO \(T3\)\*\* \| Validado con `SimilarityThresholdValidationTest` \(15 queries in-domain \+ 15 out-of-domain, embeddings reales Google AI Studio \+ pgvector Testcontainers\). In-domain: . 10/15 queries recuperan resultados relevantes. Out-of-domain: . 3/15 falsos positivos \(20% tolerancia\). Umbral cosine distance < 0.3 validado empíricamente. \|', '| 2 | Validación umbral similitud 0.3 | 🔄 **EN PROGRESO (E)** | Validado con `SimilarityThresholdValidationTest` (15 in-domain + 15 out-of-domain, embeddings reales). Falta: ≥30 consultas cada clase, umbral en propiedad `tutor.rag.similarity-threshold` validada al arrancar, distribución completa min/mediana/máx, rechazo fuera temario ≥90%, aceptación en temario con mínimo explícito (sin tolerancia 20%). |'

$content = $content -replace '\| 5 \| Test transacción RNF-03 \| . \*\*NO TESTEADO\*\* \| `@Transactional` en `retrieve()` \(readOnly\) e `indexDocuments()`. Docs afirman "no JDBC abierto durante llamada LLM" pero sin test que verifique estado conexión con llamada lenta mock. Pendiente. \|', '| 5 | Test transacción RNF-03 | 🔄 **EN PROGRESO (G)** | `@Transactional` en `retrieve()` (readOnly) e `indexDocuments()`. Falta: test con `@Transactional` temporal . fallo . revertir + git diff limpio. Tests actuales usan HikariPoolMXBean + pg_stat_activity con SlowDeterministicEmbeddingModel (2s). |'

$content = $content -replace '\| 7 \| Verificación cobertura JaCoCo \| . \*\*EXCLUSIONES INFLAN PORCENTAJES\*\* \| mathtutor-llm \*\*51.7% líneas\*\* \(excluido verificación\), mathtutor-persistence \*\*46.2%\*\* \(excluido\), mathtutor-application \*\*0%\*\* \(excluido\). Solo "core" \(DeterministicEmbeddingModel, PgVectorRetriever\) 100%/100%. `SpringAiPgVectorRetriever` \*\*0% cobertura\*\*. Documentado en `build.gradle.kts`. \|', '| 7 | Verificación cobertura JaCoCo | 🔄 **EN PROGRESO (L)** | mathtutor-llm **51.7% líneas** (excluido verificación), mathtutor-persistence **46.2%** (excluido), mathtutor-application **0%** (excluido). Solo "core" 100%. `SpringAiPgVectorRetriever` **0% cobertura**. Falta: corregir agregación JaCoCo (incluir tests integración + realApiTest), justificar exclusiones, cubrir retriever/adapters con integración real. |'

$content = $content -replace '\| 10 \| Mutation testing \(T6\) \| . \*\*COMPLETADO\*\* \| `info.solidsoft.pitest:1.19.0` \+ JUnit 5 plugin \(org.pitest:pitest-junit5-plugin:1.2.3\) en todos los módulos. Umbral 60%. Resultados: mathtutor-common 82%, mathtutor-cas 60%, mathtutor-auth 71%, mathtutor-tutor 100%, mathtutor-graphics 100%, mathtutor-persistence 45%, mathtutor-llm 32%, mathtutor-application 31% \(timeouts en tests integración\). \|', '| 10 | Mutation testing (T6) | 🔄 **EN PROGRESO (K)** | PIT ejecutado: common 82%, cas 60%, auth 71%, tutor 100%, graphics 100%. Falta: cas ≥80% (matar mutantes o justificar equivalentes en extractor, conversor, equivalencia, umbral retriever), auth ≥80%, 5 mutaciones manuales (invertir umbral, ignorar filtro módulo, no escapar `\$`, aceptar diff constante como equivalente, perder entorno extractor). |'

Set-Content "C:\Users\Juank\Documents\Ingenieria_Informatica\TFG\MathTutor\brain\04 Sprints\Sprint 01.md" -Value $content -Encoding UTF8
"Done"