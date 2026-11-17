---
sprint: S4
estado: aprobado
fecha: 2026-11-17
---
# Sprint 04 · Diseño de implementación

Alcance: [[Sprint 04]], [[Requisitos funcionales]] RF-05, RF-10, RF-20, RF-23 y RF-26; [[Requisitos no funcionales]] RNF-13. Implementar UC-11, UC-17, UC-20, UC-25 y UC-35, respetando [[Restricciones y alcance]]. Aprobado mediante la petición «Implementa sprint». El diseño no acredita implementación ni integración.

## Situación actual

- `TutorConversation` valida la propiedad y actividad de la sesión, interpreta expresiones y recupera apuntes, pero construye cada prompt sin historial y no registra turnos.
- `SessionContextService` e `InteractionLogger` son contratos pendientes de implementación. `Turn` JPA requiere una ejecución de ejercicio y aún no refleja todos los campos del DTO.
- `SymjaCasEngine.verify` compara equivalencia, pero clasifica las diferencias como OTRO. El resultado indeterminado no debe convertirse en un error matemático confirmado.
- `GraphicSpecServiceImpl` construye el DTO sin validación; el visor funciona con entradas manuales.

## Enfoque propuesto

Extender los módulos y puertos existentes con persistencia PostgreSQL y transacciones cortas. El contexto se obtiene del historial persistido; no se mantiene una segunda fuente de verdad en memoria. Una alternativa en memoria pierde las conversaciones tras reiniciar y no satisface UC-11. Una alternativa basada en resúmenes generados por IA añade llamadas y contradice la restricción del tutor.

La elección técnica es una propuesta de Codex. Se implementará por UC, primero pruebas, después código y verificación. Código y seguimiento en `sprint-4`, documentación de diseño en `DOC`.

## UC-20 y UC-25 · Contexto y registro

- Cargar estado del ejercicio e historial después de comprobar propietario y sesión activa.
- Incluir los últimos diez turnos completos, con límite adicional de 16000 caracteres. Los límites se aplican al prompt; el historial completo permanece persistido y paginado.
- Tratar mensajes e historial como datos del alumno, separados de las instrucciones del sistema. Evitar incorporar soluciones de referencia a la respuesta o al contexto visible.
- Conservar módulo fijo, tema, estado y progreso del ejercicio. Una conversación teórica puede tener turnos sin ejercicio; la migración permitirá esa asociación opcional y añadirá la asociación directa a sesión.
- Registrar por turno mensaje, respuesta, módulo y tema, diagnóstico, nivel de ayuda, latencia y métricas reales de tokens cuando el proveedor las ofrezca. Si no las ofrece, registrar ausencia de dato; no presentar estimaciones como consumo real.
- Registrar también rechazo determinista, error del proveedor y cancelación con estado explícito; nunca tratarlos como respuesta completa. No volcar el contenido personal del chat al log general.
- La escritura final debe ser atómica y actualizar la actividad de sesión. Ninguna transacción permanece abierta durante el streaming.
- Serializar los turnos de una misma sesión o rechazar concurrencia con conflicto; liberar la reserva al terminar, fallar o cancelar. Definir idempotencia por identificador de petición para evitar duplicados en reintentos del cliente.

## UC-17 · Verificación CAS

- Referencia seleccionada por el servidor desde el estado del ejercicio. El cliente nunca puede declarar una solución como referencia fiable.
- En ejercicios procedimentales del banco, verificar el siguiente paso canónico y la solución final; actualizar el progreso solo con pasos verificados y sin contar dos veces un mismo paso.
- Fuera del banco, calcular referencia únicamente para operaciones admitidas y explícitas. Las preguntas teóricas y expresiones sin objetivo verificable conservan diagnóstico no aplicable o indeterminado.
- Clasificar signos y errores de reglas solo con evidencia determinista. Conservar OTRO si no puede justificarse una categoría; no deducir equivalencia por una única muestra numérica.
- Entrada inválida pide reformulación. Timeout o indeterminación del CAS no certifica corrección ni error. No enviar al alumno la solución de referencia dentro de una pista diagnóstica.
- No implementar reconocimiento automático del banco (UC-23, S6) ni política socrática/antirrevelación completa (S5).

## UC-11 · Progreso e historial

- API de alumno autenticado para listar sus sesiones, temas y ejercicios completados; detalle paginado del historial propio.
- Propietario obtenido exclusivamente de la autenticación. Un identificador de sesión ajeno devuelve la misma respuesta que uno inexistente.
- Paginación con orden estable por fecha e identificador; tamaño máximo 100. Historial vacío devuelve colección vacía.
- Consultas/proyecciones acotadas, sin exponer entidades JPA ni soluciones de referencia. Pruebas de reinicio y aislamiento entre usuarios.

## UC-35 · Especificación gráfica

