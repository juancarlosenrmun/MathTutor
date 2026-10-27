# Casos de uso

Esfuerzo en Fibonacci (1,3,5,8,13,21). UC = funcional, EN = habilitador técnico.

| ID | Caso de uso | Actor | Requisitos | Esf. | MoSCoW | Sprint |
|---|---|---|---|---|---|---|
| **UC-01** | Registrarse, iniciar y cerrar sesión | Alumno, profesor | RF-21 | 5 | Must | [[Sprint 01\|S1]] |
| **UC-02** | Restablecer contraseña por enlace de un solo uso | Usuario | RF-21 | 5 | Should | [[Sprint 07\|S7]] |
| **UC-03** | Controlar el acceso por rol y recurso | Sistema | RF-22, RNF-04 | 5 | Must | [[Sprint 01\|S1]] |
| **UC-04** | Aceptar aviso de privacidad y gestionar cookies | Usuario | RF-27, RNF-06 | 3 | Must | [[Sprint 03\|S3]] |
| **UC-05** | Exportar y suprimir mis datos | Usuario | RF-27 | 5 | Should | [[Sprint 08\|S8]] |
| **UC-06** | Desactivar o anonimizar un perfil | Administrador | RF-25 | 5 | Should | [[Sprint 08\|S8]] |
| **UC-07** | Cambiar el rol de un usuario | Administrador | RF-25, RNF-04 | 3 | Could | [[Sprint 08\|S8]] |
| **UC-08** | Iniciar el restablecimiento de contraseña de un usuario | Administrador | RF-25 | 3 | Could | [[Sprint 08\|S8]] |
| **UC-09** | Consultar uso por tema y sesiones por usuario, con auditoría | Administrador | RF-25, RNF-13 | 5 | Could | [[Sprint 09\|S9]] |
| **UC-10** | Consultar estadística de ejercicios comenzados, completados y abandonados | Profesor | RF-24 | 8 | Should | [[Sprint 08\|S8]] |
| **UC-11** | Consultar mi progreso e historial | Alumno | RF-23 | 5 | Must | [[Sprint 04\|S4]] |
| **UC-12** | Seleccionar módulo e iniciar sesión | Alumno | RF-02 | 3 | Must | [[Sprint 02\|S2]] |
| **UC-13** | Conversar con el tutor con respuesta en streaming | Alumno | RF-01 | 8 | Must | [[Sprint 02\|S2]] |
| **UC-14** | Indexar los apuntes LaTeX al arrancar | Sistema | RF-03 | 13 | Must | [[Sprint 02\|S2]] |
| **UC-15** | Recuperar fragmentos de apuntes por módulo y tema | Sistema | RF-03 | 5 | Must | [[Sprint 03\|S3]] |
| **UC-16** | Interpretar la expresión del alumno | Sistema | RF-04 | 8 | Must | [[Sprint 03\|S3]] |
| **UC-17** | Verificar pasos y resultado con el CAS y clasificar el error | Sistema | RF-05 | 13 | Must | [[Sprint 04\|S4]] |
| **UC-18** | Guiar con pistas socráticas progresivas | Sistema | RF-07 | 13 | Must | [[Sprint 05\|S5]] |
| **UC-19** | Impedir la revelación de la solución | Sistema | RF-08 | 8 | Must | [[Sprint 05\|S5]] |
| **UC-20** | Mantener el contexto de la sesión | Sistema | RF-10 | 5 | Must | [[Sprint 04\|S4]] |
| **UC-21** | Filtrar mensajes inadecuados y aplicar cuota | Sistema | RF-11, RNF-09 | 8 | Should | [[Sprint 06\|S6]] |
| **UC-22** | Cargar el banco de ejercicios orientativo | Sistema | RF-06 | 5 | Must | [[Sprint 03\|S3]] |
| **UC-23** | Reconocer ejercicio del banco y usar sus datos | Sistema | RF-12 | 8 | Should | [[Sprint 06\|S6]] |
| **UC-24** | Aplicar la progresión pedagógica | Sistema | RF-09 | 5 | Could | [[Sprint 09\|S9]] |
| **UC-25** | Registrar cada interacción de forma estructurada | Sistema | RF-26, RNF-13 | 5 | Must | [[Sprint 04\|S4]] |
| **UC-26** | Degradar a modo solo apuntes si falla el LLM | Sistema | RNF-07 | 5 | Could | [[Sprint 09\|S9]] |
| **UC-27** | Abrir el visor WebGL con pipeline y cámara | Alumno | RF-13 | 13 | Must | [[Sprint 01\|S1]] |
| **UC-28** | Ver gráficas 2D de la función, su derivada y el área bajo la curva | Alumno | RF-14 | 8 | Must | [[Sprint 02\|S2]] |
| **UC-29** | Ver superficies con malla, triangulación y normales | Alumno | RF-15 | 13 | Must | [[Sprint 03\|S3]] |
| **UC-30** | Ver superficies con iluminación y texturas | Alumno | RF-15 | 8 | Should | [[Sprint 06\|S6]] |
| **UC-31** | Ver rectas y planos tangentes dinámicos | Alumno | RF-16 | 8 | Must | [[Sprint 05\|S5]] |
| **UC-32** | Seleccionar puntos por intersección rayo–superficie y recta–plano | Alumno | RF-17 | 13 | Should | [[Sprint 07\|S7]] |
| **UC-33** | Ver animaciones (secante a tangente, cámara) | Alumno | RF-18 | 8 | Could | [[Sprint 09\|S9]] |
| **UC-34** | Aplicar post-procesamiento | Alumno | RF-19 | 13 | Could | [[Sprint 10\|S10]] |
| **UC-35** | Generar y validar la especificación gráfica | Sistema | RF-20 | 5 | Must | [[Sprint 04\|S4]] |
| **EN-01** | Monolito modular Spring Boot, BD con pgvector, Git y CI | — | RNF-11 | 5 | Must | [[Sprint 01\|S1]] |
| **EN-02** | Seguridad transversal (CSRF, sesión, TLS, secretos, ZAP) | — | RNF-05 | 8 | Must | [[Sprint 06\|S6]] |
| **EN-03** | Logs estructurados y métricas de latencia y tokens | — | RNF-13 | 3 | Could | [[Sprint 09\|S9]] |
| **EN-04** | Spike: conversión de LaTeX a Symja y Spring AI con pgvector | — | RNF-11 | 5 | Must | [[Sprint 01\|S1]] |
| **EN-05** | Evaluación pedagógica por categorías A–D | — | RNF-08 | 13 | Must | [[Sprint 07\|S7]] |
| **EN-06** | Pruebas de rendimiento y carga | — | RNF-01, 02, 03 | 8 | Could | [[Sprint 10\|S10]] |
| **EN-07** | Accesibilidad, teclado, alternativa textual y navegadores | — | RNF-10, 12 | 8 | Should | [[Sprint 08\|S8]] |
| **EN-08** | Independencia del proveedor LLM | — | RNF-14 | 3 | Could | [[Sprint 09\|S9]] |
| **EN-09** | Cumplimiento RGPD (contrato con el proveedor, registro de tratamientos, DPD) | — | RNF-06 | 5 | Must | [[Sprint 05\|S5]] |

