---
sprint: S3
objetivo: RAG, interpretación de expresiones y superficies
puntos: 34
estado: en curso
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
- [ ] Integrado en la rama principal

### UC-15 · Recuperar fragmentos de apuntes por módulo y tema
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [ ] Integrado en la rama principal

### UC-16 · Interpretar la expresión del alumno
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [ ] Integrado en la rama principal

### UC-22 · Cargar el banco de ejercicios orientativo
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [ ] Integrado en la rama principal

### UC-29 · Ver superficies con malla, triangulación y normales
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [ ] Integrado en la rama principal

## Definición de hecho del sprint
- [ ] Todos los elementos completos
- [ ] Build y CI en verde
- [x] Cobertura en umbral verificada
- [ ] Velocidad real medida y anotada en [[Registro de iteraciones]]
- [x] [[Progreso global]] actualizado

## Notas de iteración

- 2026-11-17: los cinco UC tienen implementación y pruebas locales verificadas. `check jacocoAggregateReport` correcto: 925 pruebas Java, tutor 72/74 líneas y 73/76 ramas, aplicación 65/66 líneas y 5/6 ramas; V8 1000/1005 líneas y 524/568 ramas. UC-04 conserva pendiente de validación institucional del aviso por el DPD en EN-09 (Sprint 05). Restan CI remota del último commit e integración en DEV; por ello ningún elemento se considera completo todavía.

- 2026-11-17: UC-29 implementado en `Sprint-3`: malla de `z=f(x,y)`, triangulación con normales unitarias y rechazo de celdas que cruzan singularidades detectables. Pruebas Node y cobertura V8 en umbral; módulo gráfico `check` correcto; smoke de píxeles WebGL2 real correcto. Pendiente integración en DEV; el elemento sigue 0/5 a nivel de sprint.

- 2026-11-17: iniciada la rama `Sprint-3` desde DEV actualizada. UC-15 incorpora recuperación de apuntes al turno y respuesta fuera de temario sin llamada al LLM cuando el índice no devuelve fragmentos. Pendientes: filtrado por tema en el flujo, verificación completa y cobertura, UC-04, UC-16, UC-22, UC-29, CI remota e integración en DEV. Ninguna casilla se marca hasta disponer de evidencia suficiente.
