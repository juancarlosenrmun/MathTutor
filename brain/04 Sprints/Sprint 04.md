---
sprint: S4
objetivo: Verificación con CAS, contexto, registro y especificación gráfica
puntos: 33
estado: completado
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
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [x] Integrado en la rama principal

### UC-17 · Verificar pasos y resultado con el CAS y clasificar el error
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [x] Integrado en la rama principal

### UC-20 · Mantener el contexto de la sesión
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [x] Integrado en la rama principal

### UC-25 · Registrar cada interacción de forma estructurada
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [x] Integrado en la rama principal

### UC-35 · Generar y validar la especificación gráfica
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [x] Integrado en la rama principal

## Definición de hecho del sprint
- [x] Todos los elementos completos
- [x] Build y CI en verde
- [x] Cobertura en umbral verificada
- [x] Velocidad real medida y anotada en [[Registro de iteraciones]]
- [x] [[Progreso global]] actualizado

## Notas de iteración

### 2026-11-18 00:03 · Implementación backend verificada

- Publicación (2026-11-18 00:05): [PR #16 a DEV](https://github.com/juancarlosenrmun/MathTutor/pull/16), abierta desde `sprint-4`; [CI inicial en curso](https://github.com/juancarlosenrmun/MathTutor/actions/runs/44196131542). No fusionada; integración y cierre continúan sin marcar.

- Aprobación del diseño mediante «Implementa sprint». Implementados UC-11, UC-17, UC-20, UC-25 y UC-35 en `sprint-4`; documentación y diagramas actualizados por separado en `DOC` (dddc671).
- API de progreso/historial propios paginados; selección explícita de ejercicio del banco o derivada/simplificación libre, con referencias internas. CAS conserva CORRECTO, INCORRECTO, INDETERMINADO y NO_APLICA; pasos previos no incrementan progreso.
- Contexto persistido limitado a diez turnos y 16000 caracteres. Nueva tabla `conversation_turns` (V9), reserva UUID bajo bloqueo corto, recuperación de respuestas completas y estados terminales, expiración de reservas, cancelación/error, métricas reales cuando existen y null cuando faltan. Sin transacción durante el LLM; límite total 30 s y una suscripción por petición.
- Validación gráfica mediante contrato `graphic-spec-v1.schema.json`, comprobaciones de campos, gramática restringida y CAS; curvas y superficies, con evento GRAPHIC_SPEC. Tangentes y política pedagógica de S5 no se adelantan.
- Evidencia: `gradlew check jacocoAggregateReport assemble` correcto; **989 Java**, cero fallos/omisiones. **64 pruebas nuevas**: 22 de caja blanca (verificación, selección, orquestación y métricas) y 42 de caja negra (gráficas, persistencia y API). Los caminos blancos de UC-35 incluyen también las tres pruebas existentes adaptadas al CAS.
- JaCoCo líneas/ramas: application 95,35/87,50; auth 96,91/87,65; cas 92,44/87,97; common 92,26/97,92; graphics 100/93,62; llm 95,18/86,24; persistence 89,21/80,19; tutor 98,09/95,18. Sin exclusiones nuevas.
- Cliente existente: 62 pruebas Node; V8 1099/1108 líneas y 569/613 ramas. Navegador real, 15 pruebas Python, reproducibilidad de apuntes y puerta de integraciones correctos.
- Revisión independiente: corregida pérdida del veredicto CAS y recuperación de estados terminales. Regresiones adicionales verificadas: resuscripción, plazo total, puntos nulos y respuesta vacía. README previo del usuario fuera de los commits.
- Compatibilidad: requestId opcional para el cliente anterior; la idempotencia entre peticiones exige reutilizar UUID. La ampliación frontend lo incorporará después de la fusión.
- Pendiente CI remota, fusión a DEV y cierre de velocidad (provisional: 0/33 puntos integrados). Después se creará `Sprint-4-Frontend` desde DEV. Checklist **22/30**, cinco UC verificados localmente y cero integrados.

### 2026-11-17 23:11 · Preparación del sprint

- Creada y seleccionada la rama sprint-4 desde DEV tras git pull --ff-only. README.md contiene cambios previos del usuario, excluidos del trabajo.
- Leídos AGENTS.md, índice, sprint, requisitos, casos de uso, arquitectura, diagramas y seguimiento.
- Búsquedas individuales con find-skills e instalación global individual para Codex de springboot-tdd y spring-ai-integration. La integración PromptScript no admite instalación global; Codex sí quedó instalado.
- Diseño propuesto en DOC: brain/06 Diseño/Sprint 04 - Diseño de implementación.md. La skill brainstorming exige aprobación explícita del diseño antes de implementar; pendiente revisión del usuario.
- Sin implementación ni nuevas pruebas de caja blanca/negra (0/0); cobertura no medida. Ningún UC se marca como completado. Pendientes implementación, pruebas, JaCoCo, revisión, PR a DEV y posterior fase Sprint-4-Frontend tras fusión.
- Checklist: 0/30; elementos completos: 0/5.

### 2026-11-18 00:12 · Integración de la base

- PR #16 fusionada en DEV por petición expresa del usuario, merge 127667481b2581b8249b749d463905d57c5844f9. CI verde: https://github.com/juancarlosenrmun/MathTutor/actions/runs/44196313691 (head 61e761d).
- Base completada: cinco UC, 33/33 puntos integrados y checklist 30/30. Evidencia previa: 989 Java, 62 Node, JaCoCo/V8 en umbral, navegador y 15 Python correctos. Sin nuevas pruebas en la fusión.
- Petición siguiente: crear Sprint-4-Frontend desde DEV actualizada y ampliar la interfaz para los casos de uso del sprint 4.
