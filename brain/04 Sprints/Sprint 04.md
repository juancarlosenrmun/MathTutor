---
sprint: S4
objetivo: Verificación con CAS, contexto, registro y especificación gráfica
puntos: 33
estado: en curso
---
# Sprint 04 · Verificación con CAS, contexto, registro y especificación gráfica

Puntos: 33 · Anterior: [[Sprint 03]] · Siguiente: [[Sprint 05]]

## Justificación
UC-17 necesita UC-16 y UC-22. UC-20 y UC-25 alimentan UC-11. UC-35 prepara la conexión tutor–visor.

## Elementos
| ID | Elemento | Req. | Pts | MoSCoW |
|---|---|---|---|---|
| UC-11 | Consultar mi progreso e historial | RF-23 | 5 | Must |
| UC-17 | Verificar pasos y resultado con el CAS y clasificar el error | RF-05 | 13 | Must |
| UC-20 | Mantener el contexto de la sesión | RF-10 | 5 | Must |
| UC-25 | Registrar cada interacción de forma estructurada | RF-26, RNF-13 | 5 | Must |
| UC-35 | Generar y validar la especificación gráfica | RF-20 | 5 | Must |

## Checklist de implementación

### UC-11 · Consultar mi progreso e historial
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [x] Integrado en la rama principal

### UC-17 · Verificar pasos y resultado con el CAS y clasificar el error
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [x] Integrado en la rama principal

### UC-20 · Mantener el contexto de la sesión
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [x] Integrado en la rama principal

### UC-25 · Registrar cada interacción de forma estructurada
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [x] Integrado en la rama principal

### UC-35 · Generar y validar la especificación gráfica
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

## Ampliación frontend

La base conserva **30/30**, cinco UC y 33/33 puntos integrados en DEV. La ampliación se desarrolla en `Sprint-4-Frontend` desde DEV 713c89b; no se añaden puntos a los UC ya contabilizados.

- [x] Interfaz de los cinco UC: historial recuperable, ejercicios/progreso, diagnóstico, envío UUID y gráficas validadas
- [x] Pruebas de caja blanca y contratos de API
- [x] Pruebas de caja negra y navegador real (320–1440 px)
- [x] Build y cobertura V8/JaCoCo en umbral
- [ ] CI verde e integración de la ampliación en DEV

## Corrección del tutor: visor, ejercicios y acceso docente

- [x] Visor ocultable lateralmente con chat adaptable, paneles de igual altura, barra lateral con contraste y progreso interno
- [x] Pruebas de caja blanca del inicio de ejercicio desde el mensaje
- [x] Pruebas de caja negra del cliente y Chromium responsive
- [x] Verificación Java y cobertura JaCoCo/V8 en umbral
- [ ] CI verde e integración de `fix/tutor-visor-sidebar` en DEV

## Conversaciones simuladas y avance del ejercicio

- [x] Implementación del seguimiento contextual y la verificación de respuestas declaradas en lenguaje natural
- [x] Pruebas de caja blanca de clasificación y recuperación de contexto
- [x] Pruebas de caja negra de conversación, persistencia y estadística de avance
- [x] Cobertura en umbral (JaCoCo y V8)
- [ ] CI verde e integración de `s04-entrenamiento-tutor` en DEV

## Notas de iteración

### 2026-11-24 16:38 · Conversaciones simuladas y avance