## UC-01
Registrarse, iniciar y cerrar sesión — 5 pts, Must, S1. Requisitos: RF-21.

## UC-02
Restablecer contraseña por enlace de un solo uso — 5 pts, Should, S7. Requisitos: RF-21.

## UC-03
Controlar el acceso por rol y recurso — 5 pts, Must, S1. Requisitos: RF-22, RNF-04.

## UC-04
Aceptar aviso de privacidad y gestionar cookies — 3 pts, Must, S3. Requisitos: RF-27, RNF-06.

## UC-05
Exportar y suprimir mis datos — 5 pts, Should, S8. Requisitos: RF-27.

## UC-06
Desactivar o anonimizar un perfil — 5 pts, Should, S8. Requisitos: RF-25.

## UC-07
Cambiar el rol de un usuario — 3 pts, Could, S8. Requisitos: RF-25, RNF-04.

## UC-08
Iniciar el restablecimiento de contraseña de un usuario — 3 pts, Could, S8. Requisitos: RF-25.

## UC-09
Consultar uso por tema y sesiones por usuario, con auditoría — 5 pts, Could, S9. Requisitos: RF-25, RNF-13.

## UC-10
Consultar estadística de ejercicios comenzados, completados y abandonados — 8 pts, Should, S8. Requisitos: RF-24.

