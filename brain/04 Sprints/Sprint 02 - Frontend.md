---
sprint: S2
estado: en curso
---
# Sprint 02 · Integración frontend

Petición: incorporar al frontend las capacidades implementadas en [[Sprint 02]], en `Sprint-2-Frontend` creada desde DEV actualizada. Git no admite espacios en nombres de rama.

## Diseño y alcance

Se amplía el cliente nativo existente, manteniendo tipografía, verde y superficies claras. El alumno entra al tutor desde navegación o espacio personal. La vista combina una columna de selección de bloque con la conversación y su formulario. En móvil las columnas se apilan. El laboratorio presenta primero función e intervalo, con derivada e integral aproximadas y salida textual.

Se reutilizan los contratos de UC-12/13 y el cálculo WebGL2 de UC-28. UC-14 sigue siendo una operación del arranque: no se inventa un panel de apuntes ni se anticipa la recuperación de S3. No se añaden dependencias ni entidades.

Alternativas: una página separada duplicaría sesión y navegación; sustituir el cliente por un framework ampliaría innecesariamente el alcance. Se elige extender las vistas existentes por continuidad con S1 y con los adaptadores de S2.

## Plan de implementación y validación

1. Reproducir con pruebas los estados de catálogo pendiente, borrador privado y alta tardía al salir del chat.
2. Bloquear selección hasta disponer del catálogo, limpiar contenido al cambiar identidad y descartar operaciones canceladas al navegar.
3. Integrar tutor y laboratorio en navegación, portada y espacio del alumno; ajustar estructura y estilos responsive.
4. Ejecutar suite Node con cobertura V8, contrato Chromium de escritorio/móvil y build con JaCoCo. Revisar diff y capturas.
5. Registrar evidencia en [[Sprint 02]], [[Progreso global]] y [[Registro de iteraciones]]. La integración en DEV y CI remota quedan pendientes hasta ejecutarse.

## Invariantes de interfaz

- El bloque pertenece a la sesión creada; seleccionar otro no cambia una conversación existente.
- Los mensajes se insertan como texto, nunca como HTML remoto; SSE usa un POST sin reenvío automático.
- La pérdida de identidad borra historial y borrador. Salir durante una operación invalida su resultado tardío.
- La actualización de permisos conserva la autorización del servidor y oculta los enlaces de alumno cuando corresponde.
