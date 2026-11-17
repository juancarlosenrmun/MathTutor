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
- [x] Integrado en la rama principal

### UC-13 · Conversar con el tutor con respuesta en streaming
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [x] Integrado en la rama principal

### UC-14 · Indexar los apuntes LaTeX al arrancar
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [x] Integrado en la rama principal

### UC-28 · Ver gráficas 2D de la función, su derivada y el área bajo la curva
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

### 2026-11-17 22:51 · Formato Markdown y selección única de módulo

- **Petición**: mostrar con formato las respuestas Markdown del tutor y quitar «Cambiar de módulo» de la pestaña.
- **Implementado**: renderizado progresivo de encabezados, párrafos, énfasis, listas, citas, código y enlaces HTTP(S)/correo mediante nodos DOM; HTML remoto queda como texto. La selección de módulo permanece en «Nueva conversación» y se conservan los chats de la visita.
- **Pruebas**: cuatro pruebas nuevas de cliente (caja blanca y contrato de interfaz), incluidas respuesta SSE fragmentada y enlace peligroso. 62/62 Node y puerta Gradle `:mathtutor-graphics:check jacocoAggregateReport` correctas; V8 1099/1108 líneas y 569/613 ramas; JaCoCo en umbral, sin exclusiones nuevas.
- **Pendiente**: CI remota e integración de esta corrección en `DEV`; 4/5 comprobaciones, checklist del sprint 36/39. No cambia el diseño de dominio ni de clases.

### 2026-11-17 · README de arranque y PR hacia DEV

