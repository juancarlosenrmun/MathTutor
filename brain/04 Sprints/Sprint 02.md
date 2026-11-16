---
sprint: S2
objetivo: Chat con LLM, apuntes indexados y gráficas 2D
puntos: 32
estado: en curso
---
# Sprint 02 · Chat con LLM, apuntes indexados y gráficas 2D

Puntos: 32 · Anterior: [[Sprint 01]] · Siguiente: [[Sprint 03]]

## Justificación
Primer flujo completo de conversación (UC-12, UC-13). UC-14 indexa apuntes LaTeX y fija el contrato de formato con los autores. UC-28 completa el primer incremento gráfico (2D).

## Elementos
| ID | Elemento | Req. | Pts | MoSCoW |
|---|---|---|---|---|
| UC-12 | Seleccionar módulo e iniciar sesión | RF-02 | 3 | Must |
| UC-13 | Conversar con el tutor con respuesta en streaming | RF-01 | 8 | Must |
| UC-14 | Indexar los apuntes LaTeX al arrancar | RF-03 | 13 | Must |
| UC-28 | Ver gráficas 2D de la función, su derivada y el área bajo la curva | RF-14 | 8 | Must |

## Checklist de implementación

### UC-12 · Seleccionar módulo e iniciar sesión
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [ ] Integrado en la rama principal

### UC-13 · Conversar con el tutor con respuesta en streaming
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [ ] Integrado en la rama principal

### UC-14 · Indexar los apuntes LaTeX al arrancar
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [ ] Integrado en la rama principal

### UC-28 · Ver gráficas 2D de la función, su derivada y el área bajo la curva
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [ ] Integrado en la rama principal

## Definición de hecho del sprint
- [ ] Todos los elementos completos
- [ ] Build y CI en verde
- [x] Cobertura en umbral verificada
- [x] Velocidad real medida y anotada en [[Registro de iteraciones]]
- [x] [[Progreso global]] actualizado

## Notas de iteración

### 2026-11-16 23:18 · Implementación y verificación local

- `DEV` actualizada con `git pull --ff-only origin DEV`; creada y seleccionada `Sprint-2`. Git no admite espacios en «Sprint 2». Commits pequeños sin cofirma; no se modifica `main` ni se fusiona en `DEV`.
- UC-12: catálogo de tres bloques, sesión nueva por selección y acceso por propietario/rol. UC-13: POST SSE con TOKEN/DONE/ERROR, una llamada por turno sin reintentos, cancelación y errores genéricos. `open-in-view=false`; la prueba comprueba cero transacciones y conexiones JDBC activas en el gateway.
- UC-14: recursos LaTeX del classpath validados, versión SHA-256, indexación idempotente y sustitución atómica en pgvector; embeddings de documentos fuera de la transacción. Arranque bajo perfil `notes-indexing` y proveedor configurado; tres temas introductorios incluidos, pendiente el contenido completo de autores externos. La recuperación por consulta sigue en UC-15/S3.
- UC-28: parser acotado sin eval, función y derivada numérica, integral con signo por Simpson con control de consistencia, intervalos de dominio y rechazo de oscilaciones no resueltas. Geometría WebGL2 y encuadre ortográfico adaptable; resultados aproximados, sin anticipar CAS conversacional ni especificaciones gráficas de S4.
- Skills: ejecutado `find-skills` para Spring AI y Doxygen; instaladas/verificadas para Codex las seis skills Spring/Java de ECC. Aplicadas las skills disponibles de TDD, seguridad, puertos, JPA, implementación incremental, frontend/WebGL, Obsidian, documentación y verificación. Las búsquedas Doxygen no justificaron añadir una skill de baja adopción; se usa la herramienta oficial.
- Evidencia final: `gradlew.bat build jacocoAggregateReport` correcto. **906 pruebas Java**, cero fallos u omisiones; clasificación por suites: **781 caja blanca / 125 integración y caja negra**. Nuevas de S2: **43 Java (31 blanca / 12 negra)**. **44 pruebas Node**, V8 **99,88 % líneas / 93,40 % ramas**; **3 verificaciones Chromium** (píxeles y contrato de interfaz/SSE a 1200/375 px) y **4 regresiones Python** correctas. Puerta de integraciones PostgreSQL/pgvector correcta.
- Revisión independiente: corregidos embeddings de consulta, polos desplazados, aliasing, encuadre y cálculo de extremos trigonométricos sin cota. Regresiones observadas en rojo y luego en verde; segunda revisión sin defectos bloqueantes concretos.
- Doxygen 1.18.0 ejecutado: `build/doxygen/html/index.html`; 10 advertencias heredadas de Result/NoteChunkStore, ninguna de las clases nuevas. Configuración versionada en `Doxyfile`. Plan, contratos y diagramas actualizados por separado en `DOC`.
- Cobertura: informes en `mathtutor-*/build/reports/jacoco/test/` y agregado en `build/reports/jacoco/aggregate/`. Se mantienen las exclusiones de arranque y DTO sin lógica; en cliente solo `main.mjs`. No se añade ninguna exclusión. La política pedagógica mantiene **100 % líneas / 100 % ramas**.

| Módulo | Líneas | Ramas |
|---|---:|---:|
| application | 98,48 % | 83,33 % |
| auth | 96,91 % | 87,65 % |
| cas | 92,31 % | 87,30 % |
| common | 91,03 % | 97,92 % |
| graphics Java | 100 % | 100 % |
| llm | 95,02 % | 86,89 % |
| persistence | 85,06 % | 72,92 % |
| tutor | 96,43 % | 96,67 % |

- **Pendiente**: publicar la rama, obtener CI remota verde e integrar en `DEV`. Por eso permanecen sin marcar las cuatro integraciones, «Todos los elementos completos» y «Build y CI en verde», aunque el build local pasa.
- **Velocidad/estado**: 32/32 puntos implementados y verificados localmente; **0/32 entregados** según la definición de integración. **0/4 UC integrados**, 16/20 verificaciones de UC; checklist total **19/25**. `estado: en curso`.
- Logs locales: `.sprint02-build.log`, `.sprint02-verification-final.log`, `.sprint02-browser-final.log`; los fallos iniciales y las comprobaciones parciales quedan en los demás `.sprint02-*.log`, sin versionar artefactos de build.
