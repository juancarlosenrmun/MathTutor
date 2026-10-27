# TFG · Tutor adaptativo LLM + visualización 3D

Bóveda de contexto del proyecto. En cada sesión basta con indicar el sprint (p. ej. «sprint 3»): el agente lee `AGENTS.md`, la nota del sprint y los requisitos enlazados.

## Navegación
- [[AGENTS]] · instrucciones base del agente
- [[Requisitos funcionales]] · [[Requisitos no funcionales]] · [[Restricciones y alcance]]
- [[Casos de uso]]
- [[Plan de sprints]] · [[Riesgos de la planificación]]
- Sprints: [[Sprint 01]] · [[Sprint 02]] · [[Sprint 03]] · [[Sprint 04]] · [[Sprint 05]] · [[Sprint 06]] · [[Sprint 07]] · [[Sprint 08]] · [[Sprint 09]] · [[Sprint 10]]
- Diseño: [[Arquitectura]] · [[Diagrama de dominio]] · [[Diagrama de clases]]
- Seguimiento: [[Progreso global]] · [[Registro de iteraciones]]

## Datos clave
- Stack: Spring Boot 3.x, JPA/Hibernate, Spring AI, PostgreSQL + pgvector, Symja (CAS, pendiente de confirmar), WebGL 2 + GLSL, SSE.
- Una sola llamada al LLM por turno; orquestador determinista (no agente).
- Capacidad supuesta: 34 puntos/sprint (recalibrar tras S1). Total 311 puntos: Must 187 (60,1 %), Should 68 (21,9 %), Could 56 (18,0 %).
- Recortables primero: RF-19, RF-18, RF-09.
