---
sprint: S1
objetivo: Base técnica, acceso y visor inicial
puntos: 33
estado: en curso
---
# Sprint 01 · Base técnica, acceso y visor inicial

Anterior: — · Siguiente: [[Sprint 02]] · Plan: [[Sprint 01 - Plan de cierre]]

## Estado vigente

Los cinco elementos del sprint base están implementados, verificados por CI e integrados en DEV. La ampliación frontend solicitada después del cierre está verificada localmente en `Sprint-1-Frontend`, pendiente de CI remota e integración; véase su checklist al final. El histórico se conserva en [[Registro de iteraciones]].

| ID | Elemento | Requisitos | Puntos | Estado |
|---|---|---|---|---|
| UC-01 | Registro, inicio, cierre y renovación de sesión | RF-21 | 5 | Completado e integrado en DEV |
| UC-03 | Autorización por rol y propiedad | RF-22, RNF-04 | 5 | Completado e integrado en DEV |
| UC-27 | Visor WebGL2, pipeline GLSL y cámara | RF-13 | 13 | Completado e integrado en DEV |
| EN-01 | Monolito modular, PostgreSQL+pgvector, migraciones, Git y CI | RNF-11 | 5 | Completado e integrado en DEV |
| EN-04 | Spike LaTeX, Symja y recuperación vectorial | RNF-11 | 5 | Completado e integrado en DEV |

## Checklist del sprint

- [x] UC-01: implementación, pruebas unitarias y API, CSRF, refresh, rate limit y cobertura en umbral.
- [x] UC-03: tabla de permisos, propiedad, errores 401/403, cambio de rol sin renovar JWT y desactivación verificados.
- [x] UC-27: WebGL2 real con shaders propios, órbita/zoom/desplazamiento, ratón/teclado, resize, recuperación de contexto y cobertura V8.
- [x] EN-01: módulos, arquitectura, Flyway y PostgreSQL+pgvector verificados; CI incluye Java, cliente y prueba real de navegador.
- [x] EN-04: CAS y adaptadores LLM/retriever probados sin consumir cuota remota; integración pgvector determinista.
- [x] CI del commit final en verde.
- [x] Integración de los cinco elementos en DEV.

**Checklist: 5/5 elementos completados (33/33 puntos), CI en verde e integración en DEV verificada.**

## Evidencia de verificación

- Fecha: 2026-11-16 19:03.
- Java: **863 pruebas**, 737 de caja blanca y 126 de caja negra/integración; 0 fallos, 0 omitidas. Clasificación reproducible: pruebas de repositorio, contratos PostgreSQL, API e integración se cuentan como caja negra; el resto como caja blanca.
- Cliente: 11 pruebas Node con cobertura V8; dos comprobaciones de navegador Chromium real (compilación GLSL/píxeles/cámara y carga de la página).
- Puerta de CI: cuatro regresiones Python; comprueba classname estable en XML JUnit y rechaza integraciones omitidas/fallidas.
- Comandos: `./gradlew clean check jacocoAggregateReport --no-daemon --no-scan`, `python scripts/test_verify_integration_results.py`, `python scripts/verify-integration-results.py`, `python scripts/smoke-webgl.py`.
- Informes: `*/build/reports/jacoco/test/html/index.html`, agregado `build/reports/jacoco/aggregate/html/index.html`, `mathtutor-graphics/build/reports/client/lcov.info` y `mathtutor-graphics/build/reports/client-browser/`.

| Módulo | Líneas | Ramas |
|---|---|---|
| mathtutor-application | 97.56 % | 100.00 % |
| mathtutor-auth | 96.90 % | 87.65 % |
| mathtutor-cas | 92.31 % | 87.30 % |
| mathtutor-common | 91.03 % | 97.92 % |
| mathtutor-graphics | 100.00 % | 100.00 % |
| mathtutor-llm | 95.24 % | 86.50 % |
| mathtutor-persistence | 84.05 % | 72.92 % |
| mathtutor-tutor | 100.00 % | 100.00 % |
| Cliente JavaScript (V8) | 100,00 % | 98,10 % |

Todos cumplen 80/70; tutor cumple 90/90. JaCoCo excluye únicamente el arranque `MathTutorApplication` y DTO de common sin lógica (excepto `PedagogicalAction`). Interfaces sin código ejecutable no aportan contadores. V8 excluye únicamente `main.mjs` (importar y arrancar); `app.mjs` y toda la lógica del cliente se miden. No se excluyen adaptadores, seguridad ni persistencia.

## Notas de iteración

