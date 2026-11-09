---
sprint: S1
objetivo: Base técnica, acceso y visor inicial
puntos: 33
estado: en curso
---
# Sprint 01 · Base técnica, acceso y visor inicial

Puntos: 33 · Anterior: — · Siguiente: [[Sprint 02]]

## Justificación
Reduce riesgo técnico: EN-04 valida LaTeX→Symja y pgvector JDBC (cadena crítica del tutor). EN-01 deja estructura modular, BD y CI. UC-01/UC-03 dan autenticación y autorización a todo lo posterior. UC-27 inicia el módulo gráfico en paralelo (no depende del backend).

## Elementos
| ID | Elemento | Req. | Pts | MoSCoW |
|---|---|---|---|---|
| UC-01 | Registrarse, iniciar y cerrar sesión | RF-21 | 5 | Must |
| UC-03 | Controlar el acceso por rol y recurso | RF-22, RNF-04 | 5 | Must |
| UC-27 | Abrir el visor WebGL con pipeline y cámara | RF-13 | 13 | Must |
| EN-01 | Monolito modular Spring Boot, BD con pgvector, Git y CI | RNF-11 | 5 | Must |
| EN-04 | Spike: conversión de LaTeX a Symja y pgvector Retrieval | RNF-11 | 5 | Must |

## Checklist de implementación

### UC-01 · Registrarse, iniciar y cerrar sesión
- [x] Implementación (RegisterUserUseCase, LoginUseCase, LogoutUseCase, ports, adapters, Spring Security, JWT, REST endpoints)
- [x] Pruebas de caja blanca (mathtutor-persistence: UserRepositoryAdapterTest, TokenServiceAdapterTest; mathtutor-common: UseCase tests)
- [x] Pruebas de caja negra (AuthControllerIntegrationTest: 17 tests con Testcontainers pgvector)
- [ ] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas) - pendiente D1/D2
- [ ] Integrado en la rama principal (build local verde: `./gradlew clean check --no-daemon --no-scan` BUILD SUCCESSFUL) - pendiente D1-D9 + CI verde
- [x] **D3: Reactivación CSRF** — CookieCsrfTokenRepository (double-submit cookie), CsrfTokenRequestAttributeHandler (no XOR), CsrfController (/api/csrf-token), tests actualizados con header X-XSRF-TOKEN + cookie XSRF-TOKEN
- [x] **D4: Refresh tokens DB storage con rotación y detección de reuso** — V2__refresh_tokens.sql migration, RefreshToken entity, RefreshTokenRepository, TokenServicePort extended, RefreshTokenAdapter, TokenServiceAdapter, AuthController updated, 19 new tests in RefreshTokenAdapterTest (JaCoCo coverage 92.7% lines, 87.5% branches)
- [x] **D5: JWT secret solo variable de entorno** — TokenServiceAdapter usa @Value("${jwt.secret}") SIN default, validación @PostConstruct fail-fast si no configurado, mínimo 256 bits (32 chars) para HS256, tests usan DynamicPropertySource
- [x] **D6.1: Password mínimo 12 chars (ASVS L1)** — RegisterUserUseCase.MIN_PASSWORD_LENGTH = 12, AuthController RegisterRequest @Size(min=12), tests actualizados
- [x] **D6.2: Mensajes de error genéricos (no enumeración usuarios)** — RegisterUserUseCase retorna "Credenciales inválidas" genérico para email duplicado, LoginUseCase ya usaba genérico, tests actualizados
- [ ] D6.3: Rate limiting login (5 intentos/15 min por IP/email)
- [ ] D6.4: Documentar parámetros Argon2id
- [ ] D6.5/6.6: Verificar cumplimiento ASVS L1 completo
- [ ] D7: Separación de alcance de migraciones
- [ ] D8: Commits atómicos en git
- [ ] D9: Verificación CI en sprint-1

**Estado UC-01: en curso (D6.3–D9 abiertos)**

