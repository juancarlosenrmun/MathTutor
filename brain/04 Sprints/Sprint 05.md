---
sprint: S5
objetivo: Hito: tutor socrático completo con tangentes y RGPD
puntos: 34
estado: en curso
---
# Sprint 05 · Hito: tutor socrático completo con tangentes y RGPD

Puntos: 34 · Anterior: [[Sprint 04]] · Siguiente: [[Sprint 06]]

## Justificación
UC-18 exige UC-17 y UC-20. UC-19 en el mismo sprint (sin tutor sin control de fuga). UC-31 sobre UC-29 y UC-35. EN-09 documental antes de datos reales. Al terminar el tutor funciona de extremo a extremo.

## Elementos
| ID | Elemento | Req. | Pts | MoSCoW |
|---|---|---|---|---|
| UC-18 | Guiar con pistas socráticas progresivas | RF-07 | 13 | Must |
| UC-19 | Impedir la revelación de la solución | RF-08 | 8 | Must |
| UC-31 | Ver rectas y planos tangentes dinámicos | RF-16 | 8 | Must |
| EN-09 | Cumplimiento RGPD (contrato con el proveedor, registro de tratamientos, DPD) | RNF-06 | 5 | Must |

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

### EN-09 · Cumplimiento RGPD (contrato con el proveedor, registro de tratamientos, DPD)
- [ ] Implementación
- [ ] Pruebas de caja blanca
- [ ] Pruebas de caja negra
- [ ] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [ ] Integrado en la rama principal

## Definición de hecho del sprint
- [ ] Todos los elementos completos
- [ ] Build y CI en verde
- [x] Cobertura en umbral verificada
- [ ] Velocidad real medida y anotada en [[Registro de iteraciones]]
- [x] [[Progreso global]] actualizado

## Notas de iteración

- UC-18 aplica los niveles conceptual, procedimental, ejemplo análogo y revelación. Se muestra la referencia al siguiente intento incorrecto verificable tras tres intentos previos. La migración V10 corrige los contadores de ejercicios con referencia a partir de sus veredictos guardados.
- UC-19 valida el texto completo antes del primer evento de contenido en SSE. Contrasta formas simbólicas con el CAS o pasajes resueltos de los apuntes y sustituye una posible fuga por una pregunta local. La detección de paráfrasis es heurística.
- UC-31 calcula tangentes numéricas para coordenadas editables en el laboratorio y el visor del tutor. Chromium confirmó el cambio de posición de la recta y el trazado del plano.
- EN-09 cuenta con un expediente de registro, contrato y revisión en la rama documental `sprint-5-docs` (`7d2038f`). Quedan pendientes la confirmación institucional de la base jurídica y los plazos, el acuerdo con el proveedor y la validación del DPD de la UJA.
- Los tres casos de uso quedan pendientes de CI remota e integración en `DEV`. Checklist de elementos **12/20**.
