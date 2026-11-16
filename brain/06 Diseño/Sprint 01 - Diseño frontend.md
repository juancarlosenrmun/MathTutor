---
sprint: S1
estado: implementado en rama
fecha: 2026-11-16
---
# Sprint 01 · Diseño frontend

Relacionado: [[Sprint 01]], [[Casos de uso#UC-01]], [[Casos de uso#UC-03]], [[Casos de uso#UC-27]], [[Diagrama de clases]], [[Diagrama de dominio]].

## Objetivo y alcance

Frontend minimalista y accesible para registro, sesión, permisos y visor inicial. EN-01 y EN-04 permanecen como infraestructura y spike técnico, sin pantallas de usuario artificiales. No se incorporan chat, recuperación de contraseña, gráficas, estadísticas ni administración de otros sprints.

## Decisiones

Se conserva HTML/CSS y módulos JavaScript nativos en `/visor/index.html`. Rutas hash y secciones semánticas comparten una instancia de `SessionClient` y conservan el visor. Un framework añadiría dependencias innecesarias para seis vistas; páginas independientes obligarían a renovar la cookie en cada navegación. La ruta de cada vista permite enlaces directos y navegación atrás/adelante.

| Ruta | Vista | Comportamiento |
|---|---|---|
| `#/inicio` | Inicio | Presentación y acceso al laboratorio o al registro |
| `#/acceso` | Iniciar sesión | Correo y contraseña, mostrar/ocultar, estado ocupado y error |
| `#/registro` | Crear cuenta | Alumno, confirmación y requisitos de contraseña del servidor |
| `#/laboratorio` | Laboratorio | WebGL2 real, ejes, controles de cámara y ayuda |
| `#/cuenta` | Cuenta | Correo, rol vigente, permisos y cierre; sin sesión muestra acceso |
| `#/restringido` | Acceso restringido | Explica el 403 y ofrece laboratorio o acceso |

La autorización real y la propiedad siguen verificándose en el servidor. La vista de cuenta no concede privilegios. El alta pública siempre envía `ALUMNO`; las cuentas privilegiadas las gestiona el centro.

## Sistema visual

- Papel `#f6f7f2`, superficie blanca, tinta `#202a25`, verde `#285b43`, texto secundario `#626e65` y separadores `#dce2d9`.
- Georgia para títulos y Segoe UI/Arial para interfaz; fuentes locales, sin peticiones a terceros.
- Ilustración SVG de coordenadas en inicio y visor WebGL existente en laboratorio. Espacio generoso, alineación izquierda, botones discretos y ausencia de animaciones automáticas.
- Escritorio: contenido central, portada en dos columnas y laboratorio con ayuda lateral. Móvil: columna única, navegación visible y controles táctiles.
- Formularios con etiquetas, autocompletado, errores enfocados, contraseña limpiada al cambiar de vista y enlaces reales. Enlace de salto al contenido que conserva la ruta, foco visible y reducción de movimiento.

## Sesión y concurrencia

El token de acceso permanece en memoria. La cookie HttpOnly se utiliza para restaurar y renovar mediante CSRF; no se envían tokens por URL ni almacenamiento web. `Web Locks` serializa mutaciones de la cookie entre pestañas en navegadores modernos y contexto seguro. Existe degradación sin locks para entornos antiguos; la coordinación entre pestañas requiere esta API.

Rol consultado cada 3 segundos con timeout de 2 segundos. Un 401 intenta una única renovación; un 403 muestra acceso restringido; errores transitorios conservan la sesión. Generaciones de consulta y actividad de interfaz impiden que respuestas anteriores sobrescriban login/logout. Un cierre sin confirmación del servidor borra el acceso local y ofrece reintentar la revocación.

## Plan ejecutado y verificación

1. Pruebas fallidas de registro, refresh y logout; implementación del cliente y commit.
2. Vistas y CSS adaptable; commit visual.
3. Navegación, formularios y permisos con pruebas; commit funcional.
4. Revisión independiente; regresiones de concurrencia, cierre y salto al contenido; correcciones verificadas.
5. Build Gradle, V8, JaCoCo y Chromium; seguimiento en la rama de implementación. CI remota e integración en DEV pendientes.

Comandos: `./gradlew.bat check jacocoAggregateReport --no-daemon --no-scan`, `python scripts/smoke-webgl.py`, `python scripts/test_verify_integration_results.py`, `python scripts/verify-integration-results.py`. El smoke usa CDP y WebSocket nativos de Node para probar Web Locks con reloj real; no añade dependencias de navegador. Comprueba vistas a 1200 y 375 px, registro, login, permisos, cierre, cámara y exclusión de renovaciones. Genera seis capturas en `mathtutor-graphics/build/reports/client-browser/`.

Skills instaladas y aplicadas: [frontend-design](https://skills.sh/anthropics/skills/frontend-design) y [web-design-guidelines](https://skills.sh/vercel-labs/agent-skills/web-design-guidelines). Se aplica además el flujo local de TDD, accesibilidad, revisión y documentación. No se realizan llamadas a proveedores LLM.