- Backend construye un `GraphicSpec` a partir de una operación matemática explícita y admitida, sin una segunda llamada al LLM.
- Validar JSON por esquema versionado: tipo permitido, función obligatoria y acotada, dominio finito y ordenado, resolución y cantidad de puntos limitadas, coordenadas finitas.
- Admitir solo curvas y superficies ya soportadas por el visor; tangentes quedan para S5. Usar una gramática restringida de funciones y variables y validación CAS; aceptar una expresión en CAS por sí solo no autoriza ejecutarla en el cliente.
- Añadir evento SSE de especificación validada conservando TOKEN, DONE y ERROR. Una petición gráfica inválida devuelve un error controlado; no emitir especificación parcial ni código ejecutable.
- Pruebas de contrato y esquema, funciones permitidas, variables incompatibles, dominios invertidos, NaN/infinito, límites y entradas que intenten ejecutar código.

## Verificación y entrega

### Decisiones técnicas para la implementación

- Banco: selección explícita mediante `POST /api/tutor/sessions/{id}/exercises` con `exerciseId`; comprobar que pertenece al módulo de la sesión. No reconocer enunciados automáticamente. Operaciones libres iniciales: derivada respecto a x y simplificación de una expresión explícita; el resto queda indeterminado.
- Gráficas: `POST /api/tutor/sessions/{id}/graphics` recibe tipo, función y dominio; curva usa x y superficie usa x e y con el mismo intervalo para ambos ejes, acorde al DTO existente. Admitir números, paréntesis, x/y, suma, resta, multiplicación, división, potencia y sin/cos/tan/exp/log/sqrt/abs; rechazar asignaciones, comandos y variables adicionales. Función de 1–200 caracteres, dominio entre -10000 y 10000, mínimo menor que máximo, resolución de 2–200 pasos y hasta 100 puntos. Los puntos no admiten coordenadas no finitas.
- Concurrencia: rechazo HTTP 409 si existe un turno activo en la sesión, con reserva persistida y expiración controlada. Cada envío lleva `requestId` UUID; unicidad por sesión y UUID. El reintento completado recupera el resultado sin nueva llamada; en curso devuelve 409; fallido o cancelado devuelve su estado terminal y exige un UUID nuevo para un nuevo intento.

- Pruebas unitarias deterministas con CAS y LLM simulados; pruebas de API con roles, propiedad y CSRF; integración con Testcontainers PostgreSQL + pgvector y migraciones reales.
- Caja blanca y negra por UC: condiciones, caminos, particiones, límites y tablas de decisión. Cubrir historial acotado, concurrencia, cancelación, fallo de escritura y falta de métricas de tokens.
- Ejecutar build, JaCoCo y comprobaciones configuradas. Umbrales: al menos 80 % de líneas y 70 % de ramas por módulo; política pedagógica al menos 90 %. No ampliar exclusiones para ocultar lógica sin probar.
- Actualizar [[Diagrama de dominio]] y [[Diagrama de clases]] en DOC cuando exista implementación; checklist y seguimiento en la rama del código con evidencia real.
- Abrir PR de `sprint-4` a `DEV` tras implementación y revisión. La petición autoriza abrir la PR, pero no solicita expresamente fusionarla; esperar a que esté fusionada antes de la fase siguiente.

## Fase posterior · Frontend

Desde `DEV` actualizado y solo después de la fusión del backend, crear `Sprint-4-Frontend` (Git no permite espacios en nombres de ramas). Mostrar progreso e historial, recuperar conversación persistida, reflejar diagnóstico y estado, e interpretar especificaciones validadas con el visor existente. Buscar e instalar skills de frontend individualmente según necesidad. No implementar estadísticas docentes de S8.

La fase tendrá sus pruebas JS, cobertura equivalente y comprobación de navegador; su entrega no puede considerarse completada con la PR del backend.

## Decisiones durante la implementación

- Se conserva la tabla legada `turns`, ligada a ejecuciones, y se añade `conversation_turns` mediante V9. El nuevo registro admite conversaciones sin ejercicio y reserva UUID sin reinterpretar eventos legados. `ConversationStore` reúne operaciones que deben ser atómicas; sustituye en este flujo a los puertos incompletos `SessionContextService` e `InteractionLogger`, cuyas firmas no identifican propietario/sesión al registrar.
- El endpoint de mensajes admite UUID opcional para compatibilidad con el frontend anterior. La idempotencia entre peticiones se garantiza cuando el cliente reutiliza su UUID; al omitirlo el servidor genera uno nuevo. La fase frontend deberá enviarlo.
- Las métricas de proveedor se transportan por una sobrecarga de `LlmGateway.stream`, manteniendo el contrato de texto anterior. Ausencia de métricas se persiste como null.
- Progreso e historial se ofrecen paginados por sesión; temas acompañan los turnos y ejercicios. Los DTO públicos de ejercicio omiten referencia y pasos canónicos.
