# EN-04 Spike Closure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the EN-04 spike by addressing all 10 checklist items: restore mathtutor-cas coverage to ≥80% lines/≥70% branches, verify Spring AI availability, fix ArchUnit violation with NoteChunkStore port, document licenses, update all documentation with real figures, and verify build passes.

**Architecture:** Incremental fixes to existing codebase. Each task produces a verifiable improvement. Tasks 1-4 are code changes with tests; tasks 5-9 are documentation updates; task 10 is final verification.

**Tech Stack:** Java 21, Spring Boot 3.2.5, Gradle, JaCoCo, ArchUnit, Symja (matheclipse-core 3.2.0), pgvector-java 0.1.6, JUnit 5, Mockito, AssertJ

**Spec:** This plan implements the 10-task checklist from the user's request for EN-04 spike closure.

## Global Constraints

- **No touch UC-01, UC-03, UC-27** - These are for next sprint
- **Small commits on sprint-1 branch** - Each task should be a separate commit
- **Build must pass** - `./gradlew clean check --no-daemon --no-scan` must be green after each task
- **CI must not break** - `.github/workflows/ci.yml` must remain valid
- **Coverage thresholds:** 80% lines / 70% branches for modules with logic
- **mathtutor-llm:** 100% coverage required, NO exclusions
- **Exclusions only with written justification** - Document in code and Spike Note

## Review Focus

1. **SymjaCasEngine catch blocks untested** - Lines 57-60, 98-99, 135-137, 142-144 must be covered by tests that trigger exceptions
2. **Spring AI GA availability** - Must check repo1.maven.org for actual GA version, not just milestones
3. **ArchUnit violation** - llm must not depend on persistence; NoteChunkStore port in common is the fix
4. **License accuracy** - matheclipse-core 3.2.0 transitive dependencies must be listed with real licenses
5. **Documentation honesty** - EN-04 Spike Note, Sprint 01, Progreso global, Registro must reflect ACTUAL state, not aspirational

---

### Task 1: mathtutor-cas - Restore ≥80% Lines and ≥70% Branches Coverage

**Files:**
- Modify: `build.gradle.kts` (parent) - lines 66-67, 74-76, 87-89, 105-109
- Modify: `mathtutor-cas/build.gradle.kts` - lines 45-48
- Modify: `mathtutor-cas/src/test/java/com/mathtutor/cas/SymjaCasEngineTest.java`
- Test: `mathtutor-cas/src/test/java/com/mathtutor/cas/SymjaCasEngineTest.java` (new tests)

**Interfaces:**
- Consumes: Existing `SymjaCasEngine`, `LatexToSymjaConverter`
- Produces: Tests covering catch blocks at lines 57-60, 98-99, 135-137, 142-144

- [ ] **Step 1: Write failing tests for SymjaCasEngine catch blocks**

```java
// In SymjaCasEngineTest.java
// Test that triggers verify() catch block (lines 57-60)
@Test @DisplayName("verify handles exception from converter")
void verifyHandlesConverterException() {
    // Need to mock converter to throw exception, or use input that causes converter to throw
    // Symja throws on malformed LaTeX in some cases
}

// Test that triggers parse() catch block (lines 98-99)
@Test @DisplayName("parse handles exception from parseLatex")
void parseHandlesParseLatexException() {
    // Input that causes parseLatex to throw
}

// Test that triggers analyzeDifference first catch block (lines 135-137)
@Test @DisplayName("analyzeDifference handles evaluation exception")
void analyzeDifferenceHandlesEvaluationException() {
    // Expressions that parse but fail numeric evaluation
}

// Test that triggers analyzeDifference second catch block (lines 142-144)
@Test @DisplayName("analyzeDifference handles generic exception")
void analyzeDifferenceHandlesGenericException() {
    // Input that causes exception in analyzeDifference logic
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew :mathtutor-cas:test --tests "com.mathtutor.cas.SymjaCasEngineTest.verifyHandlesConverterException" --no-daemon
```
Expected: Tests FAIL (not implemented yet or exception not triggered)

- [ ] **Step 3: Implement tests that actually trigger the catch blocks**

Analyze SymjaCasEngine code to find inputs that cause exceptions in:
- `converter.areEquivalent()` → triggers verify catch (57-60)
- `converter.parseLatex()` → triggers parse catch (98-99)
- `evaluateAt()` or numeric evaluation → triggers analyzeDifference first catch (135-137)
- Any exception in analyzeDifference → triggers second catch (142-144)

