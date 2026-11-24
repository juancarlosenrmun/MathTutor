---
sprint: S9
objetivo: Could (1)
puntos: 29
estado: completado
---
# Sprint 09 · Could (1)

Puntos: 29 · Anterior: [[Sprint 08]] · Siguiente: [[Sprint 10]]

## Justificación
Carga por debajo de capacidad para absorber desviaciones.

## Elementos
| ID | Elemento | Req. | Pts | MoSCoW |
|---|---|---|---|---|
| UC-09 | Consultar uso por tema y sesiones por usuario, con auditoría | RF-25, RNF-13 | 5 | Could |
| UC-24 | Aplicar la progresión pedagógica | RF-09 | 5 | Could |
| UC-26 | Degradar a modo solo apuntes si falla el LLM | RNF-07 | 5 | Could |
| UC-33 | Ver animaciones (secante a tangente, cámara) | RF-18 | 8 | Could |
| EN-03 | Logs estructurados y métricas de latencia y tokens | RNF-13 | 3 | Could |
| EN-08 | Independencia del proveedor LLM | RNF-14 | 3 | Could |

## Checklist de implementación

### UC-09 · Consultar uso por tema y sesiones por usuario, con auditoría
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [x] Integrado en la rama principal

### UC-24 · Aplicar la progresión pedagógica
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [x] Integrado en la rama principal

### UC-26 · Degradar a modo solo apuntes si falla el LLM
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [x] Integrado en la rama principal

### UC-33 · Ver animaciones (secante a tangente, cámara)
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [x] Integrado en la rama principal

### EN-03 · Logs estructurados y métricas de latencia y tokens
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [x] Integrado en la rama principal

### EN-08 · Independencia del proveedor LLM
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

### 2026-11-24 10:18 · Revisión final del frontend

- Base integrada en `DEV` mediante la [PR #25](https://github.com/juancarlosenrmun/MathTutor/pull/25), merge `ebfebdb` y CI en verde.
- Práctica guiada y apuntes accesibles en el tutor. Uso por tema y sesiones visibles para administración. Animaciones verificadas con teclado y movimiento reducido.
- Checklist del sprint **35/35**. La revisión de interfaz queda en `Frontend`, con CI e integración propias pendientes. Véase [[Registro de iteraciones]].

### 2026-11-23 22:34 · Verificación local del sprint

- Se implementaron los seis cambios con pruebas de caja blanca y negra. Las seis casillas de integración quedan pendientes de la [PR #25](https://github.com/juancarlosenrmun/MathTutor/pull/25) hacia `DEV`.
- Gradle ejecutó 1167 pruebas Java y 121 del cliente sin fallos ni omisiones. PostgreSQL/pgvector, 20 pruebas Python, Chrome con WebGL2 y Doxygen verificados.
- JaCoCo superó los umbrales en ocho módulos. El menor porcentaje de líneas fue 92,47 % y el de ramas 81,91 %. La política pedagógica y el planificador alcanzaron 100 % de líneas y ramas. V8 registró 95,17 % de líneas y 90,60 % de ramas.
- UC-26 verificó el modo solo apuntes por fallo de generación o de embeddings, la cancelación y la recuperación del circuito. La disponibilidad mensual del 99 % queda por medir en despliegue.
- EN-08 se verificó mediante un servidor HTTP simulado y perfiles combinados. Los diagramas de diseño están en `Sprint-9-docs`.
- Checklist de elementos **24/30**. Quedan pendientes la CI remota, la integración y el cálculo de velocidad real.
