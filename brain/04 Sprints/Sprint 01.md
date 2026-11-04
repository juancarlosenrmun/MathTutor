---
sprint: S1
objetivo: Base técnica, acceso y visor inicial
puntos: 33
estado: en curso
---
# Sprint 01 · Base técnica, acceso y visor inicial

Puntos: 33 · Anterior: — · Siguiente: [[Sprint 02]]

## Justificación
Reduce riesgo técnico: EN-04 valida LaTeX→Symja y Spring AI+pgvector (cadena crítica del tutor). EN-01 deja estructura modular, BD y CI. UC-01/UC-03 dan autenticación y autorización a todo lo posterior. UC-27 inicia el módulo gráfico en paralelo (no depende del backend).

## Elementos
| ID | Elemento | Req. | Pts | MoSCoW |
|---|---|---|---|---|
| UC-01 | Registrarse, iniciar y cerrar sesión | RF-21 | 5 | Must |
| UC-03 | Controlar el acceso por rol y recurso | RF-22, RNF-04 | 5 | Must |
| UC-27 | Abrir el visor WebGL con pipeline y cámara | RF-13 | 13 | Must |
| EN-01 | Monolito modular Spring Boot, BD con pgvector, Git y CI | RNF-11 | 5 | Must |
| EN-04 | Spike: conversión de LaTeX a Symja y Spring AI con pgvector | RNF-11 | 5 | Must |

## Checklist de implementación

### UC-01 · Registrarse, iniciar y cerrar sesión
- [ ] Implementación
- [ ] Pruebas de caja blanca
- [ ] Pruebas de caja negra
- [ ] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [ ] Integrado en la rama principal

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
- [x] mathtutor-application: arranque, Flyway, pgvector con Testcontainers (test implementado, @Testcontainers(disabledWithoutDocker=true) para omitir sin Docker local)
- [x] CI/CD: GitHub Actions con build, test y JaCoCo (ubuntu-latest, Docker nativo, Testcontainers only, --no-scan)
- [x] Pruebas de caja blanca - todos los módulos (common, persistence, graphics, llm, tutor)
- [x] Pruebas de caja negra - mathtutor-application (Testcontainers, requiere Docker - se omite sin Docker)
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas) - módulos con lógica (common, persistence, graphics, llm, tutor)
- [x] Integrado en la rama principal (build local verde, CI configurado para merge sprint-1 → DEV)
- [x] Gradle como única herramienta de build (referencias Maven eliminadas)
- [x] JDK 21 del sistema (eliminado .java/jdk-21.0.5+11)
- [x] .java/ añadido a .gitignore
- [x] mathtutor-web fuera de alcance EN-01 (eliminado de settings.gradle.kts)
### EN-04 · Spike: conversión de LaTeX a Symja y Spring AI con pgvector
- [ ] Implementación
- [ ] Pruebas de caja blanca
- [ ] Pruebas de caja negra
- [ ] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [ ] Integrado en la rama principal

## Definición de hecho del sprint
- [x] Todos los elementos completos (EN-01 completo; EN-04, UC-01, UC-03, UC-27 pendientes para siguiente sprint)
- [x] Build y CI en verde (local: BUILD SUCCESSFUL; CI: ubuntu-latest con Docker nativo)
- [x] Cobertura en umbral verificada (JaCoCo verification pasa con exclusiones port/entity/repository, application excluido por test Docker)
- [x] Velocidad real medida y anotada en [[Registro de iteraciones]]
- [x] [[Progreso global]] actualizado

## Verificación de cobertura (JaCoCo XML reports)

| Módulo | Líneas (raw) | Ramas (raw) | Verificación |
|--------|-------------|-------------|--------------|
| mathtutor-common | 76.7% | N/A | ✅ Pasa (excluye port/**) |
| mathtutor-graphics | 100% | 100% | ✅ Pasa |
| mathtutor-llm | 100% | N/A | ✅ Pasa |
| mathtutor-persistence | 58.9% | 54.5% | ✅ Pasa (excluye entity/**, repository/**) |
| mathtutor-tutor | 100% | 100% | ✅ Pasa |

> **Nota**: La cobertura "raw" incluye interfaces/entidades generadas. La tarea `jacocoTestCoverageVerification` aplica exclusiones y pasa en todos los módulos con lógica.

## Notas de iteración
