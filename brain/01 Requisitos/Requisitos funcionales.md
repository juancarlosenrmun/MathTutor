# Requisitos funcionales

Prioridad: M=Must, S=Should, C=Could.

| ID | Requisito | Descripción | Prior. | Casos de uso |
|---|---|---|---|---|
| **RF-01** | Chat con respuesta en streaming | Interfaz conversacional de texto con respuesta progresiva (SSE). | M | [[Casos de uso#UC-13|UC-13]] |
| **RF-02** | Selección de módulo previa | El alumno elige bloque (funciones; derivadas e integrales; métodos 3D) antes del chat. Módulo fijo por sesión; cambiarlo abre sesión nueva. Todos acceden a todos los módulos. | M | [[Casos de uso#UC-12|UC-12]] |
| **RF-03** | Respuesta fundamentada en apuntes | RAG filtrado por módulo y tema sobre apuntes LaTeX embebidos en el código e indexados al arrancar. Sin fragmento sobre el umbral: mensaje fuera de temario sin llamar al LLM. | M | [[Casos de uso#UC-14|UC-14]], [[Casos de uso#UC-15|UC-15]] |
| **RF-04** | Interpretación de expresiones | Parser determinista convierte el texto del alumno en expresión simbólica. Si no es interpretable, pide reformular. | M | [[Casos de uso#UC-16|UC-16]] |
| **RF-05** | Verificación simbólica | CAS compara pasos y resultado con la solución de referencia (banco o calculada) y clasifica el error. Si es del banco y procedimental, usa pasos canónicos. | M | [[Casos de uso#UC-17|UC-17]] |
| **RF-06** | Banco de ejercicios orientativo | Ejercicios por módulo (solución, tema, tipo, dificultad; en procedimentales pasos canónicos y errores típicos con pista semilla). Solo orientativo; el tutor puede formular preguntas propias. Se priorizan las teóricas. | M | [[Casos de uso#UC-22|UC-22]] |
| **RF-07** | Tutorización socrática progresiva | Cuatro niveles de ayuda (conceptual, procedimental, ejemplo análogo, revelación) por política pedagógica determinista según intentos y diagnóstico. | M | [[Casos de uso#UC-18|UC-18]] |
| **RF-08** | No revelación de la solución | Control de salida: contra solución de referencia si existe; si no, contra fragmentos de apuntes. Si hay fuga, regenera o degrada a pista genérica. | M | [[Casos de uso#UC-19|UC-19]] |
| **RF-09** | Progresión pedagógica | Secuencia procedimental → conceptual → interpretación dentro de cada módulo. | C | [[Casos de uso#UC-24|UC-24]] |
| **RF-10** | Contexto de sesión | Estado del ejercicio e historial acotado o resumido por conversación. | M | [[Casos de uso#UC-20|UC-20]] |
| **RF-11** | Controles de entrada y cuota | Bloqueo de mensajes fuera de temario, intentos de extraer la solución o inyección; cuota por usuario. | S | [[Casos de uso#UC-21|UC-21]] |
| **RF-12** | Reconocimiento de ejercicios del banco | Identifica ejercicio del banco por id o por enunciado equivalente normalizado con CAS (nunca LLM ni similitud semántica). Si coincide usa solución, tema, pasos canónicos y pistas semilla; si no, flujo libre. | S | [[Casos de uso#UC-23|UC-23]] |
| **RF-13** | Pipeline WebGL/GLSL y cámara | Pipeline propio con shaders de vértices y fragmentos y cámara virtual (órbita, zoom, desplazamiento). | M | [[Casos de uso#UC-27|UC-27]] |
| **RF-14** | Gráficas 2D | f(x), f'(x) y área bajo la curva en integrales definidas. | M | [[Casos de uso#UC-28|UC-28]] |
| **RF-15** | Superficies | Mallado y triangulación de z=f(x,y) con normales, iluminación y texturas; refinamiento con estructura espacial. | M | [[Casos de uso#UC-29|UC-29]], [[Casos de uso#UC-30|UC-30]] |
| **RF-16** | Rectas y planos tangentes | Cálculo y dibujo dinámico de rectas y planos tangentes. | M | [[Casos de uso#UC-31|UC-31]] |
| **RF-17** | Intersección y selección | Intersecciones rayo–superficie y recta–plano para seleccionar puntos. | S | [[Casos de uso#UC-32|UC-32]] |
| **RF-18** | Animaciones | Interpolación y transformaciones (secante a tangente) y trayectorias de cámara. | C | [[Casos de uso#UC-33|UC-33]] |
| **RF-19** | Post-procesamiento | Render a textura, efectos y superposición de capas. | C | [[Casos de uso#UC-34|UC-34]] |
| **RF-20** | Especificación gráfica | Backend emite JSON validado por esquema (tipo, función, dominio). El cliente solo interpreta; expresiones validadas con CAS antes de evaluarse. | M | [[Casos de uso#UC-35|UC-35]] |
| **RF-21** | Registro y sesión | Registro, inicio y cierre de sesión seguros; restablecimiento de contraseña por enlace de un solo uso. | M | [[Casos de uso#UC-01|UC-01]], [[Casos de uso#UC-02|UC-02]] |
| **RF-22** | Roles | Alumno, profesor y administrador con permisos diferenciados. | M | [[Casos de uso#UC-03|UC-03]] |
| **RF-23** | Progreso del alumno | Historial de sesiones, temas trabajados y ejercicios completados. | M | [[Casos de uso#UC-11|UC-11]] |
| **RF-24** | Seguimiento docente | Estadística agregada por módulo y tema de ejercicios comenzados/completados/abandonados, sin datos individuales; abandono al inicio vs cerca de la solución (umbral 50 % configurable); abandono por inactividad >30 min configurable o cambio de ejercicio; progreso = proporción de pasos correctos verificados con CAS (o nivel de ayuda si no hay solución). | S | [[Casos de uso#UC-10|UC-10]] |
| **RF-25** | Administración | Desactivar/anonimizar perfiles, cambiar roles, iniciar restablecimiento de contraseña, consultar uso por tema y sesiones por usuario; cada consulta individual auditada. | S | [[Casos de uso#UC-06|UC-06]], [[Casos de uso#UC-07|UC-07]], [[Casos de uso#UC-08|UC-08]], [[Casos de uso#UC-09|UC-09]] |
| **RF-26** | Registro estructurado de interacciones | Por turno: mensaje, módulo, tema, diagnóstico, nivel de ayuda, latencia, tokens. Por ejercicio: estado y progreso. | M | [[Casos de uso#UC-25|UC-25]] |
| **RF-27** | Privacidad y derechos | Aviso de privacidad, política de cookies, exportación y supresión de datos propios. | M | [[Casos de uso#UC-04|UC-04]], [[Casos de uso#UC-05|UC-05]] |
