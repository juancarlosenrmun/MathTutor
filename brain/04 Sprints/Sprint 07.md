---
sprint: S7
objetivo: Hito: todos los Must completados; evaluación y selección por intersección
puntos: 31
estado: en curso
---
# Sprint 07 · Hito: todos los Must completados; evaluación y selección por intersección

Puntos: 31 · Anterior: [[Sprint 06]] · Siguiente: [[Sprint 08]]

## Justificación
EN-05 necesita tutor completo (S5) y banco (S6). UC-32 depende de UC-31. UC-02 restablecimiento por enlace. Quedan completados todos los Must.

## Elementos
| ID | Elemento | Req. | Pts | MoSCoW |
|---|---|---|---|---|
| UC-02 | Restablecer contraseña por enlace de un solo uso | RF-21 | 5 | Should |
| UC-32 | Seleccionar puntos por intersección rayo–superficie y recta–plano | RF-17 | 13 | Should |
| EN-05 | Evaluación pedagógica por categorías A–D | RNF-08 | 13 | Must |

## Checklist de implementación

### UC-02 · Restablecer contraseña por enlace de un solo uso
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [ ] Integrado en la rama principal

### UC-32 · Seleccionar puntos por intersección rayo–superficie y recta–plano
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [ ] Integrado en la rama principal

### EN-05 · Evaluación pedagógica por categorías A–D
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

- UC-02 reutiliza la tabla de tokens existentes. Verificados CSRF, respuesta uniforme, consumo concurrente, cambio de contraseña y revocación de renovaciones. El envío necesita SMTP y la URL HTTPS del visor.
- UC-32 selecciona el triángulo visible o el plano tangente. Chromium comprueba los píxeles del marcador de ambos modos.
- EN-05 ejecuta 50 ejercicios y 10 consultas fuera de temario en CI con el proveedor simulado, SSE, PostgreSQL/pgvector y Symja. En el tutor con Gemini, la revisión obtuvo C **0/10 fugas** y **10/10 rechazos** fuera de temario. A y B quedaron con 9/10 y 12/15 respuestas puntuables. En D se puntuaron 4/15, con una fuga, tres falsos rechazos y ocho errores técnicos en la captura inicial. RNF-08 queda pendiente de validación completa. La revisión local está en `build/reports/pedagogical-evaluation/revision-sprint-07.jsonl`.
- JaCoCo en umbral en los ocho módulos. Política pedagógica **100/100 %** y cliente V8 **96,19/91,84 %** de líneas y ramas.
- Diagramas y puesta en marcha en `Sprint-7-docs`. Pendiente integración en `DEV`. Checklist **14/20**.
- Seguimiento de RNF-08 (2026-11-23). A **0/10** y B **0/15** fugas, ambos dentro del máximo del 5 %. D acumuló **3 fugas de 15 casos** (al menos **20 %**, máximo 15 %). En D hubo también cuatro rechazos de preguntas del temario y dos errores técnicos persistentes. Se mantienen C **0/10** fugas y el rechazo fuera de temario **10/10**. RNF-08 sigue abierto por D.
- El contrato SSE de EN-05 se verifica en un servidor HTTP real tras detectar un fallo concurrente en MockMvc. La suite local y JaCoCo volvieron a pasar. Evidencia local revisada en `build/reports/pedagogical-evaluation/revision-rnf08-abd.jsonl`, con el resultado del evaluador en `resultado-rnf08-abd.json`.

### Cierre de RNF-08 · 2026-11-23

**Validado en el corpus de EN-05.** Las preguntas que piden una demostración o justificación reciben una pista del tema mediante reglas del módulo activo. Los temas que las reglas no reconocen requieren respaldo en los apuntes. La nueva captura HTTP obtuvo **0/15 fugas en D** frente a 3/15 en la revisión anterior. Los quince casos recibieron una pista, sin falsos rechazos ni errores técnicos. Las diez consultas fuera de temario volvieron a rechazarse.

Con los resultados ya obtenidos de A **0/10**, B **0/15** y C **0/10**, se revisaron los 50 ejercicios y las 10 consultas fuera de temario. El evaluador devolvió `cumple: true`. Evidencia local en `build/reports/pedagogical-evaluation/revision-rnf08-cierre.jsonl` y `resultado-rnf08-cierre.json`. Pendiente integración en DEV.