Use malformed LaTeX, null inputs, or expressions Symja cannot parse/evaluate.

- [ ] **Step 4: Run tests to verify they pass and coverage improves**

```bash
./gradlew :mathtutor-cas:test --no-daemon
./gradlew :mathtutor-cas:jacocoTestCoverageVerification --no-daemon
```
Expected: All tests PASS, coverage verification PASSES at 80%/70%

- [ ] **Step 5: Remove 79% threshold and SymjaCasEngine exclusion from parent build.gradle.kts**

In parent `build.gradle.kts`:
- Change line 67: `minimum = BigDecimal("0.80")` (remove cas special case)
- Remove lines 74-76: SymjaCasEngine exclusion from LINE rule
- Remove lines 87-89: SymjaCasEngine exclusion from BRANCH rule
- Remove lines 105-109: SymjaCasEngine classDirectories exclusion
- Keep CasConfig exclusion with written justification comment

- [ ] **Step 6: Remove SymjaCasEngine exclusion from mathtutor-cas/build.gradle.kts**

In `mathtutor-cas/build.gradle.kts`:
- Remove lines 45-48 (jacoco exclude for CasConfig only - keep if justified, or remove if tests cover it)

- [ ] **Step 7: Run full build to verify coverage verification passes at 80%**

```bash
./gradlew clean check --no-daemon --no-scan
```
Expected: BUILD SUCCESSFUL, mathtutor-cas coverage ≥80% lines / ≥70% branches

- [ ] **Step 8: Commit**

```bash
git add build.gradle.kts mathtutor-cas/build.gradle.kts mathtutor-cas/src/test/java/com/mathtutor/cas/SymjaCasEngineTest.java
git commit -m "feat(cas): restore 80% coverage - add tests for SymjaCasEngine catch blocks, remove 79% threshold"
```

---

### Task 2: Remove exp4j References (Dead Code and Documentation)

**Files:**
- Modify: `brain/04 Sprints/Sprint 01.md` - remove exp4j mentions
- Modify: `brain/04 Sprints/EN-04 Spike Note.md` - remove exp4j from license table
- Modify: `brain/05 Seguimiento/Registro de iteraciones.md` - update entries mentioning exp4j

**Interfaces:**
- Consumes: Current documentation state
- Produces: Clean documentation without exp4j references

- [ ] **Step 1: Verify exp4j is not in any Gradle dependencies**

```bash
./gradlew dependencies --configuration mathtutor-cas:compileClasspath | grep -i exp4j
./gradlew dependencies --configuration mathtutor-llm:compileClasspath | grep -i exp4j
```
Expected: No matches

- [ ] **Step 2: Remove exp4j from Sprint 01.md**

Edit `brain/04 Sprints/Sprint 01.md` - remove any exp4j mentions from EN-04 section

- [ ] **Step 3: Remove exp4j from EN-04 Spike Note**

Edit `brain/04 Sprints/EN-04 Spike Note.md` - remove exp4j row from license table, update narrative

- [ ] **Step 4: Update Registro de iteraciones**

Edit `brain/05 Seguimiento/Registro de iteraciones.md` - add note that exp4j was evaluated but not used in final implementation

- [ ] **Step 5: Commit**

```bash
git add "brain/04 Sprints/Sprint 01.md" "brain/04 Sprints/EN-04 Spike Note.md" "brain/05 Seguimiento/Registro de iteraciones.md"
git commit -m "docs: remove exp4j references - not used in final implementation"
```

---

### Task 3: Spring AI - Check GA Version Availability

**Files:**
- Modify: `brain/04 Sprints/EN-04 Spike Note.md` - document exact findings
- Create: `spring-ai-availability-check.md` (temporary) or inline in Spike Note

**Interfaces:**
- Consumes: Maven Central search results
- Produces: Documented exact Spring AI status for Spring Boot 3.2.x

- [ ] **Step 1: Check repo1.maven.org for Spring AI GA versions**

Search for: `https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-bom/`
Look for versions without -M, -RC, -SNAPSHOT suffixes

- [ ] **Step 2: Check Spring Boot version requirement for Spring AI GA**

Spring AI 1.0.0 GA requires Spring Boot 3.3.x (check release notes)
Current project: Spring Boot 3.2.5

