---
sprint: S9
objetivo: Could (1)
puntos: 29
estado: en curso
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
- [ ] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [ ] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [ ] Integrado en la rama principal

### UC-24 · Aplicar la progresión pedagógica
- [ ] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [ ] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [ ] Integrado en la rama principal

### UC-26 · Degradar a modo solo apuntes si falla el LLM
- [ ] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [ ] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [ ] Integrado en la rama principal

### UC-33 · Ver animaciones (secante a tangente, cámara)
- [ ] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [ ] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [ ] Integrado en la rama principal

### EN-03 · Logs estructurados y métricas de latencia y tokens
- [ ] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [ ] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [ ] Integrado en la rama principal

### EN-08 · Independencia del proveedor LLM
- [ ] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [ ] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [ ] Integrado en la rama principal

## Definición de hecho del sprint
- [ ] Todos los elementos completos
- [ ] Build y CI en verde
- [x] Cobertura en umbral verificada
- [ ] Velocidad real medida y anotada en [[Registro de iteraciones]]
- [x] [[Progreso global]] actualizado

## Notas de iteración

### 2026-11-23 22:34 ? Verificaci?n local del sprint

- Se completaron los seis cambios con pruebas de caja blanca y negra. Las seis casillas de integraci?n quedan pendientes de la revisi?n de la PR de `Sprint-9` hacia `DEV`.
- Gradle ejecut? 1167 pruebas Java y 121 del cliente sin fallos ni omisiones. PostgreSQL/pgvector, 20 pruebas Python, Chrome con WebGL2 y Doxygen verificados.
- JaCoCo super? los umbrales en ocho m?dulos. El menor porcentaje de l?neas fue 92,47 % y el de ramas 81,91 %. La pol?tica pedag?gica y el planificador alcanzaron 100 % de l?neas y ramas. V8 registr? 95,17 % de l?neas y 90,60 % de ramas.
- UC-26 verific? el modo solo apuntes por fallo de generaci?n o de embeddings, la cancelaci?n y la recuperaci?n del circuito. La disponibilidad mensual del 99 % queda por medir en despliegue.
- EN-08 se verific? mediante un servidor HTTP simulado y perfiles combinados. Los diagramas de dise?o est?n en `Sprint-9-docs`.
- Checklist de elementos **24/30**. Quedan pendientes la CI remota, la integraci?n y el c?lculo de velocidad real.
