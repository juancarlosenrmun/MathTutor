# Sprint 02 · Diseño y plan de implementación

**Objetivo:** entregar [[Sprint 02]]: UC-12, UC-13, UC-14 y UC-28, conforme a [[Requisitos funcionales]] RF-01/02/03/14.

**Arquitectura:** conservar el monolito y los puertos de common. Sesiones persistentes con módulo inmutable y propietario; catálogo de tres bloques. Orquestador sin transacción y una sola suscripción al gateway por mensaje. POST con SSE permite usar Authorization y CSRF sin credenciales en URL. Indexación de recursos del classpath al arrancar: contrato LaTeX validado, versión SHA-256, sustitución idempotente y embeddings fuera de la transacción de escritura. Gráficas mediante parser aritmético limitado, muestreo y pipeline WebGL2 existente; sin eval.

**Decisiones de implementación:** ampliar los componentes actuales evita introducir otro servidor, autenticación o motor gráfico. Se descarta subir apuntes desde la interfaz porque están versionados en código. RAG por consulta, CAS conversacional, memoria de diálogo y política socrática pertenecen a sprints posteriores.

**Restricciones:** español, claves por entorno, ninguna transacción durante LLM, umbral por módulo 80 % líneas/70 % ramas; política pedagógica 90 %. Documentación y diagramas en DOC; seguimiento en Sprint-2. Comentarios públicos compatibles con Doxygen/Javadoc.

## Plan

Para ejecución directa en esta sesión, con test-driven-development, springboot-tdd, incremental-implementation y verification-before-completion.

- [x] UC-12: pruebas de catálogo, módulo inválido, propiedad y sesión nueva; puerto SessionStore, servicio, adaptador JPA y API; migración del catálogo. Ejecutar unitarias y contrato PostgreSQL; commit independiente.
- [x] UC-13: pruebas de deltas, error, cancelación, mensaje vacío/límite y única llamada; orquestador y POST SSE, cliente accesible integrado con autenticación. Verificar ausencia de transacción durante la suscripción; commit independiente.
- [x] UC-14: pruebas de metadatos, secciones, comentarios LaTeX, fórmulas, errores, idempotencia y actualización; parser y arranque del indexador con recursos de ejemplo; PostgreSQL/pgvector. Commit independiente.
- [x] UC-28: pruebas numéricas de función, derivada y área, discontinuidades, límites e inyección; controles y geometría WebGL2. Node/V8 y navegador; commit independiente.
- [x] Ejecutar check, JaCoCo agregado, puerta de integración y revisión completa. Registrar cifras y exclusiones; actualizar [[Diagrama de dominio]] y [[Diagrama de clases]] en DOC y seguimiento en Sprint-2. No marcar integración ni CI sin evidencia remota.

## Revisión de riesgos

- Un identificador ajeno recibe el mismo error que uno inexistente.
- Una reconexión no reenvía mensajes ni multiplica llamadas al proveedor.
- La cancelación del lector cancela el flujo, y los errores no exponen mensajes del proveedor.
- Un recurso LaTeX inválido falla antes de modificar el índice; los embeddings no retienen conexiones JDBC.
- Las funciones discontinuas no generan segmentos falsos; dominios y cantidades de muestras están acotados.
