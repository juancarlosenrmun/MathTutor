---
sprint: S1
objetivo: Base técnica, acceso y visor inicial
puntos: 33
estado: en curso
---
# Sprint 01 · Base técnica, acceso y visor inicial

Anterior: — · Siguiente: [[Sprint 02]] · Plan: [[Sprint 01 - Plan de cierre]]

## Estado vigente

Los cinco elementos están implementados y verificados localmente. El cierre formal queda condicionado a CI verde e integración en DEV. Esta nota sustituye las declaraciones contradictorias de cierre de anteriores iteraciones; el histórico se conserva en [[Registro de iteraciones]].

| ID | Elemento | Requisitos | Puntos | Estado |
|---|---|---|---|---|
| UC-01 | Registro, inicio, cierre y renovación de sesión | RF-21 | 5 | Verificado localmente |
| UC-03 | Autorización por rol y propiedad | RF-22, RNF-04 | 5 | Verificado localmente |
| UC-27 | Visor WebGL2, pipeline GLSL y cámara | RF-13 | 13 | Verificado localmente |
| EN-01 | Monolito modular, PostgreSQL+pgvector, migraciones, Git y CI | RNF-11 | 5 | Verificado localmente |
| EN-04 | Spike LaTeX, Symja y recuperación vectorial | RNF-11 | 5 | Verificado localmente |

## Checklist del sprint

- [x] UC-01: implementación, pruebas unitarias y API, CSRF, refresh, rate limit y cobertura en umbral.
- [x] UC-03: tabla de permisos, propiedad, errores 401/403, cambio de rol sin renovar JWT y desactivación verificados.
- [x] UC-27: WebGL2 real con shaders propios, órbita/zoom/desplazamiento, ratón/teclado, resize, recuperación de contexto y cobertura V8.
- [x] EN-01: módulos, arquitectura, Flyway y PostgreSQL+pgvector verificados; CI incluye Java, cliente y prueba real de navegador.
- [x] EN-04: CAS y adaptadores LLM/retriever probados sin consumir cuota remota; integración pgvector determinista.
- [ ] CI del commit final en verde.
- [ ] Integración de los cinco elementos en DEV.

**Checklist técnica: 5/5 elementos (33/33 puntos implementados y verificados); cierre formal pendiente de CI e integración.**

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
- Pendiente formal: resultado de CI remoto e integración en DEV. No se marca completado hasta disponer de ambas evidencias.
