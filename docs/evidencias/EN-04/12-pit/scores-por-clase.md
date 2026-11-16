# PIT Mutation Testing - Puntuaciones por Clase (Commit 1d77997)

**Fecha:** 2026-11-16  
**SHA:** 1d7799705530ca15c50f1e4e32d84e56fc07864b  
**Comando:** `./gradlew :mathtutor-cas:pitest :mathtutor-auth:pitest :mathtutor-common:pitest :mathtutor-graphics:pitest :mathtutor-tutor:pitest`

---

## Resumen por Módulo

| Módulo | Clases | Cobertura Línea | Mutación | Test Strength | Umbral Config | Estado |
|--------|--------|-----------------|----------|---------------|---------------|--------|
| **cas** | 5 | 89% | **60%** | 67% | 55% | ✅ PASS (módulo) |
| **auth** | 5 | 87% | **71%** | 81% | 65% | ✅ PASS (módulo) |
| **common** | 10 | 85% | **82%** | 90% | 75% | ✅ PASS (módulo) |
| **graphics** | 1 | 100% | **100%** | 100% | 90% | ✅ PASS (módulo) |
| **tutor** | 1 | 100% | **100%** | 100% | 90% | ✅ PASS (módulo) |
| **llm** | — | 17% | **14%** | 75% | 15% | ❌ FAIL (193 NO_COVERAGE) |
| **persistence** | — | — | — | — | 40% | No ejecutado |
| **application** | — | — | — | — | 0% | Deshabilitado |

---

## Detalle por Clase (Módulos Core)

### mathtutor-cas (60% mutación — 3 clases < 80%)

| Clase | Línea | Mutación | Strength | Estado ≥80% |
|-------|-------|----------|----------|-------------|
| CasConfig.java | 0% | **0%** | 100% | ❌ EXCLUIDA (config) |
| EquivalenceResult.java | 83% | **50%** | 100% | ❌ |
| **LatexContentExtractor.java** | 99% | **61%** | 63% | ❌ **Crítico** |
| **LatexToSymjaConverter.java** | 82% | **63%** | 71% | ❌ **Crítico** |
| **SymjaCasEngine.java** | 87% | **59%** | 69% | ❌ **Crítico** |

**Supervivientes críticos en CAS:**
- `LatexContentExtractor`: 61% (24 mutantes sobreviven) — MathMutator en `evaluateNumericallyThreeState`, ConditionalsBoundary en tolerancias
- `LatexToSymjaConverter`: 63% (20 mutantes sobreviven) — operadores aritméticos, bucles, resta simbólica
- `SymjaCasEngine`: 59% (15 mutantes sobreviven) — equivalencia simbólica

### mathtutor-auth (71% mutación — 1 clase < 80%)

| Clase | Línea | Mutación | Strength | Estado ≥80% |
|-------|-------|----------|----------|-------------|
| AuditLogAdapter.java | 100% | 88% | 88% | ✅ |
| PasswordEncoderAdapter.java | 83% | 90% | 100% | ✅ |
| **RefreshTokenAdapter.java** | 73% | **64%** | 100% | ❌ |
| TokenServiceAdapter.java | 94% | 65% | 79% | ❌ |
| UserRepositoryAdapter.java | 100% | 79% | 79% | ❌ (borde) |

### mathtutor-common (82% mutación — 4 clases < 80%)

| Clase | Línea | Mutación | Strength | Estado ≥80% |
|-------|-------|----------|----------|-------------|
| LoginUseCase.java | 100% | 88% | 88% | ✅ |
| LogoutUseCase.java | 100% | 100% | 100% | ✅ |
| RegisterUserUseCase.java | 93% | 91% | 97% | ✅ |
| Result.java | 75% | 88% | 88% | ✅ |
| User.java | 100% | 100% | 100% | ✅ |
| PedagogicalAction.java | 100% | 100% | 100% | ✅ |
| **CasEngine.java (port)** | 0% | **0%** | 100% | ❌ INTERFAZ |
| **GraphicSpecValidator.java (port)** | 0% | **0%** | 100% | ❌ INTERFAZ |
| **InputGuard.java (port)** | 0% | **0%** | 100% | ❌ INTERFAZ |
| **OutputGuard.java (port)** | 0% | **0%** | 100% | ❌ INTERFAZ |

> **Nota:** Las 4 clases de `common.port` son **interfaces/ports** sin implementación (0% cobertura esperada). No cuentan para objetivo ≥80%.

### mathtutor-graphics (100% mutación)

| Clase | Línea | Mutación | Strength | Estado ≥80% |
|-------|-------|----------|----------|-------------|
| GraphicSpecServiceImpl.java | 100% | 100% | 100% | ✅ |

### mathtutor-tutor (100% mutación)

| Clase | Línea | Mutación | Strength | Estado ≥80% |
|-------|-------|----------|----------|-------------|
| PedagogicalPolicyImpl.java | 100% | 100% | 100% | ✅ |

---

## Clases Críticas < 80% (Objetivo: ≥80% por clase)

| Prioridad | Módulo | Clase | Mutación | Acción Requerida |
|-----------|--------|-------|----------|------------------|
| **1** | cas | LatexContentExtractor | 61% | Tests de mutación: MathMutator, ConditionalsBoundary, Increments |
| **2** | cas | LatexToSymjaConverter | 63% | Tests de mutación: operadores, límites, resta simbólica |
| **3** | cas | SymjaCasEngine | 59% | Tests de mutación: equivalencia simbólica |
| 4 | auth | RefreshTokenAdapter | 64% | Tests de limpieza/expiración tokens |
| 5 | auth | TokenServiceAdapter | 65% | Tests de firma/validación JWT |
| 6 | auth | UserRepositoryAdapter | 79% | Tests de borde (borde 80%) |

---

## Excepciones Justificadas

| Módulo | Umbral | Justificación |
|--------|--------|---------------|
| **cas** | 55% | 3 clases core <80% (extractor, conversor, engine). Tests de mutación pendientes. |
| **auth** | 65% | 3/5 adaptadores <80%. Tests de mutación pendientes. |
| **common** | 75% | 4 puertos (interfaces) 0% — esperado. Clases de dominio ≥80%. |
| **llm** | 15% | 193 NO_COVERAGE (clases config, real-api tests excluidos). Mutación real ~14%. |
| **persistence** | 40% | Entidades JPA + repositorios (Spring Data genera código). |
| **application** | 0% | Tests integración lentos (Testcontainers), deshabilitado. |

---

## Próximos Pasos para ≥80% por Clase

1. **LatexContentExtractor** (cas): Añadir tests que maten `MathMutator` (operadores aritméticos en `evaluateNumericallyThreeState`), `ConditionalsBoundaryMutator` (límites tolerancia ±10⁻¹⁰), `IncrementsMutator` (contadores bucles)
2. **LatexToSymjaConverter** (cas): Tests para `NEGATE_CONDITIONALS` en límites, `MATH` en división/resta, `INCREMENTS` en bucles
3. **SymjaCasEngine** (cas): Tests equivalencia simbólica (mutantes `SAME_RETURN_VALS`, `NULL_RETURNS`)
4. **RefreshTokenAdapter** (auth): Tests limpieza tokens expirados, revocación familia
5. **TokenServiceAdapter** (auth): Tests firma JWT, validación claims, expiración
6. **UserRepositoryAdapter** (auth): Tests borde findByEmail, existsByEmail

---

**Generado desde:** `build/reports/pitest/*/index.html`  
**Verificación:** `sha256sum -c SHA256SUMS` (incluye este archivo)