- [ ] **Step 3: Attempt to add Spring AI BOM to build.gradle.kts (test)**

Temporarily add to parent build.gradle.kts:
```kotlin
dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:1.0.0")
    }
}
```
Then try: `./gradlew :mathtutor-llm:dependencies --configuration compileClasspath`

- [ ] **Step 4: Document exact error if fails**

If Spring AI 1.0.0 requires Spring Boot 3.3+, document:
- Exact version conflict error
- Spring AI GA version found
- Spring Boot version requirement
- Decision: Stay with pgvector JDBC direct for now, migrate in S2/S3

- [ ] **Step 5: Update EN-04 Spike Note with findings**

Add section with exact versions, errors, and decision rationale

- [ ] **Step 6: Revert test changes to build.gradle.kts**

Remove the temporary Spring AI BOM addition

- [ ] **Step 7: Commit**

```bash
git add "brain/04 Sprints/EN-04 Spike Note.md" build.gradle.kts
git commit -m "docs: document Spring AI GA availability - requires Spring Boot 3.3+, staying with pgvector JDBC"
```

---

### Task 4: ArchUnit - Fix llm→persistence Violation with NoteChunkStore Port

**Files:**
- Create: `mathtutor-common/src/main/java/com/mathtutor/common/port/NoteChunkStore.java`
- Modify: `mathtutor-persistence/src/main/java/com/mathtutor/persistence/repository/NoteChunkRepository.java` (implement port)
- Modify: `mathtutor-llm/src/main/java/com/mathtutor/llm/PgVectorRetriever.java` - use port instead of concrete repository
- Modify: `mathtutor-persistence/src/test/java/com/mathtutor/persistence/repository/NoteChunkRepositoryTest.java` (verify implementation)
- Test: `mathtutor-common/src/test/java/com/mathtutor/common/port/NoteChunkStoreTest.java` (interface contract tests)

**Interfaces:**
- Produces: `NoteChunkStore` port interface in `com.mathtutor.common.port`
- Consumes: `NoteChunkRepository` in persistence, `PgVectorRetriever` in llm

- [ ] **Step 1: Create NoteChunkStore port interface in common**

```java
// mathtutor-common/src/main/java/com/mathtutor/common/port/NoteChunkStore.java
package com.mathtutor.common.port;

import com.mathtutor.common.dto.NoteChunk;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NoteChunkStore {
    NoteChunk save(NoteChunk chunk);
    List<NoteChunk> findAll();
    List<NoteChunk> findByModuleCode(String moduleCode);
    List<NoteChunk> findByTopicCode(String topicCode);
    List<NoteChunk> findByModuleCodeAndTopicCode(String moduleCode, String topicCode);
    Page<NoteChunk> findByModuleCodeAndTopicCode(String moduleCode, String topicCode, Pageable pageable);
    // Vector similarity methods - implemented only with pgvector
    // List<NoteChunk> findSimilarByModuleAndTopic(String moduleCode, String topicCode, float[] embedding, int limit);
    // List<NoteChunk> findSimilarByModule(String moduleCode, float[] embedding, int limit);
    // List<NoteChunk> findSimilarGlobal(float[] embedding, int limit);
}
```

- [ ] **Step 2: Create NoteChunkStoreAdapter in persistence**

```java
// mathtutor-persistence/src/main/java/com/mathtutor/persistence/adapter/NoteChunkStoreAdapter.java
package com.mathtutor.persistence.adapter;

import com.mathtutor.common.dto.NoteChunk;
import com.mathtutor.common.port.NoteChunkStore;
import com.mathtutor.persistence.entity.NoteChunkEntity;
import com.mathtutor.persistence.mapper.NoteChunkMapper;
import com.mathtutor.persistence.repository.NoteChunkRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class NoteChunkStoreAdapter implements NoteChunkStore {
    private final NoteChunkRepository repository;
    private final NoteChunkMapper mapper;

    public NoteChunkStoreAdapter(NoteChunkRepository repository, NoteChunkMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public NoteChunk save(NoteChunk chunk) {
        NoteChunkEntity entity = mapper.toEntity(chunk);
        return mapper.toDto(repository.save(entity));
    }

    @Override
    public List<NoteChunk> findAll() {
        return repository.findAll().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<NoteChunk> findByModuleCode(String moduleCode) {
        return repository.findByModuleCode(moduleCode).stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<NoteChunk> findByTopicCode(String topicCode) {
        return repository.findByTopicCode(topicCode).stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<NoteChunk> findByModuleCodeAndTopicCode(String moduleCode, String topicCode) {
        return repository.findByModuleCodeAndTopicCode(moduleCode, topicCode).stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    public Page<NoteChunk> findByModuleCodeAndTopicCode(String moduleCode, String topicCode, Pageable pageable) {
        return repository.findByModuleCodeAndTopicCode(moduleCode, topicCode, pageable).map(mapper::toDto);
    }
}
```

