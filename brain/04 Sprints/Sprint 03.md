---
sprint: S3
objetivo: RAG, interpretación de expresiones y superficies
puntos: 34
estado: completado
---
# Sprint 03 · RAG, interpretación de expresiones y superficies

Puntos: 34 · Anterior: [[Sprint 02]] · Siguiente: [[Sprint 04]]

## Justificación
UC-15 depende de UC-14. UC-16 y UC-22 son prerrequisitos de UC-17. UC-29 es el segundo incremento gráfico. UC-04 pronto para tener privacidad/cookies antes de probar con usuarios.

## Elementos
| ID | Elemento | Req. | Pts | MoSCoW |
|---|---|---|---|---|
| UC-04 | Aceptar aviso de privacidad y gestionar cookies | RF-27, RNF-06 | 3 | Must |
| UC-15 | Recuperar fragmentos de apuntes por módulo y tema | RF-03 | 5 | Must |
| UC-16 | Interpretar la expresión del alumno | RF-04 | 8 | Must |
| UC-22 | Cargar el banco de ejercicios orientativo | RF-06 | 5 | Must |
| UC-29 | Ver superficies con malla, triangulación y normales | RF-15 | 13 | Must |

## Checklist de implementación

### UC-04 · Aceptar aviso de privacidad y gestionar cookies
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [x] Integrado en la rama principal

### UC-15 · Recuperar fragmentos de apuntes por módulo y tema
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [x] Integrado en la rama principal

### UC-16 · Interpretar la expresión del alumno
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [x] Integrado en la rama principal

### UC-22 · Cargar el banco de ejercicios orientativo
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [x] Integrado en la rama principal

### UC-29 · Ver superficies con malla, triangulación y normales
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

## Ampliación frontend del Sprint 03

- [x] Interfaz para consultar apuntes y preparar expresiones, y detalle ajustable de superficies
- [x] Pruebas de caja blanca del cliente
- [x] Contratos de caja negra y comprobación HTTP local
- [x] Cobertura cliente y JaCoCo en umbral
- [x] Integrado en `DEV` tras CI remota

## Notas de iteración

- 2026-11-17 16:39: ampliación frontend fusionada a `DEV` por [PR #13](https://github.com/juancarlosenrmun/MathTutor/pull/13), commit `e5fd3dd`, tras [CI de la PR en verde](https://github.com/juancarlosenrmun/MathTutor/actions/runs/44115774524). La ampliación pasa a 5/5; checklist total 35/35. La CI posterior de `DEV` está en ejecución y se comprobará al cerrar la configuración local.

- 2026-11-17 16:14: ampliación frontend en `Sprint-3-Frontend`, creada desde `DEV` actualizada. Las sugerencias del tutor solo rellenan el borrador para UC-15/16; el control 16/32/64 ajusta la malla de UC-29. UC-04 ya tenía vista y preferencias de cookies. El banco UC-22 sigue siendo orientativo e interno; su selección por el alumno no dispone de contrato público en este sprint. `check jacocoAggregateReport` correcto: 925 pruebas Java sin fallos/omisiones; V8 1005/1010 líneas y 531/574 ramas. Salud y visor HTTP 200 en `127.0.0.1:8080`. Ampliación 4/5, pendiente CI e integración en `DEV`; base del sprint 30/30 integrada.

- 2026-11-17: los cinco UC quedaron integrados en `DEV` por [PR #12](https://github.com/juancarlosenrmun/MathTutor/pull/12), commit `a1a0ee2`. [CI de la PR](https://github.com/juancarlosenrmun/MathTutor/actions/runs/44107554358) y [CI de DEV](https://github.com/juancarlosenrmun/MathTutor/actions/runs/44108491687) en verde. Checklist 30/30 (25/25 comprobaciones de UC); velocidad real 34/34 puntos. La validación institucional del aviso por DPD corresponde a EN-09 del Sprint 05 antes de usar datos reales.

- 2026-11-17: los cinco UC tienen implementación y pruebas locales verificadas. `check jacocoAggregateReport` correcto: 925 pruebas Java, tutor 72/74 líneas y 73/76 ramas, aplicación 65/66 líneas y 5/6 ramas; V8 1000/1005 líneas y 524/568 ramas. UC-04 conserva pendiente de validación institucional del aviso por el DPD en EN-09 (Sprint 05). Restan CI remota del último commit e integración en DEV; por ello ningún elemento se considera completo todavía.

- 2026-11-17: UC-29 implementado en `Sprint-3`: malla de `z=f(x,y)`, triangulación con normales unitarias y rechazo de celdas que cruzan singularidades detectables. Pruebas Node y cobertura V8 en umbral; módulo gráfico `check` correcto; smoke de píxeles WebGL2 real correcto. Pendiente integración en DEV; el elemento sigue 0/5 a nivel de sprint.

- 2026-11-17: iniciada la rama `Sprint-3` desde DEV actualizada. UC-15 incorpora recuperación de apuntes al turno y respuesta fuera de temario sin llamada al LLM cuando el índice no devuelve fragmentos. Pendientes: filtrado por tema en el flujo, verificación completa y cobertura, UC-04, UC-16, UC-22, UC-29, CI remota e integración en DEV. Ninguna casilla se marca hasta disponer de evidencia suficiente.
