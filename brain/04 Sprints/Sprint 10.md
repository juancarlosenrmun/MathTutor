---
sprint: S10
objetivo: Contingencia y Could (2)
puntos: 21
estado: en curso
---
# Sprint 10 · Contingencia y Could (2)

Puntos: 21 · Anterior: [[Sprint 09]] · Siguiente: —

## Justificación
UC-34 completa el cuarto incremento gráfico; EN-06 mide rendimiento con el sistema completo; margen como reserva.

## Elementos
| ID | Elemento | Req. | Pts | MoSCoW |
|---|---|---|---|---|
| UC-34 | Aplicar post-procesamiento | RF-19 | 13 | Could |
| EN-06 | Pruebas de rendimiento y carga | RNF-01, 02, 03 | 8 | Could |

## Checklist de implementación

### UC-34 · Aplicar post-procesamiento
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [x] Integrado en la rama principal

### EN-06 · Pruebas de rendimiento y carga
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [x] Integrado en la rama principal

## Definición de hecho del sprint
- [ ] Todos los elementos completos
- [x] Build y CI en verde
- [x] Cobertura en umbral verificada
- [ ] Velocidad real medida y anotada en [[Registro de iteraciones]]
- [x] [[Progreso global]] actualizado

## Fix del tutor y el visor

- [x] Ejercicios inéditos del módulo, continuidad del diálogo, visor contraído y deslizadores con la coordenada de tangencia implementados.
- [x] Caja blanca del flujo sin coincidencias, filtro de temario, tema del turno anterior y movimiento del punto sin otra petición.
- [x] Caja negra en Chrome con Google AI Studio, seguimiento de una conversación, curva y superficie. Confirmado el rechazo de una consulta ajena al módulo.
- [x] Cobertura JaCoCo y V8 en umbral y verificación Gradle en verde.
- [ ] CI remota e integración del fix en `DEV`.

## Notas de iteración

### 2026-11-24 11:28 · Seguimiento del diálogo y coordenada de tangencia

- [[Casos de uso#UC-20|UC-20]] admite la derivabilidad como propiedad al discutir la continuidad y usa el tema del último turno completado para respuestas breves reconocibles. El tema debe pertenecer al módulo de la sesión. El prompt pide retomar la intervención del alumno.
- [[Casos de uso#UC-31|UC-31]] muestra la coordenada x con formato español junto a la barra del tutor y del laboratorio y la actualiza al moverla.
- 4 casos unitarios nuevos, 1 prueba HTTP de conversación y 1 recorrido Chromium ampliado. Google AI Studio respondió a los cuatro turnos probados en la misma conversación, incluido «¿Por qué?». También se verificó el rechazo de una pregunta ajena.
- Gradle pasó con 1.181 pruebas Java y 127 Node. JaCoCo del tutor 99,20 % de líneas y 94,84 % de ramas. V8 94,95 % y 89,47 %. Chrome y las comprobaciones de accesibilidad pasaron.
- Checklist **17/20**, fix **4/5**, estado `en curso`. Quedan pendientes CI remota, integración en `DEV` y las mediciones de EN-06 ya anotadas. Véase [[Registro de iteraciones]].

### 2026-11-24 10:57 · Fix del tutor y el visor

- La rama `fix/tutor-chat-visor` sale de `DEV`. Se registra en este sprint por los cambios de EN-06 en el streaming del tutor y de UC-34 en el visor compartido.
- Ajuste solicitado a RF-03 ([[Casos de uso#UC-15|UC-15]]). Un ejercicio de un tema reconocible del módulo puede recibir una pista aunque el índice no recupere coincidencias. El umbral RAG se conserva y se llama al generador una sola vez por turno. El CAS ya distingue las expresiones aisladas de los enunciados con fórmulas.
- [[Casos de uso#UC-35|UC-35]] valida la función del enunciado o el ejemplo generado antes de desplegar el visor. [[Casos de uso#UC-31|UC-31]] mueve la recta o el plano al arrastrar los deslizadores de su dominio sin repetir la validación ni cargar otra malla. El visor vuelve a contraerse al cambiar de conversación.
- Se añadieron 6 casos Java y 1 recorrido Node. Pasaron 1.176 pruebas Java, 127 Node, Chromium y Chrome con Google AI Studio. Se comprobó una ecuación inédita y la apertura de una curva y una superficie con sus controles. JaCoCo del tutor 99,18 % de líneas y 94,36 % de ramas. V8 94,92 % y 89,52 %.
- Checklist del fix **4/5** y del sprint **17/20**. Pendientes la CI remota y la integración del fix, además de las mediciones de EN-06 en GPU integrada y del p95 con el proveedor real. Véase [[Registro de iteraciones]].

### 2026-11-24 10:18 · Revisión final del frontend

- Base integrada en `DEV` mediante la [PR #26](https://github.com/juancarlosenrmun/MathTutor/pull/26), merge `e4a1ea1` y CI en verde.
- Contraste verificado en laboratorio y tutor. EN-06 sigue pendiente de las mediciones de RNF-01 con proveedor real y de RNF-02 con GPU integrada.
- Checklist del sprint **13/15**. La revisión de interfaz queda en `Frontend`, con CI e integración propias pendientes. Véase [[Registro de iteraciones]].

- UC-34 añade el control **Realzar contraste** al visor del laboratorio y del tutor. El render a textura aplica el efecto a la escena y conserva las guías y el marcador en su propia capa.
- EN-06 mide HTTP/SSE sobre PostgreSQL con `./gradlew :mathtutor-application:test --tests '*TutorLoadTest'`. Las 30 conversaciones entran en el generador simulado antes de liberar su respuesta. Se observan Hikari y `pg_stat_activity` durante la espera.
- La verificación del 24 de noviembre obtuvo primer TOKEN p95 de 232 ms y máximo de 238 ms, DONE p95 de 246 ms, cero conexiones JDBC activas y cero transacciones abiertas durante la generación.
- Corrección necesaria para EN-06. La cadena de seguridad de los dos POST SSE escribe sus cabeceras antes de iniciar el flujo. Esto evita cierres prematuros observados durante las regresiones. Véase la [incidencia de Spring Security](https://github.com/spring-projects/spring-security/issues/15510).
- `python scripts/smoke-webgl.py --performance` requiere Chrome o Chromium con aceleración y Playwright resoluble desde Node. Se puede indicar su instalación con `MATHTUTOR_BROWSER_DEPS`. El escenario de 100.000 triángulos mide preparación de escena y FPS con y sin contraste, e identifica el adaptador en `mathtutor-graphics/build/reports/client-browser/performance.json`.
- La RTX 4060 disponible superó 120 FPS en ambos modos. La escena quedó lista en 269 ms con render directo y 191 ms con contraste. Queda pendiente repetir la medición con una GPU integrada y medir RNF-01 con el proveedor LLM real.
- Diagramas en la [rama Sprint-10-docs](https://github.com/juancarlosenrmun/MathTutor/tree/Sprint-10-docs).
- Checklist **11/15**. La [PR #26](https://github.com/juancarlosenrmun/MathTutor/pull/26) queda pendiente de integración y de cierre de la velocidad del sprint.
- Safari 27.0.1 en `xcode-27` superó GLSL y píxeles en el primer intento, pero agotó la espera del estado «Visor listo» en el laboratorio. La misma prueba pasó al relanzar el job sobre `676840c`. También pasaron Safari en `macos-26` y el build general. Véase el [segundo intento de Safari](https://github.com/juancarlosenrmun/MathTutor/actions/runs/45357889731/attempts/2).
