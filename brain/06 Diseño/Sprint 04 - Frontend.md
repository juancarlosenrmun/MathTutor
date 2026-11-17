---
sprint: S4
estado: en curso
---
# Sprint 04 · Frontend

Petición autorizada: fusionar [[Sprint 04]] y ampliar su frontend desde DEV. Base integrada mediante PR #16, merge 1276674; rama `Sprint-4-Frontend` desde DEV 713c89b.

## Diseño y alcance

Mantener la interfaz actual: Segoe UI/Arial para controles y Georgia en titulares; paleta existente papel #f6f7f2, superficie #ffffff, tinta #202a25, verde #285b43 y bordes #dce2d9. Columna izquierda para historial paginado, conversación central y visor a la derecha. En móvil se apilan sin desbordamiento. Controles adicionales en detalles desplegables para no desplazar el chat.

```text
Tu historial       Conversación / diagnóstico       Visor
Nueva conversación Pregunta y respuesta              Tipo y función
Sesiones guardadas Ejercicios y progreso             Dibujar
Cargar más         Compositor / recuperar envío      Alternativa textual
```

- UC-11/20: recuperar sesiones y turnos guardados, continuar conversaciones activas, paginar sin borrar el contexto ni confundir respuestas tardías de otra identidad. Las sesiones cerradas admiten consulta, no escritura.
- UC-17: seleccionar ejercicio del banco o derivada/simplificación libre; mostrar estado, progreso y diagnóstico conservador. Añadir catálogo paginado por módulo autenticado como soporte necesario de selección, sin exponer referencias ni adelantar reconocimiento de S6.
- UC-25: enviar UUID por turno y reutilizarlo solo al recuperar un envío incierto. No reintentar automáticamente. Mostrar estados de error/cancelación y borrar datos privados al salir de la cuenta.
- UC-35: solicitar y leer especificaciones validadas del backend; comprobarlas también en cliente y usar los parsers existentes sin eval ni HTML remoto. Respetar los límites reales del visor y explicar cualquier dominio no representable.

## Plan incremental

1. Prueba de contrato del catálogo seguro, puerto/adaptador/API y verificación de integración.
2. Pruebas de petición autenticada, SSE y recuperación de historial; componentes pequeños para transporte, historial y gráficas.
3. Pruebas de selección/progreso y enlace del visor; integrar paneles con el tutor y estado de identidad/sesión.
4. Pruebas de errores, límites, reintentos UUID, respuestas tardías y roles; comprobar teclado, móvil, lector de pantalla y capturas en navegador real.
5. Ejecutar check y JaCoCo/V8 en umbral, revisión independiente final y actualizar seguimiento. Mantener la integración frontend sin marcar hasta su fusión.

Conservar límites del backend; usar curva con al menos 16 muestras y superficies dentro de límites del generador existente (128 divisiones, dominio ±100 y 150 caracteres), con error claro para especificaciones fuera de ese rango. No sustituir la gramática del visor por ejecución de código.

## Implementación y revisión

Frontend distribuido en `tutor-api.mjs`, `tutor-history.mjs`, `tutor-sessions.mjs`, `study.mjs` y `graphic-spec.mjs`, coordinados desde `mountTutor`. El visor compartido se limpia al retirar contenido privado. Las conversaciones cerradas son de solo lectura.

Se añaden pruebas de contrato para paginación, selección explícita, diagnóstico, recuperación UUID, aislamiento de identidad y límite de las especificaciones. El navegador real verifica herramientas abiertas a 320–1440 px y genera capturas `tutor-s4.png` y `tutor-s4-movil.png`.

Revisión independiente: dos hallazgos corregidos con pruebas observadas en rojo y después verde. Se invalida el panel anterior antes de recuperar otro historial; si falla la apertura, no queda una conversación parcialmente activa. La paginación crea un controlador nuevo tras cancelación. También se diferencia un rechazo previo al turno de un envío incierto, evitando instrucciones de recuperación inaplicables.

La base backend está integrada en DEV (30/30). La integración de la ampliación frontend se registra por separado y permanece pendiente hasta su fusión.