- Rama `s04-entrenamiento-tutor`, creada desde DEV actualizada. [[Casos de uso#UC-20|UC-20]] recupera el ejercicio ante respuestas breves como «La de la derecha» y pide concreción si fallan los embeddings. [[Casos de uso#UC-17|UC-17]] verifica expresiones afirmadas como «Me sale 6*x» y distingue las frases condicionales. [[Casos de uso#UC-25|UC-25]] evita sumar intentos por aclaraciones reconocidas o por turnos rechazados.
- La prueba de [[Casos de uso#UC-11|UC-11]] comprobó el estado público tras cada turno y la estadística docente: un paso verificado dejó el avance en 50 %, un error lo conservó y el resultado final verificado cerró el ejercicio al 100 %. Repetir el envío con el mismo UUID conserva el recuento.
- Gradle ejecutó 1.202 pruebas Java y 129 Node en verde, incluidas tres pruebas nuevas de caja blanca y seis recorridos HTTP/SSE de caja negra (29 turnos). Integraciones PostgreSQL/pgvector sin omisiones. JaCoCo del tutor: 99,16 % de líneas y 94,39 % de ramas. Política pedagógica: 100 % en ambas. V8: 93,84 % y 88,46 %.
- Pendientes: evaluación con un proveedor real, ampliación del corpus para ajuste de pesos, CI remota e integración en DEV. Checklist de esta ampliación **4/5**, total **42/45** y estado `en curso`.

### Plan de la rama de entrenamiento conversacional

1. Simular diálogos completos en español con ejercicios de los tres bloques. Etiquetar cada turno y reservar conversaciones completas para verificar el cambio sin usarlas como ejemplos del tutor.
2. Ejecutar las simulaciones contra el servicio HTTP/SSE con CAS y PostgreSQL/pgvector reales. Usar un generador simulado y comprobar el veredicto, los intentos y el avance después de cada mensaje.
3. Corregir las regresiones reproducidas en [[Casos de uso#UC-20|UC-20]] y [[Casos de uso#UC-17|UC-17]]. Mantener en el backend el registro de estado y avance de [[Casos de uso#UC-25|UC-25]] y su lectura en [[Casos de uso#UC-11|UC-11]]. Las comprobaciones cruzadas de rechazo aprovechan [[Casos de uso#UC-21|UC-21]] sin ampliar el filtro ni la cuota.

#### Corpus conversacional

`mathtutor-application/src/test/resources/tutor-conversations.jsonl` contiene seis conversaciones sintéticas en español, redactadas para esta iteración. Hay tres conversaciones de desarrollo y tres de validación, con 29 turnos del alumno en total: 11 claros, seis ambiguos, cuatro erróneos, cinco fuera de contexto y tres de cierre. Los identificadores y enunciados de las conversaciones son distintos. Cada fila contiene `messages` con roles `user` y `assistant` y `expected` con las expectativas de estado, veredicto, intentos y avance. Las respuestas de referencia ilustran la guía socrática. La prueba envía únicamente los mensajes del alumno y comprueba el contrato de la aplicación sin usar el texto de referencia para simular la respuesta.

El corpus utiliza mensajes con roles y queda pendiente elegir el modelo, su plantilla de chat y la configuración de entrenamiento. Para comparar modelos se conservan los diálogos de validación completos y se mide por separado el rechazo incorrecto, el rechazo de desvíos y la concordancia del avance. Este corpus inicial contiene únicamente datos sintéticos.

### 2026-11-18 00:46 · Arranque local con IA y README

- Petición expresa: reparar las instrucciones del README y dejar la aplicación ejecutándose con IA real. Es un ajuste operativo de arranque heredado; no cambia los UC ni el diseño del sprint.
- Reproducido el fallo del comando anterior: `bootRun` ejecuta desde `mathtutor-application` y no encuentra el `.env` de la raíz. Además faltaba `notes-indexing`, necesario para cargar apuntes; activar solo `spring-ai-pgvector` no indexa.
- Arranque comprobado: Docker `up -d --wait postgres`, JWT temporal generado en memoria, perfiles `dev,google-ai-studio,notes-indexing` e importación `optional:file:../.env[.properties]`. Aplicación dejada en segundo plano, puerto 8080, PID Java 22060. Frontend HTTP 200 y salud UP; índice de 121 fragmentos.
- Verificación de caja negra real: registro temporal, conversación en Funciones y turno de continuidad completado con TOKEN/DONE; proveedor registra 1852 tokens de entrada y 343 de salida. Cuenta y datos temporales eliminados. Cero pruebas unitarias nuevas, sin cambios de código ni nueva medición de cobertura; se conserva la evidencia 990 Java/79 Node y JaCoCo/V8 de la iteración anterior.
- README corregido y versionado por separado en DOC (e5729cc), conservando y actualizando la copia de trabajo que ya tenía cambios previos. Clave local intacta y sin publicar. La aplicación permanece accesible en `http://localhost:8080/visor/index.html`.
- CI del frontend comprobada en verde en https://github.com/juancarlosenrmun/MathTutor/actions/runs/44200914184; PR #17 aún abierta, sin autorización de fusión en esta petición. Checklist **34/35**, pendiente integrar frontend en DEV.

### 2026-11-18 00:34 · Frontend verificado localmente

- Publicación: [PR #17 hacia DEV](https://github.com/juancarlosenrmun/MathTutor/pull/17), desde `Sprint-4-Frontend`. CI remota e integración pendientes; no se fusiona la ampliación en esta iteración.

- Creada `Sprint-4-Frontend` después de fusionar PR #16 y actualizar DEV. Ejecutado `find-skills frontend-design` e instalada individualmente `anthropics/skills@frontend-design` para Codex; aplicada también `frontend-ui-engineering`. Se conserva el diseño actual.
- Historial de cuenta paginado y recuperación de conversaciones activas/cerradas. Selección de banco o derivada/simplificación libre, progreso y veredictos CAS. Catálogo seguro por módulo como soporte de la selección, sin reconocimiento automático ni exposición de soluciones. Envíos con UUID y recuperación manual sin reintentos automáticos.
- Gráficas del backend interpretadas con los parsers existentes; límites del visor controlados y alternativa textual. Limpieza de geometría privada y datos al cambiar de cuenta. Sesiones cerradas sin escritura.
- Revisión independiente corregida: invalidar el panel previo si falla abrir otro historial y renovar el controlador de paginación tras cancelar. Ambas regresiones observadas en rojo y después verde; sin hallazgos importantes pendientes.
- Verificación: `check jacocoAggregateReport assemble` correcto, **990 Java y 79 Node**, cero fallos/omisiones. **18 pruebas nuevas**: 3 blancas (intérprete/limpieza de visor) y 15 negras (transporte, componentes y catálogo API), además del contrato de navegador ampliado. Pasan 15 Python, reproducibilidad de apuntes y puerta de integraciones.
- V8: **1338/1349 líneas (99,18 %) y 782/858 ramas (91,14 %)**. JaCoCo líneas/ramas: application 95,40/87,50; auth 96,91/87,65; cas 92,44/87,97; common 92,31/97,92; graphics 100/93,62; llm 95,18/86,24; persistence 89,34/79,63; tutor 98,09/95,18. Sin exclusiones nuevas.
- Capturas comprobadas en `mathtutor-graphics/build/reports/client-browser/tutor-s4.png` y `tutor-s4-movil.png`. Diseño y diagramas versionados por separado en DOC, commit a59f771. README previo intacto y fuera de los commits.
- Pendiente CI y fusión de frontend en DEV. Checklist total **34/35**, ampliación **4/5**, `estado: en curso`.

### 2026-11-18 00:03 · Implementación backend verificada

- Publicación (2026-11-18 00:05): [PR #16 a DEV](https://github.com/juancarlosenrmun/MathTutor/pull/16), abierta desde `sprint-4`; [CI inicial en curso](https://github.com/juancarlosenrmun/MathTutor/actions/runs/44196131542). No fusionada; integración y cierre continúan sin marcar.

- Aprobación del diseño mediante «Implementa sprint». Implementados UC-11, UC-17, UC-20, UC-25 y UC-35 en `sprint-4`; documentación y diagramas actualizados por separado en `DOC` (dddc671).
- API de progreso/historial propios paginados; selección explícita de ejercicio del banco o derivada/simplificación libre, con referencias internas. CAS conserva CORRECTO, INCORRECTO, INDETERMINADO y NO_APLICA; pasos previos no incrementan progreso.
- Contexto persistido limitado a diez turnos y 16000 caracteres. Nueva tabla `conversation_turns` (V9), reserva UUID bajo bloqueo corto, recuperación de respuestas completas y estados terminales, expiración de reservas, cancelación/error, métricas reales cuando existen y null cuando faltan. Sin transacción durante el LLM; límite total 30 s y una suscripción por petición.
- Validación gráfica mediante contrato `graphic-spec-v1.schema.json`, comprobaciones de campos, gramática restringida y CAS; curvas y superficies, con evento GRAPHIC_SPEC. Tangentes y política pedagógica de S5 no se adelantan.
- Evidencia: `gradlew check jacocoAggregateReport assemble` correcto; **989 Java**, cero fallos/omisiones. **64 pruebas nuevas**: 22 de caja blanca (verificación, selección, orquestación y métricas) y 42 de caja negra (gráficas, persistencia y API). Los caminos blancos de UC-35 incluyen también las tres pruebas existentes adaptadas al CAS.
- JaCoCo líneas/ramas: application 95,35/87,50; auth 96,91/87,65; cas 92,44/87,97; common 92,26/97,92; graphics 100/93,62; llm 95,18/86,24; persistence 89,21/80,19; tutor 98,09/95,18. Sin exclusiones nuevas.
- Cliente existente: 62 pruebas Node; V8 1099/1108 líneas y 569/613 ramas. Navegador real, 15 pruebas Python, reproducibilidad de apuntes y puerta de integraciones correctos.
- Revisión independiente: corregida pérdida del veredicto CAS y recuperación de estados terminales. Regresiones adicionales verificadas: resuscripción, plazo total, puntos nulos y respuesta vacía. README previo del usuario fuera de los commits.
- Compatibilidad: requestId opcional para el cliente anterior; la idempotencia entre peticiones exige reutilizar UUID. La ampliación frontend lo incorporará después de la fusión.
- Pendiente CI remota, fusión a DEV y cierre de velocidad (provisional: 0/33 puntos integrados). Después se creará `Sprint-4-Frontend` desde DEV. Checklist **22/30**, cinco UC verificados localmente y cero integrados.

### 2026-11-17 23:11 · Preparación del sprint

- Creada y seleccionada la rama sprint-4 desde DEV tras git pull --ff-only. README.md contiene cambios previos del usuario, excluidos del trabajo.
- Leídos AGENTS.md, índice, sprint, requisitos, casos de uso, arquitectura, diagramas y seguimiento.
- Búsquedas individuales con find-skills e instalación global individual para Codex de springboot-tdd y spring-ai-integration. La integración PromptScript no admite instalación global; Codex sí quedó instalado.
- Diseño propuesto en DOC: brain/06 Diseño/Sprint 04 - Diseño de implementación.md. La skill brainstorming exige aprobación explícita del diseño antes de implementar; pendiente revisión del usuario.
- Sin implementación ni nuevas pruebas de caja blanca/negra (0/0); cobertura no medida. Ningún UC se marca como completado. Pendientes implementación, pruebas, JaCoCo, revisión, PR a DEV y posterior fase Sprint-4-Frontend tras fusión.
- Checklist: 0/30; elementos completos: 0/5.

### 2026-11-18 00:12 · Integración de la base

- PR #16 fusionada en DEV por petición expresa del usuario, merge 127667481b2581b8249b749d463905d57c5844f9. CI verde: https://github.com/juancarlosenrmun/MathTutor/actions/runs/44196313691 (head 61e761d).
- Base completada: cinco UC, 33/33 puntos integrados y checklist 30/30. Evidencia previa: 989 Java, 62 Node, JaCoCo/V8 en umbral, navegador y 15 Python correctos. Sin nuevas pruebas en la fusión.
- Petición siguiente: crear Sprint-4-Frontend desde DEV actualizada y ampliar la interfaz para los casos de uso del sprint 4.

### 2026-11-18 · Corrección del tutor solicitada por el usuario

- Rama `fix/tutor-visor-sidebar`, creada desde DEV actualizado. Se adscribe a S4 por UC-17/20 y la ampliación del frontend; la petición puntual autoriza también el acceso docente.
- Visor con `details/summary`, ancho estable al cerrar y altura abierta acotada. Barra lateral con separación entre nueva conversación, chats y ayuda; selección activa visible.
- Sin botón ni desplegable de inicio manual. El cliente inicia el ejercicio antes del turno para comandos `Deriva x^2` / `Simplifica x+x` o coincidencia textual exacta con un enunciado del banco cargado (primera página). Las preguntas teóricas y los pasos siguientes continúan sin crear otro ejercicio. No se implementa reconocimiento semántico ni equivalencia CAS de UC-23/S6.
- Profesor habilitado en navegación, conversación, historial y API del tutor; la propiedad de cada sesión sigue siendo obligatoria. Sin cambios de entidades, relaciones ni interfaces públicas Java.
- 90 pruebas Node correctas y Chromium/WebGL2 entre 320 y 1440 px; 991 pruebas Java sin fallos; build y JaCoCo en umbral. V8 99,22 % líneas / 91,06 % ramas. Pendiente CI e integración.

### 2026-11-18 10:16 · Visor lateral y conversación sin indicadores internos

- **Petición**: ocultar el visor a lo ancho y ampliar el chat, retirar progreso/ejercicios trabajados y mejorar la barra lateral con las skills frontend-design y frontend-ui-engineering.
- **Diseño aplicado**: verde profundo `#173e30`, selección `#315e47`, acento claro `#e4f0d9`, texto `#f3f8f4` y ayuda `#c2d6c9`. Tipografía existente, cabecera de cuaderno, acción nueva conversación, historial con actualización junto al título y navegación al pie. En móvil se compacta la barra y se apila el visor.
- **Implementado**: botón nativo Ocultar/Mostrar visor con aria-expanded y aria-controls; al cerrar desaparece la columna del visor y el chat ocupa todo su ancho. Al abrir se conserva la geometría. Progreso, ejercicios trabajados y diagnóstico retirados del DOM y de las burbujas; estado matemático y selección automática permanecen internos. Eliminados controles y consultas de progreso innecesarios en el cliente. Sin cambios en entidades ni API Java.
- **Evidencia cliente**: 91 Node, V8 98,97 % líneas / 91,82 % ramas. Contrato Chromium comprueba ancho cero del visor, crecimiento del chat superior a 200 px en escritorio, recuperación, ausencia de indicadores y responsive 320–1440 px. WebGL2 correcto; capturas de escritorio abierto/cerrado y móvil revisadas.
- **Verificación final**: 991 Java correctas, `check jacocoAggregateReport` con salida 0, JaCoCo en umbral en ocho módulos. Pendiente CI remota e integración en DEV; corrección 4/5, total 38/40, estado en curso.

### 2026-11-18 10:18 · Altura común del tutor

- **Petición**: barra lateral y visor con el mismo alto que el chat.
- **Implementado**: las tres columnas se estiran a la altura de su fila en escritorio (>900 px); retirados el alineado superior y el tope de 48 rem del visor en esa vista. El historial mantiene la altura del chat con el visor oculto. En móvil se conserva la disposición apilada. Sin cambios de dominio, clases ni API.
- **Pruebas**: contrato Chromium ampliado con 6 comprobaciones de caja negra a 1024/1440 px: igualdad de barra/chat, visor/chat y barra/chat con visor oculto. Responsive 320–1440 px y WebGL2 correctos; captura revisada. Sin nuevas pruebas unitarias para este ajuste CSS.
- **Verificación**: 991 Java y 91 Node correctas; `check jacocoAggregateReport` con salida 0 y cobertura JaCoCo/V8 en umbral (V8 98,97 % líneas / 91,82 % ramas). Pendiente CI remota e integración en DEV. Checklist 38/40; corrección 4/5.