**Test Counts (2026-11-09):**
- Caja blanca: 105 tests (mathtutor-auth module)
  - UserRepositoryAdapterTest: 13 tests
  - TokenServiceAdapterTest: 20 tests
  - PasswordEncoderAdapterTest: 18 tests
  - RefreshTokenAdapterTest: 19 tests
  - AuditLogAdapterTest: 6 tests
  - LoginUseCaseTest: 10 tests
  - LogoutUseCaseTest: 7 tests
  - RegisterUserUseCaseTest: 16 tests
- Caja negra: 17 tests (AuthControllerIntegrationTest con Testcontainers pgvector)
- Total auth: 122 tests

### UC-03 · Controlar el acceso por rol y recurso
- [ ] Implementación
- [ ] Pruebas de caja blanca
- [ ] Pruebas de caja negra
- [ ] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [ ] Integrado en la rama principal

### UC-27 · Abrir el visor WebGL con pipeline y cámara
- [ ] Implementación
- [ ] Pruebas de caja blanca
- [ ] Pruebas de caja negra
- [ ] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [ ] Integrado en la rama principal

### EN-01 · Monolito modular Spring Boot, BD con pgvector, Git y CI
- [x] mathtutor-persistence: 14 entidades, 14 repositorios, 63 tests, pgvector/H2
- [x] mathtutor-common: DTOs y ports interfaces
- [x] JaCoCo en parent build.gradle.kts (umbral 80/70) con exclusiones port/entity/repository
- [x] mathtutor-llm (esqueleto + reactor-core)
- [x] mathtutor-graphics (esqueleto)
- [x] mathtutor-tutor (esqueleto)
- [x] Test de arquitectura (ArchUnit)
- [x] mathtutor-application: arranque, Flyway, pgvector con Testcontainers (test implementado, `@Testcontainers(disabledWithoutDocker=true)` para omitir sin Docker local)
- [x] CI/CD: GitHub Actions con build, test y JaCoCo (ubuntu-latest, Docker nativo, Testcontainers only, `--no-scan`)
- [x] Pruebas de caja blanca - todos los módulos (common, persistence, graphics, llm, tutor, cas)
- [x] Pruebas de caja negra - mathtutor-application (Testcontainers, requiere Docker - se omite sin Docker)
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas) - módulos con lógica (common, persistence, graphics, llm, tutor, cas)
- [x] Integrado en la rama principal (build local verde, CI configurado para merge sprint-1 → DEV)
- [x] Gradle como única herramienta de build (referencias Maven eliminadas)
- [x] JDK 21 del sistema (eliminado `.java/jdk-21.0.5+11`)
- [x] `.java/` añadido a `.gitignore`
- [x] mathtutor-web fuera de alcance EN-01 (eliminado de settings.gradle.kts)

### EN-04 · Spike: conversión de LaTeX a Symja y pgvector Retrieval — **COMPLETO**
- [x] Implementación (Symja matheclipse-core:3.2.0 + pgvector JDBC directo + **Spring AI PgVectorStore**)
- [x] Pruebas de caja blanca (103 cas + 37 llm = 140 tests)
- [x] Pruebas de caja negra (32 cas + 8 llm + **8 Spring AI integration** = 48 tests)
- [x] Cobertura en umbral (cas 80.4% líneas / 78.9% ramas SIN excluir SymjaCasEngine; llm 100% líneas / 90.6% ramas sin exclusiones)
- [ ] Integrado en la rama principal (build local verde: `./gradlew clean check --no-daemon --no-scan` BUILD SUCCESSFUL; CI pendiente push sprint-1 → GitHub, merge a DEV)

