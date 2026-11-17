---
sprint: S4
objetivo: Verificación con CAS, contexto, registro y especificación gráfica
puntos: 33
estado: en curso
---
# Sprint 04 · Verificación con CAS, contexto, registro y especificación gráfica

Puntos: 33 · Anterior: [[Sprint 03]] · Siguiente: [[Sprint 05]]

## Justificación
UC-17 necesita UC-16 y UC-22. UC-20 y UC-25 alimentan UC-11. UC-35 prepara la conexión tutor–visor.

## Elementos
| ID | Elemento | Req. | Pts | MoSCoW |
|---|---|---|---|---|
| UC-11 | Consultar mi progreso e historial | RF-23 | 5 | Must |
| UC-17 | Verificar pasos y resultado con el CAS y clasificar el error | RF-05 | 13 | Must |
| UC-20 | Mantener el contexto de la sesión | RF-10 | 5 | Must |
| UC-25 | Registrar cada interacción de forma estructurada | RF-26, RNF-13 | 5 | Must |
| UC-35 | Generar y validar la especificación gráfica | RF-20 | 5 | Must |

## Checklist de implementación

### UC-11 · Consultar mi progreso e historial
- [ ] Implementación
- [ ] Pruebas de caja blanca
- [ ] Pruebas de caja negra
- [ ] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [ ] Integrado en la rama principal

### UC-17 · Verificar pasos y resultado con el CAS y clasificar el error
- [ ] Implementación
- [ ] Pruebas de caja blanca
- [ ] Pruebas de caja negra
- [ ] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [ ] Integrado en la rama principal

### UC-20 · Mantener el contexto de la sesión
- [ ] Implementación
- [ ] Pruebas de caja blanca
- [ ] Pruebas de caja negra
- [ ] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [ ] Integrado en la rama principal

### UC-25 · Registrar cada interacción de forma estructurada
- [ ] Implementación
- [ ] Pruebas de caja blanca
- [ ] Pruebas de caja negra
- [ ] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [ ] Integrado en la rama principal

### UC-35 · Generar y validar la especificación gráfica
- [ ] Implementación
- [ ] Pruebas de caja blanca
- [ ] Pruebas de caja negra
- [ ] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [ ] Integrado en la rama principal

## Definición de hecho del sprint
- [ ] Todos los elementos completos
- [ ] Build y CI en verde
- [ ] Cobertura en umbral verificada
- [ ] Velocidad real medida y anotada en [[Registro de iteraciones]]
- [ ] [[Progreso global]] actualizado

## Notas de iteración

### 2026-11-17 23:11 · Preparación del sprint

- Creada y seleccionada la rama sprint-4 desde DEV tras git pull --ff-only. README.md contiene cambios previos del usuario, excluidos del trabajo.
- Leídos AGENTS.md, índice, sprint, requisitos, casos de uso, arquitectura, diagramas y seguimiento.
- Búsquedas individuales con find-skills e instalación global individual para Codex de springboot-tdd y spring-ai-integration. La integración PromptScript no admite instalación global; Codex sí quedó instalado.
- Diseño propuesto en DOC: brain/06 Diseño/Sprint 04 - Diseño de implementación.md. La skill brainstorming exige aprobación explícita del diseño antes de implementar; pendiente revisión del usuario.
- Sin implementación ni nuevas pruebas de caja blanca/negra (0/0); cobertura no medida. Ningún UC se marca como completado. Pendientes implementación, pruebas, JaCoCo, revisión, PR a DEV y posterior fase Sprint-4-Frontend tras fusión.
- Checklist: 0/30; elementos completos: 0/5.