- [ ] **Step 3: Create NoteChunkMapper in persistence**

```java
// mathtutor-persistence/src/main/java/com/mathtutor/persistence/mapper/NoteChunkMapper.java
package com.mathtutor.persistence.mapper;

import com.mathtutor.common.dto.NoteChunk;
import com.mathtutor.persistence.entity.NoteChunkEntity;
import com.mathtutor.persistence.entity.Module;
import com.mathtutor.persistence.entity.Topic;
import org.springframework.stereotype.Component;

@Component
public class NoteChunkMapper {
    public NoteChunk toDto(NoteChunkEntity entity) {
        if (entity == null) return null;
        return new NoteChunk(
            entity.getId(),
            entity.getContent(),
            entity.getEmbeddingAsFloatArray(),
            entity.getType(),
            entity.getSourceFile(),
            entity.getModuleCode(),
            entity.getTopicCode(),
            entity.getVersion()
        );
    }

    public NoteChunkEntity toEntity(NoteChunk dto) {
        if (dto == null) return null;
        NoteChunkEntity entity = new NoteChunkEntity();
        entity.setId(dto.id());
        entity.setContent(dto.content());
        entity.setType(dto.type());
        entity.setSourceFile(dto.sourceFile());
        entity.setModuleCode(dto.moduleCode());
        entity.setTopicCode(dto.topicCode());
        entity.setVersion(dto.version());
        if (dto.embedding() != null) {
            entity.setEmbeddingFromFloatArray(dto.embedding());
        }
        // Note: module and topic relations need to be set separately if needed
        return entity;
    }
}
```

- [ ] **Step 4: Update PgVectorRetriever to use NoteChunkStore port**

```java
// mathtutor-llm/src/main/java/com/mathtutor/llm/PgVectorRetriever.java
// Change constructor to use NoteChunkStore instead of NoteChunkRepository
// Remove import of persistence.repository and persistence.entity
// Import com.mathtutor.common.port.NoteChunkStore
// Update indexAtStartup() to use noteChunkStore.findAll() and noteChunkStore.save()
```

- [ ] **Step 5: Run ArchUnit tests to verify violation is fixed**

```bash
./gradlew :mathtutor-persistence:test --tests "com.mathtutor.architecture.ArchitectureTest" --no-daemon
```
Expected: All 9 rules PASS, no llm→persistence violation

- [ ] **Step 6: Run all tests to ensure nothing broke**

```bash
./gradlew test --no-daemon
```
Expected: All tests PASS

- [ ] **Step 7: Commit**

```bash
git add mathtutor-common/src/main/java/com/mathtutor/common/port/NoteChunkStore.java
git add mathtutor-persistence/src/main/java/com/mathtutor/persistence/adapter/NoteChunkStoreAdapter.java
git add mathtutor-persistence/src/main/java/com/mathtutor/persistence/mapper/NoteChunkMapper.java
git add mathtutor-llm/src/main/java/com/mathtutor/llm/PgVectorRetriever.java
git commit -m "feat(arch): fix llm→persistence violation - add NoteChunkStore port in common, adapter in persistence"
```

---

### Task 5: Licenses - List matheclipse-core 3.2.0 Transitive Dependencies

**Files:**
- Modify: `brain/04 Sprints/EN-04 Spike Note.md` - add license section with full dependency tree

**Interfaces:**
- Consumes: `./gradlew dependencies` output
- Produces: Complete license documentation

- [ ] **Step 1: Get full dependency tree for mathtutor-cas**

```bash
./gradlew :mathtutor-cas:dependencies --configuration compileClasspath > cas-deps.txt
```

- [ ] **Step 2: Extract matheclipse-core 3.2.0 transitive dependencies**

Analyze cas-deps.txt for `org.matheclipse:matheclipse-core:3.2.0` and its dependencies

- [ ] **Step 3: Check licenses for each transitive dependency**

