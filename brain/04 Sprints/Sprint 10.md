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
- [ ] Integrado en la rama principal

### EN-06 · Pruebas de rendimiento y carga
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

- UC-34 añade el control **Realzar contraste** al visor del laboratorio y del tutor. El render a textura aplica el efecto a la escena y conserva las guías y el marcador en su propia capa.
- EN-06 mide HTTP/SSE sobre PostgreSQL con `./gradlew :mathtutor-application:test --tests '*TutorLoadTest'`. Las 30 conversaciones entran en el generador simulado antes de liberar su respuesta. Se observan Hikari y `pg_stat_activity` durante la espera.
- La verificación del 24 de noviembre obtuvo primer TOKEN p95 de 232 ms y máximo de 238 ms, DONE p95 de 246 ms, cero conexiones JDBC activas y cero transacciones abiertas durante la generación.
- Corrección necesaria para EN-06. La cadena de seguridad de los dos POST SSE escribe sus cabeceras antes de iniciar el flujo. Esto evita cierres prematuros observados durante las regresiones. Véase la [incidencia de Spring Security](https://github.com/spring-projects/spring-security/issues/15510).
- `python scripts/smoke-webgl.py --performance` requiere Chrome o Chromium con aceleración y Playwright resoluble desde Node. Se puede indicar su instalación con `MATHTUTOR_BROWSER_DEPS`. El escenario de 100.000 triángulos mide preparación de escena y FPS con y sin contraste, e identifica el adaptador en `mathtutor-graphics/build/reports/client-browser/performance.json`.
- La RTX 4060 disponible superó 120 FPS en ambos modos. La escena quedó lista en 269 ms con render directo y 191 ms con contraste. Queda pendiente repetir la medición con una GPU integrada y medir RNF-01 con el proveedor LLM real.
- Diagramas en la [rama Sprint-10-docs](https://github.com/juancarlosenrmun/MathTutor/tree/Sprint-10-docs).
- Checklist **11/15**. La [PR #26](https://github.com/juancarlosenrmun/MathTutor/pull/26) queda pendiente de integración y de cierre de la velocidad del sprint.
- Safari 27.0.1 en `xcode-27` superó GLSL y píxeles en el primer intento, pero agotó la espera del estado «Visor listo» en el laboratorio. La misma prueba pasó al relanzar el job sobre `676840c`. También pasaron Safari en `macos-26` y el build general. Véase el [segundo intento de Safari](https://github.com/juancarlosenrmun/MathTutor/actions/runs/45357889731/attempts/2).
