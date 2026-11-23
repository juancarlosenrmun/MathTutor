# Progreso global

Sprint 01 · README local con IA (2026-11-18 12:13): actualizado en `config-docs` (`29dce2b`), con preparación inicial y arranque diario para Windows/Linux/macOS. Comandos cotejados con `config`; sin cambios de código ni nuevas pruebas. Checklist **22/25**, configuración **4/5**, estado **en curso**; pendiente integración. Véase [[Sprint 01]].

Sprint 01 · configuración local portable (2026-11-18 11:18): **en curso**, configuración **4/5**, checklist **22/25**. PostgreSQL/JWT por dispositivo en `.env`, generación de secretos e inicio `localAi`. 994 Java, 91 Node y contrato Compose correctos; cobertura en umbral. Pendiente CI e integración de `config` en DEV y `config-docs` en DOC. Véase [[Sprint 01]].

Sprint 04 · altura de paneles (2026-11-18 10:18): **en curso**, corrección **4/5**, checklist **38/40**. Barra lateral, chat y visor alineados en altura. 991 Java, 91 Node y Chromium correctos; JaCoCo/V8 en umbral. Pendiente CI e integración. Véase [[Sprint 04]].

Sprint 04 · visor lateral y chat (2026-11-18 10:16): **en curso**, corrección **4/5**, checklist **38/40**. Chat ampliable al ocultar el visor, indicadores de ejercicio internos y barra lateral con contraste. 991 Java, 91 Node y Chromium correctos; V8 98,97/91,82 %, JaCoCo en umbral. Pendiente CI e integración. Véase [[Sprint 04]].

Sprint 04 · corrección del tutor (2026-11-18 10:00): **en curso**, fix **4/5**, checklist **38/40**. 991 Java, 90 Node y Chromium correctos; JaCoCo/V8 en umbral. Pendiente CI e integración de `fix/tutor-visor-sidebar`. Véase [[Sprint 04]].

Sprint 02 · fórmulas del tutor (2026-11-18 01:22): **en curso**, corrección 4/5 y checklist total **40/44**. LaTeX en línea y bloque con KaTeX local/MathML, comprobado durante streaming y al recuperar el historial. 990 Java y 87 Node correctas, Chromium y cobertura en umbral; V8 99,22/91,57 %. Pendiente CI e integración de `fix/login-registro`. Véase [[Sprint 02]].

Sprint 01 · corrección de acceso (2026-11-18 01:10): **en curso**, fix 4/5 y checklist total **18/20**. Rama `fix/login-registro`; cuentas locales de administrador/alumno creadas y login verificado. El registro muestra los requisitos de contraseña rechazados. 990 Java, 81 Node y ocho comprobaciones Chrome correctas; JaCoCo/V8 en umbral. Pendiente CI e integración del fix. Véase [[Sprint 01]].

Sprint 02 · corrección visual del tutor (2026-11-17 22:51): **en curso**, 4/5 comprobaciones locales; pendiente CI e integración en `DEV`. Respuestas Markdown con formato seguro y cambio de módulo solo al iniciar una conversación. 62/62 Node, Gradle y JaCoCo en verde; V8 1099/1108 líneas y 569/613 ramas. Checklist total **36/39**. Véase [[Sprint 02]].

Sprint 03 · corrección de pregunta sugerida (2026-11-17 17:09): **en curso**, 4/5 comprobaciones locales; pendiente CI e integración en `DEV`. La base y la ampliación anterior conservan 5/5 UC, 34/34 puntos y 35/35 casillas integradas. La pregunta genérica quedaba bajo 0,70 de similitud; las sugerencias específicas superan el umbral. 925 Java, 58 Node; V8 1013/1018 líneas y 537/579 ramas. Véase [[Sprint 03]].

Sprint 02 · ampliación frontend (2026-11-17 00:06): **en curso**, 4/5 verificaciones locales y checklist total **29/30**; pendiente CI remota e integración en DEV. Tutor y gráficas integrados en navegación; 47 Node, tres comprobaciones Chromium y 906 Java sin fallos/omisiones. V8 99,88/93,97 y JaCoCo en umbral. La base mantiene 4/4 UC y 32/32 puntos integrados. Véase [[Sprint 02]].

