---
sprint: S8
objetivo: Should restantes y administración
puntos: 32
estado: en curso
---
# Sprint 08 · Should restantes y administración

Puntos: 32 · Anterior: [[Sprint 07]] · Siguiente: [[Sprint 09]]

## Justificación
UC-05 y EN-07 deben estar terminados antes de abrir a alumnado real. UC-10 usa datos de UC-23 y UC-25.

## Elementos
| ID | Elemento | Req. | Pts | MoSCoW |
|---|---|---|---|---|
| UC-05 | Exportar y suprimir mis datos | RF-27 | 5 | Should |
| UC-06 | Desactivar o anonimizar un perfil | RF-25 | 5 | Should |
| UC-07 | Cambiar el rol de un usuario | RF-25, RNF-04 | 3 | Could |
| UC-08 | Iniciar el restablecimiento de contraseña de un usuario | RF-25 | 3 | Could |
| UC-10 | Consultar estadística de ejercicios comenzados, completados y abandonados | RF-24 | 8 | Should |
| EN-07 | Accesibilidad, teclado, alternativa textual y navegadores | RNF-10, 12 | 8 | Should |

## Checklist de implementación

### UC-05 · Exportar y suprimir mis datos
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [ ] Integrado en la rama principal

### UC-06 · Desactivar o anonimizar un perfil
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [ ] Integrado en la rama principal

### UC-07 · Cambiar el rol de un usuario
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [ ] Integrado en la rama principal

### UC-08 · Iniciar el restablecimiento de contraseña de un usuario
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [ ] Integrado en la rama principal

### UC-10 · Consultar estadística de ejercicios comenzados, completados y abandonados
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [ ] Integrado en la rama principal

### EN-07 · Accesibilidad, teclado, alternativa textual y navegadores
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [ ] Integrado en la rama principal

## Definición de hecho del sprint
- [ ] Todos los elementos completos
- [ ] Build y CI en verde
- [x] Cobertura en umbral verificada
- [ ] Velocidad real medida y anotada en [[Registro de iteraciones]]
- [x] [[Progreso global]] actualizado

## Notas de iteración

- UC-05 usa una descarga JSON de la cuenta autenticada y suprime los registros propios en PostgreSQL. Para suprimir la cuenta del último administrador debe existir otro administrador activo.
- UC-06 y UC-07 vuelven a validar los permisos del operador bajo bloqueo. UC-08 reutiliza la cola SMTP y necesita la configuración de correo del despliegue. UC-10 cuenta los ejercicios iniciados y separa activos, completados y abandonados por módulo y tema.
- EN-07 incorpora una alternativa que funciona sin WebGL, selección con teclado y navegación a 390 px. El historial conserva las respuestas para su lectura y una región de estado anuncia cada respuesta completa. NVDA 2026.3.1 con Chrome en Windows leyó «Tu pregunta» y la respuesta de prueba tras terminar el streaming.
- RNF-12 se verificó con las dos últimas versiones estables de escritorio a fecha de la prueba. Chrome 158.0.8127.36 y 157.0.8103.78 en Windows, Firefox 160.0.1 y 159.0.1 en Windows, Edge 158.0.4370.33 en Windows y 157.0.4341.60 en Linux, y Safari 27.2 y 27.0.1 en macOS. Compilaron los shaders y se comprobaron los píxeles de curvas, superficies y tangentes. Safari 27.1 se publicó para iOS. [Notas de Apple](https://developer.apple.com/documentation/safari-release-notes/safari-27_1-release-notes.md) · [Ejecución de Safari nativo](https://github.com/juancarlosenrmun/MathTutor/actions/runs/45261297715)
- Los recorridos de accesibilidad se reproducen con `node scripts/sprint8-accessibility.mjs`, Playwright y axe-core. `MATHTUTOR_BROWSER_DEPS` admite una carpeta externa de dependencias. Las pruebas de WebGL usan `scripts/smoke-webgl.py` con `CHROME_BIN` para Chrome y Edge, o `--native firefox` y `--native safari` con Selenium.
- Se ajustaron tres contratos SSE del Sprint 04 para usar HTTP real. Era necesario para evitar una carrera de MockMvc durante la comprobación completa del sprint.
- Checklist de implementación **24/30**. La [PR #24](https://github.com/juancarlosenrmun/MathTutor/pull/24) sigue pendiente de integración en `DEV`. Véase [[Registro de iteraciones]].
