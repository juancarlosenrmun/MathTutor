---
sprint: S1
estado: completado
---
# Plan de cierre del Sprint 01

Petición: completar [[Sprint 01]], conservar los cambios previos, verificar cobertura y preparar CI.

## Diseño y límites

- UC-03: implementar `AccessPolicy` con denegación por defecto. Alumno: módulos, ejercicios y sesiones propias; profesor: contenido y estadísticas agregadas; administrador: gestión. La comprobación de una sesión privada exige propiedad incluso al administrador: las consultas administrativas individuales y su auditoría pertenecen a UC-09. El JWT ya consulta el usuario activo en cada petición.
- UC-27: cliente estático servido por Spring Boot, WebGL2/GLSL propio sin Three.js, cámara orbital, zoom y desplazamiento, controles por teclado y ratón, recuperación de contexto y errores en español. Escena de ejes; gráficas y superficies quedan para sus sprints.
- Pruebas: tabla de decisión de permisos, contratos HTTP reales con PostgreSQL, cámara/pipeline/visor con Node y cobertura nativa V8. Umbrales 80 % líneas y 70 % ramas; tutor 90 %.
- Mantener `sprint-1`, que contiene trabajo previo sin confirmar y no coincide con DEV; no trasladar ni fusionar cambios anteriores automáticamente. Integración en DEV condicionada a CI verde.

## Pasos

1. Ejecutar baseline `gradlew clean check jacocoAggregateReport`.
2. Añadir pruebas de permisos, propiedad, rol actualizado y desactivación; comprobar rojo, implementar política y contrato `/api/access/sessions/{id}` (204 permitido, 403 denegado).
3. Añadir pruebas Node de cámara, shaders, errores y ciclo de vida; comprobar rojo, implementar cliente en recursos estáticos.
4. Conectar pruebas JS y cobertura a CI; depurar fallos sin reducir umbrales ni ocultar integraciones.
5. Revisar diff y `.gitignore`, verificar build e informes y actualizar [[Sprint 01]], [[Progreso global]] y [[Registro de iteraciones]] con evidencia.

## Revisión

Revisar especialmente accesos cruzados, registro con privilegios, revocación sin renovar JWT, shaders fallidos, pérdida de contexto y recursos GPU liberados.

## Resultado

Ejecutado y verificado el 2026-11-16 19:15. Los cinco elementos de [[Sprint 01]] están integrados en DEV con CI verde. La reconciliación de historia se realizó en un worktree separado, conservando las fuentes verificadas. Diseño y auditoría publicados en DOC.