- UC-03 aplica denegación por defecto. Alumno: contenido y sesiones propias; profesor: contenido y estadísticas agregadas; administrador: gestión. `/api/access/sessions/{id}` no revela datos: 204 para propietario con permiso, 403 para ajena/inexistente/sin permiso, 401 sin identidad activa. El acceso individual administrativo ajeno requiere la auditoría de UC-09 y queda pendiente de su sprint.
- El registro público solo permite ALUMNO; elegir PROFESOR o ADMIN se rechaza. El filtro consulta el usuario activo en cada petición; la interfaz refresca el rol cada 3 s con timeout de 2 s y descarta respuestas obsoletas.
- UC-27 se sirve en `/visor/index.html`. Escena de ejes de referencia; no adelanta gráficas, superficies ni interpretación de especificaciones. Conserva el visor al volver desde la caché de navegación y libera recursos al salir.
- Las pruebas ordinarias usan dobles locales; las pruebas externas siguen etiquetadas y requieren ejecución voluntaria. Esta iteración no acredita cuota, semántica ni disponibilidad actuales de Google, ni nuevas cifras PIT/licencias.
- `.gitignore` protege secretos, compilados, cachés, IDE, copias y paquetes de evidencias. Se retiran del índice los artefactos ya versionados conservando las fuentes, wrapper, plantillas `.env` y lockfiles.
- Se conservó `sprint-1`, con cambios previos, para la PR existente a DEV; no se trabajó en main. Los diagramas se actualizan en el worktree DOC, separados del código. Las eliminaciones documentales ajenas encontradas durante la sesión no se incluyen en los commits de implementación.
- Cierre formal 2026-11-16 19:15: [CI en verde](https://github.com/juancarlosenrmun/MathTutor/actions/runs/43925892460) sobre `f484c03`; [PR #6 fusionada en DEV](https://github.com/juancarlosenrmun/MathTutor/pull/6), commit `5205d65540bd4239ef1acb22361acf16105514bf`. Diagramas y auditoría publicados por separado en DOC (`c215792`). Sin pendientes de implementación de S1.

## Evidencia de integración

- Se contrastaron los artefactos remotos: 863 pruebas Java correctas, 0 fallos, 0 omitidas; JaCoCo en umbral en los ocho módulos. V8 y las dos pruebas Chromium también pasan en GitHub Actions.
- La reconciliación con DEV conserva exactamente el árbol de fuentes verificado; evita reintroducir controladores/adaptadores antiguos duplicados, migraciones duplicadas y binarios. No se modifica main.
- Continuación solicitada: se comprobaron `git log` y `git status`; se confirmó que el push de `f484c03` terminó y se ejecutó el commit/push de documentación que quedó interrumpido.

## Verificación después del pull de DEV · 2026-11-16 19:40

- Resueltos seis conflictos y 19 deltas automáticos que reintroducían código antiguo; se preservan la arquitectura modular y las fuentes de `f04669a`, validadas por CI. No se alteran el diseño ni el alcance del sprint.
- Build limpio, 863 pruebas Java (737 blanca / 126 negra), 11 Node y cuatro regresiones de la puerta CI correctas; cero fallos u omisiones Java. JaCoCo conserva los umbrales en todos los módulos y V8 registra 100/98,10.
- WebGL verificado en Chromium y sobre la aplicación real. Seis comprobaciones HTTP correctas, PostgreSQL saludable y aplicación reiniciada en `/visor/index.html` con salud `UP`.
- Checklist confirmada: 5/5, 33/33 puntos. Sin pendientes derivados de la fusión; evidencia detallada en [[Registro de iteraciones]].

## Ampliación de interfaz · Sprint-1-Frontend

Petición: diseñar e implementar todas las vistas de S1 con estilo minimalista y commits pequeños sin cofirma. Git no admite espacios: se utiliza `Sprint-1-Frontend` desde DEV actualizada en lugar del nombre solicitado. No se adelantan UC de otros sprints.

Diseño y plan: conservar HTML/CSS/ES modules y APIs existentes. Frente a añadir un framework o páginas con sesiones independientes, usar vistas con rutas hash permite conservar el token solo en memoria. Paleta: papel `#f6f7f2`, tinta `#202a25`, verde `#285b43`, gris `#626e65`, borde `#dce2d9`; títulos Georgia y cuerpo Segoe UI. Navegación discreta, formularios con etiquetas y errores, laboratorio con visor real y ayuda lateral. Alineación izquierda, espacio generoso y adaptación móvil. Detalle y diagramas actualizados por separado en DOC: [[Sprint 01 - Diseño frontend]].

- [x] UC-01 frontend: inicio, acceso, registro de alumno, cuenta, renovación y cierre; pruebas y cobertura local.
- [x] UC-03 frontend: rol vigente, permisos y vista de acceso restringido; pruebas locales.
- [x] UC-27 frontend: laboratorio responsive, controles de cámara y alternativas accesibles; navegador real.
- [x] EN-01/EN-04: conservar build e integración técnica existentes; regresiones verificadas sin llamadas remotas al LLM.
- [ ] CI de la ampliación en verde e integración en DEV.

Secuencia ejecutada: pruebas de sesión fallidas → cliente de registro/refresh/logout → vistas semánticas y CSS → navegación y formularios → revisión independiente → regresiones y corrección de renovación entre pestañas, polling obsoleto y salto al contenido → build y cobertura. Commits sin cofirma. Skills instaladas globalmente: `anthropics/skills:frontend-design` y `vercel-labs/agent-skills:web-design-guidelines`; se aplican junto a frontend-ui-engineering, TDD y obsidian-markdown.

### Evidencia local de la ampliación

- Java: 863 pruebas (737 blanca / 126 negra), cero fallos u omisiones; `check jacocoAggregateReport` correcto. JaCoCo conserva los umbrales en los ocho módulos; mínimo persistence 84,05 % líneas / 72,92 % ramas, tutor 100/100.
- Cliente: 29 pruebas Node, incluidas las tres regresiones de revisión. V8: 99,79 % líneas / 96,94 % ramas; puerta 80/70 activa, excluye solo `main.mjs` de arranque.
- Navegador Chromium: pipeline GLSL con píxeles reales, carga de página y contrato frontend con rutas, registro, login, cierre, rol, 403, Web Locks nativo y cámara. Sin desbordamientos a 1200 y 375 px. Seis capturas de escritorio/móvil en `mathtutor-graphics/build/reports/client-browser/`.
- Cuatro pruebas Python de la puerta CI correctas; integraciones PostgreSQL/pgvector verificadas sin omisiones. Revisión independiente final sin hallazgos bloqueantes.
- Vista previa operativa en `http://127.0.0.1:8081/visor/index.html`: Spring Boot con PostgreSQL local, secreto JWT aleatorio solo en el entorno del proceso. Salud y entrega de las seis vistas, CSS y módulo JavaScript comprobadas por HTTP 200; no se crean usuarios de prueba.
- Comandos: `./gradlew.bat check jacocoAggregateReport --no-daemon --no-scan`, `./gradlew.bat :mathtutor-graphics:check jacocoAggregateReport --no-daemon --no-scan`, `python scripts/smoke-webgl.py`, `python scripts/test_verify_integration_results.py`, `python scripts/verify-integration-results.py`.

### Notas de iteración de la ampliación

- EN-01 y EN-04 son infraestructura/spike; no requieren nuevas vistas de usuario. Se mantienen fuera de alcance chat, recuperación de contraseña, gráficas y administración de otros sprints.
- El cierre fallido borra el token local y ofrece reintentar la revocación de la cookie; ninguna respuesta de permisos anterior puede ocultar ese reintento. Web Locks coordina mutaciones de cookie entre pestañas en navegadores modernos/contexto seguro; entornos antiguos sin esta API no coordinan pestañas.
- La evidencia anterior de 5/5 UC/EN integrados permanece válida para el sprint base. La ampliación frontend queda **4/5 comprobaciones**, pendiente de CI remota e integración en DEV; no se fusiona ni se modifica main. Por esta ampliación el frontmatter pasa a `en curso`.

## Revisión de vistas con visión global de S1–S10

Se leen los diez sprints completos para situar S1 en el producto: S2 conversación y módulos; S3 RAG/expresiones/superficies; S4 progreso y verificación; S5 tutor socrático y tangentes; S6 controles y seguridad; S7 recuperación y selección; S8 derechos, docencia y administración; S9 auditoría/degradación/animación; S10 postprocesamiento y rendimiento. El mapa y sus decisiones están en [[Sprint 01 - Diseño frontend]], actualizado en DOC.

La iteración de S1 implementa siete vistas: inicio, acceso, registro, espacio autenticado, laboratorio, cuenta y acceso restringido. La portada explica el tutor adaptativo en desarrollo; tras registro o login se abre `#/espacio`. Allí se muestran el rol vigente y accesos reales al laboratorio y a la cuenta. Las cuentas de alumno/profesor/administrador tienen textos propios sin habilitar paneles de otros sprints. Cuenta y laboratorio permiten volver al espacio. No se añaden chat, selección de módulos, historial, estadísticas, subida de apuntes ni datos simulados al producto.

- [x] UC-01: recorrido acceso/registro → espacio → cuenta/laboratorio, sin perder sesión; verificado en Node y Chromium.
- [x] UC-03: espacio protegido y presentación por rol vigente, cambio sin reautenticar y caducidad; sin conceder nuevos permisos.
- [x] UC-27: laboratorio conservado, navegación contextual y diseño adaptable; sin regresiones en pipeline/cámara.

Verificación local de esta iteración: 31 pruebas Node, V8 **99,80 % líneas / 96,98 % ramas**; `:mathtutor-graphics:check jacocoAggregateReport` correcto. Java: 863 pruebas (737 blanca / 126 negra), cero fallos/omitidas, JaCoCo en umbral en todos los módulos. Tres comprobaciones Chromium correctas; se prueban todas las vistas públicas y autenticadas a 1200/375 px, con diez capturas incluyendo espacio alumno/docente/administrativo y móvil. Cuatro regresiones Python y puerta PostgreSQL/pgvector correctas. Revisión independiente sin defectos bloqueantes.

Vista previa reiniciada con los recursos actuales en `http://127.0.0.1:8081/visor/index.html`: salud HTTP 200, siete vistas y módulo `workspaceFor` comprobados. Secreto JWT nuevo únicamente en entorno del proceso; no se crean cuentas de prueba ni se realizan llamadas al LLM.

Estado de la iteración: **3/3 comprobaciones locales**. La ampliación frontend conserva **4/5**, pendiente únicamente de CI remota e integración en DEV. El sprint base conserva 5/5 UC/EN; frontmatter `en curso` mientras siga pendiente la integración de la ampliación.
