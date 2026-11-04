# Plan EN-01 (restante) · Monolito modular, BD con pgvector, Git y CI

Alcance: solo EN-01 (RNF-11). Fuera: Spring AI (EN-04/EN-08), pipeline WebGL (UC-27), orquestador y casos de uso (S5), REST y seguridad (UC-01/UC-03/EN-02).
Ya hecho: mathtutor-persistence (14 entidades, 14 repositorios, 63 tests), mathtutor-common (DTOs y ports). No se revierte; se anota en Notas de iteración que las entidades adelantan trabajo de otros elementos.
Cobertura: ≥ 80 % líneas y ≥ 70 % ramas por módulo (JaCoCo, check en verify).

## Tareas
1. **JaCoCo en el parent POM**: regla `check` en `verify` (líneas 0,80 / ramas 0,70 por módulo), informe agregado, exclusiones documentadas (clase main, DTO sin lógica). Verificar que el build falla bajo el umbral.
2. **mathtutor-llm** (esqueleto): módulo Maven que depende solo de `common`; paquete de implementación vacío preparado para el port de acceso al LLM. Sin Spring AI. Test de arquitectura.
3. **mathtutor-graphics** (esqueleto): ídem para el motor gráfico, sin WebGL ni shaders.
4. **mathtutor-tutor** (esqueleto): ídem para tutorización, sin orquestador ni política.
5. **Test de arquitectura (ArchUnit)**: llm, graphics, tutor y persistence dependen solo de `common` (interfaces); ninguno depende de otro; `application` es el único que los conecta.
6. **mathtutor-application**: `MathTutorApplication`, perfil de configuración, Flyway, arranque contra PostgreSQL+pgvector con Testcontainers. Tests: el contexto arranca, la extensión `vector` existe, los módulos se cablean solo por interfaces.
7. **CI (GitHub Actions)**: `./mvnw verify` con Java del proyecto, caché de Maven, Docker para Testcontainers, subida del informe JaCoCo como artefacto. Sin Checkstyle ni SpotBugs.
8. **Verificación final**: `./mvnw verify` en verde, informe JaCoCo en umbral, `Sprint 01.md` (EN-01), `Progreso global.md`, `Registro de iteraciones.md`, diagramas de dominio y clases si cambia el diseño.

## Orden
1 → (2, 3, 4 en paralelo) → 5 → 6 → 7 → 8

## Criterios de aceptación EN-01
- Build multimódulo compila y pasa tests.
- Módulos desacoplados por interfaces (verificado por ArchUnit).
- BD con pgvector levantada en tests con Testcontainers.
- CI en verde.
- Cobertura en umbral.