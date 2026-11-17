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
- [ ] Implementación
- [ ] Pruebas de caja blanca
- [ ] Pruebas de caja negra
- [ ] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [ ] Integrado en la rama principal

### UC-15 · Recuperar fragmentos de apuntes por módulo y tema
- [ ] Implementación
- [ ] Pruebas de caja blanca
- [ ] Pruebas de caja negra
- [ ] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [ ] Integrado en la rama principal

### UC-16 · Interpretar la expresión del alumno
- [ ] Implementación
- [ ] Pruebas de caja blanca
- [ ] Pruebas de caja negra
- [ ] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [ ] Integrado en la rama principal

### UC-22 · Cargar el banco de ejercicios orientativo
- [ ] Implementación
- [ ] Pruebas de caja blanca
- [ ] Pruebas de caja negra
- [ ] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [ ] Integrado en la rama principal

### UC-29 · Ver superficies con malla, triangulación y normales
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

- 2026-11-17: iniciada la rama `Sprint-3` desde DEV actualizada. UC-15 incorpora recuperación de apuntes al turno y respuesta fuera de temario sin llamada al LLM cuando el índice no devuelve fragmentos. Pendientes: filtrado por tema en el flujo, verificación completa y cobertura, UC-04, UC-16, UC-22, UC-29, CI remota e integración en DEV. Ninguna casilla se marca hasta disponer de evidencia suficiente.