Sprint 02 base (2026-11-16 23:51): completado e integrado en DEV mediante [PR #9](https://github.com/juancarlosenrmun/MathTutor/pull/9), tras [CI verde](https://github.com/juancarlosenrmun/MathTutor/actions/runs/43972987433). Incluye ocho capítulos de Industriales y 121 fragmentos; checklist base 25/25.

> [!note] Estado vigente
> S1 base completado: 5/5 elementos, 33/33 puntos, CI verde e integrado en DEV. Ampliación frontend en `Sprint-1-Frontend`: 4/5 comprobaciones verificadas localmente; pendiente de CI remota e integración en DEV. Véase [[Sprint 01]].

Última verificación de integración (2026-11-16 19:40): conflictos del pull resueltos, build y cobertura en verde, 863 pruebas Java correctas y aplicación local operativa. S1 conserva 5/5 elementos y 33/33 puntos.

Ampliación frontend (2026-11-16 22:10): seis vistas, 29 pruebas Node, tres comprobaciones Chromium y cuatro regresiones Python correctas. Build y cobertura en umbral; vista previa en `http://127.0.0.1:8081/visor/index.html`. Estado de la ampliación: 4/5, pendiente CI remota e integración.

Revisión con contexto de los diez sprints (2026-11-16 22:21): siete vistas de S1, incluido espacio autenticado por rol; 31 pruebas Node, V8 99,80/96,98, JaCoCo en umbral y contrato Chromium correcto. Iteración 3/3 local; ampliación 4/5 pendiente de CI remota e integración. El resto de sprints no cambia de estado.

Publicación (2026-11-16 22:39): [PR #8 hacia DEV](https://github.com/juancarlosenrmun/MathTutor/pull/8) abierta, sin conflictos y con CI iniciada. Se conserva 4/5 en la ampliación hasta CI verde e integración.

| Sprint | Objetivo | Pts | Hechos | Estado |
|---|---|---|---|---|
| [[Sprint 01\|S1]] | Base técnica, acceso y visor inicial | 33 | 5/5 base (33/33 pts); frontend 4/5; fix acceso 4/5; configuración 4/5; README actualizado; checklist 22/25 | en curso (config pendiente de CI e integración; histórico de acceso/frontend conservado) |
| [[Sprint 02\|S2]] | Chat con LLM, apuntes indexados y gráficas 2D | 32 | 4/4 base (32/32 pts); corrección Markdown 4/5; fórmulas 4/5 | en curso (corrección pendiente de CI e integración; checklist 40/44) |
| [[Sprint 03\|S3]] | RAG, interpretación de expresiones y superficies | 34 | 5/5 base (34/34 pts); frontend 5/5; corrección 4/5; checklist 39/40 | en curso (corrección pendiente de CI e integración) |
| [[Sprint 04\|S4]] | Verificación con CAS, contexto, registro y especificación gráfica | 33 | 5/5 UC y 33/33 puntos integrados; base 30/30; frontend 4/5; corrección de visor y chat (altura alineada) 4/5; total 38/40 | en curso ([PR #17](https://github.com/juancarlosenrmun/MathTutor/pull/17) frontend con CI verde, pendiente de integración; base PR #16 integrada) |
| [[Sprint 05\|S5]] | Hito: tutor socrático completo con tangentes y RGPD | 34 | 0/4 integrados, 3/4 verificados localmente, 12/20 casillas | en curso (pendientes CI e integración en DEV, y cierre institucional de EN-09) |
| [[Sprint 06\|S6]] | Seguridad transversal, filtrado, banco de ejercicios y superficies iluminadas | 32 | 0/4 | pendiente |
| [[Sprint 07\|S7]] | Hito: todos los Must completados; evaluación y selección por intersección | 31 | 0/3 | pendiente |
| [[Sprint 08\|S8]] | Should restantes y administración | 32 | 0/6 | pendiente |
| [[Sprint 09\|S9]] | Could (1) | 29 | 0/6 | pendiente |
| [[Sprint 10\|S10]] | Contingencia y Could (2) | 21 | 0/2 | pendiente |
