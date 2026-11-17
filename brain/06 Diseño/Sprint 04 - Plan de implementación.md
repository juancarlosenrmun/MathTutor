# Sprint 04 · Plan de implementación

Objetivo: implementar los cinco UC de [[Sprint 04]] conforme a [[Sprint 04 - Diseño de implementación]], aprobado mediante «Implementa sprint».

Arquitectura: puertos en common, orquestación en tutor, adaptadores transaccionales cortos en persistence y API autenticada en application. Spring Boot 3.4, Java 21, PostgreSQL y Symja existentes.

Restricciones: una llamada LLM por turno, sin transacción durante streaming, español, propiedad desde autenticación, sin soluciones de referencia en API. Código en sprint-4 y documentación en DOC; frontend después de fusión.

## Pasos

- [ ] UC-35: pruebas de gramática y límites en GraphicSpecServiceImplTest; implementar validador con CAS y esquema JSON versionado. Verificar módulo graphics.
- [ ] UC-20/25: pruebas del puerto ConversationStore y adaptador PostgreSQL; migrar turnos para sesión, métricas y estados terminales. Reservar por UUID bajo bloqueo corto de sesión; historial acotado a diez turnos/16000 caracteres. Verificar persistencia e idempotencia.
- [ ] UC-17: pruebas de solución/paso, error de signo e indeterminación; seleccionar ejercicio explícito por módulo y guardar progreso verificado. No implementar reconocimiento automático ni política de S5.
- [ ] UC-11: API paginada de sesiones, ejercicios y turnos propios con DTO; pruebas de aislamiento y límites.
- [ ] Integración: conectar contexto, diagnóstico y registro al streaming; emitir especificaciones validadas; probar errores, cancelación, reintentos y ausencia de transacción ante el LLM.
- [ ] Ejecutar check jacocoAggregateReport, revisar diff completo y revisión independiente final; corregir defectos y abrir PR a DEV. Mantener integración sin marcar hasta fusión.

Cada incremento empieza con una prueba en rojo, termina en verde y tiene commit tipo(UC-NN). Las notas de iteración registran decisiones y evidencia. Verificación local y CI son evidencias distintas.

## Focos de revisión

Reservas tras reinicio o cancelación; repetición de un paso ya verificado; metadatos sin consumo real; expresiones admitidas por CAS pero inseguras para cliente; respuestas truncadas o fallos de persistencia sin DONE.