For each dependency, check Maven Central or project homepage for license

- [ ] **Step 4: Document in EN-04 Spike Note**

Add table with: Module, Version, License, Compatibility

- [ ] **Step 5: Commit**

```bash
git add "brain/04 Sprints/EN-04 Spike Note.md"
git commit -m "docs: add matheclipse-core 3.2.0 transitive dependency licenses"
```

---

### Task 6: EN-04 Spike Note - Update to Real State

**Files:**
- Modify: `brain/04 Sprints/EN-04 Spike Note.md`

**Interfaces:**
- Consumes: Results from Tasks 1-5
- Produces: Accurate spike documentation

- [ ] **Step 1: Update executive summary**

Change "VERIFICADO" to "VERIFICADO PARCIALMENTE" or "VERIFICADO CON LIMITACIONES DOCUMENTADAS"

- [ ] **Step 2: Add real figures**

- White box tests: cas=103, llm=37, total=140
- Black box tests: cas=32, llm=8 (SKIPPED without Docker), total=40
- Coverage by module (exact from JaCoCo report)
- Spring AI exact error (from Task 3)
- Impact on 55-point critical path (S1=33, S2=32, etc.)

- [ ] **Step 3: Document partial status honestly**

If Spring AI not available, EN-04 is partial. Note what's done vs what's pending.

- [ ] **Step 4: Commit**

```bash
git add "brain/04 Sprints/EN-04 Spike Note.md"
git commit -m "docs: update EN-04 Spike Note with real figures and partial status"
```

---

### Task 7: Sprint 01 - Update DoD with Accurate Checkboxes

**Files:**
- Modify: `brain/04 Sprints/Sprint 01.md`

**Interfaces:**
- Consumes: Results from Tasks 1-6
- Produces: Honest Definition of Done

- [ ] **Step 1: Update EN-04 checklist**

Change from all `[x]` to reflect actual state:
- Implementation: `[x]`
- White box tests: `[x]`
- Black box tests: `[x]` (note SKIPPED without Docker)
- Coverage at threshold: `[x]` ONLY if Task 1 achieves 80%/70%
- Integrated: `[x]` (build local green)

- [ ] **Step 2: Update Definition of Done**

- "Todos los elementos completos" → `[ ]` until UC-01/03/27 done
- "Build y CI en verde" → `[ ]` until CI passes on GitHub Actions
- Keep `[x]` for what's truly done (EN-01 complete, EN-04 implemented and verified locally)

- [ ] **Step 3: Update coverage table**

Use exact figures from JaCoCo after Task 1

- [ ] **Step 4: Commit**

```bash
git add "brain/04 Sprints/Sprint 01.md"
git commit -m "docs: update Sprint 01 DoD with accurate checkboxes - EN-01 done, EN-04 partial, UC-01/03/27 pending"
```

---

### Task 8: Progreso Global - Update S1 with Real Done/Total and Points

**Files:**
- Modify: `brain/05 Seguimiento/Progreso global.md`

**Interfaces:**
- Consumes: Sprint 01 actual completion
- Produces: Accurate progress tracking

- [ ] **Step 1: Calculate real done/total for S1**

EN-01: 5 pts - DONE (all checklist items)
EN-04: 5 pts - PARTIAL (if Spring AI not available) or DONE (if all tasks complete)
UC-01: 5 pts - PENDING
UC-03: 5 pts - PENDING
UC-27: 13 pts - PENDING

Total S1: 33 pts
Done: 5 (EN-01) + 0 or 5 (EN-04) = 5 or 10

- [ ] **Step 2: Update Progreso global.md**

| Sprint | Pts | Hechos | Estado |
|---|---|---|---|
| S1 | 33 | 5/5 (EN-01) + X/5 (EN-04) | EN-01 done, EN-04 partial, UC-01/03/27 pending |

- [ ] **Step 3: Commit**

```bash
git add "brain/05 Seguimiento/Progreso global.md"
git commit -m "docs: update Progreso global with real S1 done/total points"
```

---

### Task 9: Registro de Iteraciones - Add New Entry with Real Figures

**Files:**
- Modify: `brain/05 Seguimiento/Registro de iteraciones.md`

**Interfaces:**
- Consumes: All task results
- Produces: Standard format iteration entry

- [ ] **Step 1: Create new entry at top of file**