- **Petición**: documentar el arranque local del frontend con comandos ordenados y abrir PR hacia DEV.
- **Resultado**: README actualizado con PostgreSQL, secreto JWT temporal, `bootRun` y URL `/visor/index.html`. [PR #10 del frontend](https://github.com/juancarlosenrmun/MathTutor/pull/10) y [PR #11 del README](https://github.com/juancarlosenrmun/MathTutor/pull/11) abiertas hacia DEV y fusionables; CI en ejecución.
- **Ramas**: README guardado en DOC (`7a690ff`). La simulación DOC→DEV reveló conflicto en README; la PR documental parte de DEV (`dbf8ea8`) y contiene solo README. Excepción por el conflicto entre DOC y DEV.
- **Pruebas/cobertura**: cambio documental, sin pruebas nuevas ni nuevo informe; evidencia anterior: 48 Node, V8 99,43/91,85 y `:mathtutor-graphics:check` correcto.
- **Pendiente**: CI remota y posterior integración; checklist **32/34**, `estado: en curso`.

### 2026-11-17 · Adaptación visual al boceto `Diseno.pdf`

- **Petición**: reorganizar el frontend según las tres pantallas del boceto y conservar la plantilla MathTutor.
- **Implementado**: selección del catálogo real mediante tarjetas; chats de la visita en barra lateral; conversación y visor WebGL en columnas. El visor vuelve al laboratorio al salir del tutor. Los chats se borran al perder la identidad.
- **Alcance**: el PDF ilustra cuatro categorías y superficies 3D; S2 expone tres bloques y gráficas 2D. Las superficies pertenecen a [[Sprint 03]] (UC-29). No se muestran conversaciones históricas del servidor porque S2 no ofrece un listado de sesiones.
- **Verificación**: 48 pruebas Node en verde, incluida una nueva de caja blanca sobre tarjetas y recuperación local; V8 99,43 % líneas y 91,85 % ramas. Comprobaciones reales Chromium de rutas, móvil, sesión, permisos, cámara y WebGL2 correctas. `:mathtutor-graphics:check` correcto, incluidas pruebas Java e integraciones exigidas por sus dependencias.
- **Pendiente**: CI remota e integración de esta adaptación en DEV. El estado sigue `en curso`.

## Checklist de adaptación al boceto

- [x] Selección de módulos y chats de la visita según el boceto.
- [x] Conversación y visor integrados, con diseño adaptable.
- [x] Caja blanca, caja negra Chromium y V8 en umbral.
- [ ] CI remota verde e integración de la adaptación en DEV (PR #10 abierta).

## Corrección de presentación del tutor

- [x] Implementación de Markdown y eliminación del control duplicado.
- [x] Pruebas de caja blanca del cliente.
- [x] Contrato de caja negra del flujo de nueva conversación.
- [x] Cobertura cliente y JaCoCo en umbral.
- [ ] CI remota e integración en `DEV`.

### 2026-11-17 00:06 · Ampliación frontend

- **Petición/rama**: crear «Sprint 2 Frontend», ejecutar find-skills para frontend e incorporar las funciones S2. DEV actualizada; creada y seleccionada `Sprint-2-Frontend` (Git no admite espacios). Búsqueda `npx skills find frontend` ejecutada; aplicadas las skills locales de frontend, TDD, revisión, verificación y Obsidian.
- **Implementado**: tutor en navegación y espacio del alumno; selección de bloque junto al chat, estados de carga y vaciado de historial explicados. Laboratorio con función, intervalo, derivada e integral aproximadas presentado antes del visor. Diseño existente adaptado a móvil. UC-14 conserva indexación al arrancar, sin panel ni recuperación de S3.
- **Correcciones**: catálogo pendiente bloquea el inicio; cambio de identidad limpia borrador e historial; salir durante una operación invalida resultados tardíos. Tres regresiones reproducidas en rojo y después en verde. Sin cambios en backend, entidades ni contratos públicos.
- **Pruebas**: 47 Node correctas (caja blanca y contratos simulados; tres nuevas de caja blanca); tres comprobaciones Chromium de caja negra (pipeline, página y contrato de interfaz/SSE). Responsive a 320/375/768/1024/1440 px; capturas del tutor en escritorio/móvil inspeccionadas. Regresión completa: 906 Java, cero fallos u omisiones; puerta PostgreSQL/pgvector correcta. Sin llamadas a proveedores LLM reales.
- **Cobertura/build**: `gradlew.bat build jacocoAggregateReport --no-daemon --no-scan` correcto. V8 99,88 % líneas / 93,97 % ramas; JaCoCo en umbral en los ocho módulos, mínimo persistence 85,06/72,92; tutor 96,43/96,67. Sin nuevas exclusiones. Informes en rutas habituales y logs `.sprint02-frontend-*.log`.
- **Revisión/commits**: diff revisado por corrección, seguridad, arquitectura y presentación, sin bloqueantes. Código en `0c5c3fa` y `cce9da7`; diseño y diagramas por separado en DOC (`0f5dd4a`), sin cofirma ni cambios en main.
- **Pendiente**: CI remota e integración de esta ampliación en DEV. Base 4/4 UC, 32/32 puntos y 25/25 comprobaciones históricas; ampliación 4/5, total **29/30**; `estado: en curso`.

## Checklist de ampliación frontend

- [x] Integrar las funciones S2 en las vistas y navegación existentes.
- [x] Pruebas de caja blanca y regresiones de estados del tutor.
- [x] Caja negra Chromium, responsive y revisión de capturas.
- [x] Build local, V8 y JaCoCo en umbral.
- [ ] CI remota verde e integración de la ampliación en DEV (PR #10 y README #11 abiertos).

## Historial de la base S2

### 2026-11-16 23:51 · Apuntes Industriales, CI e integración en DEV

- **Petición**: ejecutar CI e integrar en DEV; durante esa ejecución, incorporar los apuntes del directorio externo `Apuntes/Industriales`.
- **Corpus**: ocho capítulos y **121 fragmentos** de 150–400 palabras, distribuidos entre los tres bloques existentes. Las tres notas introductorias se sustituyen por el contenido aportado. Se conservan doce fuentes originales byte a byte y autoría (Diego García Zamora y Nuria V. Castillo González, Universidad de Jaén), con SHA-256 y correspondencia de tema/módulo en `industriales-manifest.json`.
- **Importador**: `scripts/import_industriales.py`, sin dependencias externas ni ejecución LaTeX. Conserva fórmulas, entornos y parejas teorema–demostración; expande macros conocidas y retira formato editorial. El glosario y los ejercicios propuestos se preservan en las fuentes, fuera del índice de apuntes; no se adelanta el banco de UC-22. `--check` valida reproducibilidad y se ejecuta en CI.
- **Pruebas/evidencia**: build local correcto y repetición de las pruebas de arranque tras la última regeneración, ambas verdes. Revisión independiente sin bloqueantes. [CI del commit 89397ed](https://github.com/juancarlosenrmun/MathTutor/actions/runs/43972987433) correcta: **906 Java (781 blanca / 125 negra e integración), 44 Node, 15 Python y tres comprobaciones Chromium**, sin fallos u omisiones. Los informes remotos confirman los porcentajes de la tabla inferior y V8 **99,88 % líneas / 93,40 % ramas**. De las Python, once verifican el importador y cuatro la puerta de integraciones. Sin nuevas exclusiones de cobertura.
- **Integración**: [PR #9](https://github.com/juancarlosenrmun/MathTutor/pull/9) fusionada en DEV tras CI verde, commit `eca06657da2f78ef4ed8862702747e5f2407441e`; commits originales preservados y sin cofirma. Copia local actualizada en DEV. La CI de DEV comprueba también la integración y la publicación del seguimiento; `main` permanece intacta.
- **Documentación**: plan de importación, contrato de autores y notas de diseño actualizados por separado en DOC. Fuentes externas sin modificar; no se importan PDF, logs, cachés ni archivos de compilación.
- **Estado vigente**: **4/4 UC integrados, 32/32 puntos entregados, 20/20 comprobaciones de UC y checklist total 25/25**. `estado: completado`. Las entradas siguientes conservan la historia previa a la integración.

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
- **Velocidad/estado**: 32/32 puntos implementados y verificados localmente; **0/32 entregados** según la definición de integración. **0/4 UC integrados**, 16/20 verificaciones de UC; checklist total **19/25**. `estado: completado`.
- Logs locales: `.sprint02-build.log`, `.sprint02-verification-final.log`, `.sprint02-browser-final.log`; los fallos iniciales y las comprobaciones parciales quedan en los demás `.sprint02-*.log`, sin versionar artefactos de build.