## UC-11
Consultar mi progreso e historial — 5 pts, Must, S4. Requisitos: RF-23.

## UC-12
Seleccionar módulo e iniciar sesión — 3 pts, Must, S2. Requisitos: RF-02.

## UC-13
Conversar con el tutor con respuesta en streaming — 8 pts, Must, S2. Requisitos: RF-01.

## UC-14
Indexar los apuntes LaTeX al arrancar — 13 pts, Must, S2. Requisitos: RF-03.

## UC-15
Recuperar fragmentos de apuntes por módulo y tema — 5 pts, Must, S3. Requisitos: RF-03.

## UC-16
Interpretar la expresión del alumno — 8 pts, Must, S3. Requisitos: RF-04.

## UC-17
Verificar pasos y resultado con el CAS y clasificar el error — 13 pts, Must, S4. Requisitos: RF-05.

## UC-18
Guiar con pistas socráticas progresivas — 13 pts, Must, S5. Requisitos: RF-07.

## UC-19
Impedir la revelación de la solución — 8 pts, Must, S5. Requisitos: RF-08.

## UC-20
Mantener el contexto de la sesión — 5 pts, Must, S4. Requisitos: RF-10.

## UC-21
Filtrar mensajes inadecuados y aplicar cuota — 8 pts, Should, S6. Requisitos: RF-11, RNF-09.

## UC-22
Cargar el banco de ejercicios orientativo — 5 pts, Must, S3. Requisitos: RF-06.

## UC-23
Reconocer ejercicio del banco y usar sus datos — 8 pts, Should, S6. Requisitos: RF-12.

## UC-24
Aplicar la progresión pedagógica — 5 pts, Could, S9. Requisitos: RF-09.

## UC-25
Registrar cada interacción de forma estructurada — 5 pts, Must, S4. Requisitos: RF-26, RNF-13.

## UC-26
Degradar a modo solo apuntes si falla el LLM — 5 pts, Could, S9. Requisitos: RNF-07.

## UC-27
Abrir el visor WebGL con pipeline y cámara — 13 pts, Must, S1. Requisitos: RF-13.

## UC-28
Ver gráficas 2D de la función, su derivada y el área bajo la curva — 8 pts, Must, S2. Requisitos: RF-14.

## UC-29
Ver superficies con malla, triangulación y normales — 13 pts, Must, S3. Requisitos: RF-15.

## UC-30
Ver superficies con iluminación y texturas — 8 pts, Should, S6. Requisitos: RF-15.

## UC-31
Ver rectas y planos tangentes dinámicos — 8 pts, Must, S5. Requisitos: RF-16.

## UC-32
Seleccionar puntos por intersección rayo–superficie y recta–plano — 13 pts, Should, S7. Requisitos: RF-17.

## UC-33
Ver animaciones (secante a tangente, cámara) — 8 pts, Could, S9. Requisitos: RF-18.

## UC-34
Aplicar post-procesamiento — 13 pts, Could, S10. Requisitos: RF-19.

## UC-35
Generar y validar la especificación gráfica — 5 pts, Must, S4. Requisitos: RF-20.

## EN-01
Monolito modular Spring Boot, BD con pgvector, Git y CI — 5 pts, Must, S1. Requisitos: RNF-11.

## EN-02
Seguridad transversal (CSRF, sesión, TLS, secretos, ZAP) — 8 pts, Must, S6. Requisitos: RNF-05.

## EN-03
Logs estructurados y métricas de latencia y tokens — 3 pts, Could, S9. Requisitos: RNF-13.

## EN-04
Spike: conversión de LaTeX a Symja y Spring AI con pgvector — 5 pts, Must, S1. Requisitos: RNF-11.

## EN-05
Evaluación pedagógica por categorías A–D — 13 pts, Must, S7. Requisitos: RNF-08.

## EN-06
Pruebas de rendimiento y carga — 8 pts, Could, S10. Requisitos: RNF-01, 02, 03.

## EN-07
Accesibilidad, teclado, alternativa textual y navegadores — 8 pts, Should, S8. Requisitos: RNF-10, 12.

## EN-08
Independencia del proveedor LLM — 3 pts, Could, S9. Requisitos: RNF-14.

## EN-09
Cumplimiento RGPD (contrato con el proveedor, registro de tratamientos, DPD) — 5 pts, Must, S5. Requisitos: RNF-06.