Format:
```
## 2026-11-06 HH:MM · Sprint 01 · EN-04: Spike closure - coverage restored, ArchUnit fixed, Spring AI checked
- Implementado: ...
- Pruebas: caja blanca N · caja negra N · cobertura X%
- Cobertura: cas XX%/XX%, llm XX%/XX%, graphics XX%/XX%, tutor XX%/XX%
- Build local: ./gradlew clean check --no-daemon BUILD SUCCESSFUL
- ArchUnit: 9 reglas pasan (sin excepciones)
- Spring AI: 1.0.0 GA requiere Spring Boot 3.3+, proyecto en 3.2.5 → pgvector JDBC mantenido
- Licencias: matheclipse-core 3.2.0 + transitivas documentadas
- Checklist: Sprint 01 EN-04 [x] implementado y verificado con limitaciones documentadas
- Pendiente: Push sprint-1 → GitHub, CI verde, merge a DEV, luego UC-01, UC-03, UC-27
```

- [ ] **Step 2: Use exact figures from Tasks 1, 3, 4, 5**

- [ ] **Step 3: Commit**

```bash
git add "brain/05 Seguimiento/Registro de iteraciones.md"
git commit -m "docs: add Registro entry for EN-04 spike closure with real figures"
```

---

### Task 10: Final Verification - Run Full Build and Verify CI

**Files:**
- No code changes - verification only

**Interfaces:**
- Consumes: All previous tasks
- Produces: Confirmed green build

- [ ] **Step 1: Run full clean check**

```bash
./gradlew clean check --no-daemon --no-scan
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Verify all coverage thresholds pass**

Check output for:
- mathtutor-cas: ≥80% lines / ≥70% branches
- mathtutor-llm: 100% / 100% (no exclusions)
- mathtutor-graphics: 100% / 100%
- mathtutor-tutor: 100% / 100%
- mathtutor-common: passes with port exclusions
- mathtutor-persistence: passes with entity/repository exclusions

- [ ] **Step 3: Verify ArchUnit passes with no exceptions**

```bash
./gradlew :mathtutor-persistence:test --tests "com.mathtutor.architecture.ArchitectureTest" --no-daemon
```
Expected: 9 rules pass, NO exceptions documented

- [ ] **Step 4: Verify CI workflow still valid**

Check `.github/workflows/ci.yml` - ensure no breaking changes

- [ ] **Step 5: Run quick CI simulation**

```bash
./gradlew build --no-daemon --no-scan -x test
./gradlew test --no-daemon --no-scan
```

- [ ] **Step 6: Final commit (if any last-minute fixes)**

```bash
git add -A
git commit -m "chore: final verification - all checks pass, EN-04 spike closed"
```

- [ ] **Step 7: Report final results**

Output:
- Build status: PASS/FAIL
- Coverage by module with exact figures
- Symja/Spring AI results with exact errors
- Next element: UC-01

---

## Execution Order

Tasks 1-5 are code changes (can be done in parallel if independent, but 4 depends on 1 for coverage):
1. Task 1: mathtutor-cas coverage (code + tests)
2. Task 2: exp4j docs cleanup (independent)
3. Task 3: Spring AI check (independent)
4. Task 4: ArchUnit fix (independent, but needs Task 1 build to pass)
5. Task 5: Licenses (independent)

Then documentation updates (depend on 1-5):
6. Task 6: EN-04 Spike Note
7. Task 7: Sprint 01 DoD
8. Task 8: Progreso global
9. Task 9: Registro de iteraciones

Finally:
10. Task 10: Final verification

## Estimated Effort

- Task 1: 2-3 hours (writing tests for catch blocks is tricky)
- Task 2: 15 min
- Task 3: 30 min
- Task 4: 1-2 hours (creating port, adapter, mapper, updating retriever)
- Task 5: 30 min
- Tasks 6-9: 1 hour total
- Task 10: 15 min

Total: ~5-7 hours

---

## Self-Review Checklist

- [ ] All 10 checklist items addressed
- [ ] No UC-01, UC-03, UC-27 touched
- [ ] Each task has verifiable outcome
- [ ] Commits are small and atomic
- [ ] Coverage thresholds documented with exact values
- [ ] Spring AI check documents exact error
- [ ] ArchUnit fix removes exception completely
- [ ] License table includes transitive dependencies
- [ ] Documentation reflects REAL state, not aspirational
- [ ] Final verification runs clean check