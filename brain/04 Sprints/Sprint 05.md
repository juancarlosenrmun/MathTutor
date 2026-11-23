---
sprint: S5
objetivo: Hito: tutor socrático completo con tangentes
puntos: 29
estado: en curso
---
# Sprint 05 · Hito: tutor socrático completo con tangentes

Puntos: 29 · Anterior: [[Sprint 04]] · Siguiente: [[Sprint 06]]

## Justificación
UC-18 exige UC-17 y UC-20. UC-19 en el mismo sprint (sin tutor sin control de fuga). UC-31 sobre UC-29 y UC-35. Al terminar el tutor funciona de extremo a extremo.

## Elementos
| ID | Elemento | Req. | Pts | MoSCoW |
|---|---|---|---|---|
| UC-18 | Guiar con pistas socráticas progresivas | RF-07 | 13 | Must |
| UC-19 | Impedir la revelación de la solución | RF-08 | 8 | Must |
| UC-31 | Ver rectas y planos tangentes dinámicos | RF-16 | 8 | Must |

## Checklist de implementación

### UC-18 · Guiar con pistas socráticas progresivas
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [ ] Integrado en la rama principal

### UC-19 · Impedir la revelación de la solución
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [ ] Integrado en la rama principal

### UC-31 · Ver rectas y planos tangentes dinámicos
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [ ] Integrado en la rama principal

## Definición de hecho del sprint
- [ ] Todos los elementos completos
- [x] Build y CI en verde
- [x] Cobertura en umbral verificada
- [ ] Velocidad real medida y anotada en [[Registro de iteraciones]]
- [x] [[Progreso global]] actualizado

## Notas de iteración

- UC-18 aplica los niveles conceptual, procedimental, ejemplo análogo y revelación. Se muestra la referencia al siguiente intento incorrecto verificable tras tres intentos previos. La migración V10 corrige los contadores de ejercicios con referencia a partir de sus veredictos guardados.
- UC-19 valida el texto completo antes del primer evento de contenido en SSE. Contrasta formas simbólicas con el CAS o pasajes resueltos de los apuntes y sustituye una posible fuga por una pregunta local. La detección de paráfrasis es heurística.
- UC-31 calcula tangentes numéricas para coordenadas editables en el laboratorio y el visor del tutor. Chromium confirmó el cambio de posición de la recta y el trazado del plano.
- Alcance actualizado a 29 puntos a petición del usuario. El prototipo usa la modalidad gratuita de Gemini.
- Las pruebas de los tres casos de uso pasaron en la [CI de la PR #21](https://github.com/juancarlosenrmun/MathTutor/actions/runs/45160969572) sobre el código del sprint. Queda pendiente la integración en `DEV`. Checklist de elementos **12/15**.