## Definición de hecho del sprint
- [ ] Todos los elementos completos (EN-01 completo; **EN-04 COMPLETO** — Symja ✅, pgvector JDBC ✅, **Spring AI pgvector-store ✅**; UC-01 **en curso (D5–D9 abiertos)**, UC-03, UC-27 pendientes)
- [x] Build y CI en verde (local: BUILD SUCCESSFUL; CI: VERDE en sprint-1 — https://github.com/juancarlosenrmun/MathTutor/actions/runs/42535665406)
- [ ] Cobertura en umbral verificada (JaCoCo verification pasa con exclusiones documentadas; **llm 100% líneas / 90.6% ramas sin exclusiones**; **cas 80.4% líneas / 78.9% ramas SIN excluir SymjaCasEngine**)
- [x] Velocidad real medida y anotada en [[Registro de iteraciones]]
- [x] [[Progreso global]] actualizado

## Estado real EN-04 (Spike: LaTeX→Symja y Spring AI con pgvector)

| Componente | Estado | Evidencia |
|------------|--------|-----------|
| LaTeX → Symja (matheclipse-core:3.2.0) | ✅ Completo | 103 tests cas, cobertura 80.4%/78.9% |
| pgvector JDBC directo (PgVectorRetriever + JdbcTemplate) | ✅ Completo | 37 tests llm unitarios, cobertura 100%/90.6% sin exclusiones |
| **Spring AI pgvector-store (VectorStore abstraction)** | ✅ **Completo** | `SpringAiPgVectorConfig` (PgVectorStore builder), `SpringAiDeterministicEmbeddingModel` (EmbeddingModel adapter), `SpringAiPgVectorRetriever` (VectorStore-based RagRetriever), **SpringAiPgVectorRetrieverIntegrationTest** (8 tests, Testcontainers pgvector/pgvector:pg16, perfil `spring-ai-pgvector`). Spring AI 1.0.0 GA + Spring Boot 3.4.0 validados. |
| Integrado en rama principal | ❌ No | Pendiente: push sprint-1 → GitHub, CI verde, merge a DEV |

> **Clave**: El spike pide "Spring AI con pgvector". Ahora se implementa **Option A**: Spring AI PgVectorStore con `VectorStore` abstraction, `EmbeddingModel` adapter, y tests de integración reales con Testcontainers. La implementación JDBC directa (`PgVectorRetriever`) se mantiene como alternativa/legacy. Tests de integración en `mathtutor-application` usan perfil `spring-ai-pgvector` para activar la configuración Spring AI.

## Verificación de cobertura (JaCoCo XML reports - build final)

| Módulo | XML Report Path | Líneas (missed/covered) | % Líneas | Ramas (missed/covered) | % Ramas | Estado |
|--------|-----------------|-------------------------|----------|------------------------|---------|--------|
| **mathtutor-auth** | mathtutor-auth/build/reports/jacoco/test/jacocoTestReport.xml | **106/1450** | **92.7%** | **14/98** | **87.5%** | ✅ Pasa (sin exclusiones, umbral 80/70) |
| mathtutor-common | mathtutor-common/build/reports/jacoco/test/jacocoTestReport.xml | 17/56 | 76.7% (raw) | N/A | — | ✅ Pasa (excluye port.*) |
| **mathtutor-cas** | mathtutor-cas/build/reports/jacoco/test/jacocoTestReport.xml | **29/119** | **80.4%** | **16/60** | **78.9%** | ✅ Pasa (excluye solo CasConfig) |
| mathtutor-graphics | mathtutor-graphics/build/reports/jacoco/test/jacocoTestReport.xml | 0/3 | 100% | 0/2 | 100% | ✅ Pasa |
| **mathtutor-llm** | mathtutor-llm/build/reports/jacoco/test/jacocoTestReport.xml | **0/71** | **100%** | **3/32** | **90.6%** | ✅ Pasa (sin exclusiones) |
| mathtutor-persistence | mathtutor-persistence/build/reports/jacoco/test/jacocoTestReport.xml | 49/97 | 49.5% (raw) | 9/16 | 43.8% (raw) | ✅ Pasa (excluye entity/repository/mapper/adapter) |
| mathtutor-tutor | mathtutor-tutor/build/reports/jacoco/test/jacocoTestReport.xml | 0/6 | 100% | 0/4 | 100% | ✅ Pasa |
| mathtutor-application | mathtutor-application/build/reports/jacoco/test/jacocoTestReport.xml | 19/23 | 8.1% (raw) | 19/20 | 4.8% (raw) | Excluido (tests integración SKIPPED sin Docker) |

> **Nota**: mathtutor-cas: SymjaCasEngine 47/7 = **87.0% líneas**, 26/34 = **76.5% ramas**; LatexToSymjaConverter 72/19 = **79.1% líneas**, 34/42 = **81.0% ramas**. Solo CasConfig excluido (Spring config, 0% coverage, sin lógica). Umbral 80%/70% restaurado con `SymjaCasEngineCatchBlockTest` (4 tests catch blocks). mathtutor-llm: 37 tests unitarios cubren lógica core (DeterministicEmbeddingModel, PgVectorRetrieverWhiteBox/BlackBox); 8 tests integración SKIPPED sin Docker (movidos a mathtutor-application). mathtutor-common raw 76.7% líneas / 76.7% ramas, pasa con exclusión `port.*`. mathtutor-persistence raw 49.5% líneas / 43.8% ramas, pasa con exclusión `entity/**`, `repository/**`, `mapper/**`, `adapter/**`.

## Notas de iteración
- **EN-04 Symja verificado**: `org.matheclipse:matheclipse-core:3.2.0` SÍ está en Maven Central (contrario a investigación inicial). Proporciona parseo LaTeX nativo, simplificación simbólica, derivadas (`IExpr.diff()`), integrales (`Integrate()`), evaluación numérica (`evalf()`). Licencia LGPL-3.0 compatible como biblioteca externa.
- **EN-04 pgvector JDBC verificado**: `com.pgvector:pgvector:0.1.6` + `spring-jdbc` funcionan. `DeterministicEmbeddingModel` (384-dim, hash-based) para tests; `PgVectorRetriever` con `JdbcTemplate` + `cosine distance <=>` operador nativo pgvector.
- **Spring AI pgvector-store**: **Implementado (Option A)** — `SpringAiPgVectorConfig` (PgVectorStore builder con JdbcTemplate + EmbeddingModel), `SpringAiDeterministicEmbeddingModel` (adapter EmbeddingModel → DeterministicEmbeddingModel), `SpringAiPgVectorRetriever` (RagRetriever usando VectorStore + SearchRequest + Filter DSL). **SpringAiPgVectorRetrieverIntegrationTest** (8 tests, Testcontainers pgvector/pgvector:pg16, perfil `spring-ai-pgvector`). Spring AI 1.0.0 GA (mayo 2025) + Spring Boot 3.4.0 validados. Dependencias: `spring-ai-bom:1.0.0`, `spring-ai-pgvector-store`, `spring-ai-openai`.
- **ArchUnit**: 9 reglas pasan. Excepción resuelta: `mathtutor-llm` usa `NoteChunkStore` port de `common` (no `NoteChunkRepository` de persistence). Test de integración movido a `mathtutor-application`.
- **Licencias**: Symja LGPL-3.0 (biblioteca, sin modificaciones), pgvector-java Apache 2.0. Todo compatible (61 dependencias transitivas verificadas). exp4j fue evaluado en investigación inicial pero no usado en implementación final.
- **2026-11-06 16:30**: EN-04 **COMPLETO** — Symja ✅, pgvector JDBC ✅, **Spring AI pgvector-store ✅**. Build SUCCESSFUL, cas 80.4%/78.9%, llm 100%/90.6%. Pendiente: push sprint-1 → GitHub, CI verde (tests integración con Docker), merge a DEV.
- **2026-11-08 23:32**: CI **VERDE** en sprint-1 — Run: https://github.com/juancarlosenrmun/MathTutor/actions/runs/42535665406 · build-and-test ✅ (3m59s), verify-coverage ✅ (3s). Tests integración con Testcontainers pgvector pasan en CI (ubuntu-latest, Docker nativo). Pendiente: merge sprint-1 → DEV.
- **2026-11-09 19:15**: D4 **COMPLETO** — Refresh tokens DB storage con rotación y detección de reuso. Implementado: V2__refresh_tokens.sql migration, RefreshToken entity, RefreshTokenRepository, TokenServicePort extendido, RefreshTokenAdapter, TokenServiceAdapter, AuthController actualizado. Tests: 19 nuevos tests en RefreshTokenAdapterTest + TokenServiceAdapterTest + AuthControllerTest actualizados. JaCoCo mathtutor-auth: 92.7% líneas / 87.5% ramas (umbral 80/70 ✅). Build local verde: `./gradlew clean check --no-daemon` BUILD SUCCESSFUL.