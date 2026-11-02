# AGENTS.md · Instrucciones base del agente (TFG tutor adaptativo)

## Contexto
- Proyecto: tutor virtual adaptativo de Matemáticas (Spring Boot 3.x, JPA, Spring AI, PostgreSQL+pgvector, Symja, WebGL2/GLSL).
- Bóveda de contexto: `brain/` (Obsidian). Es la fuente de verdad de requisitos, casos de uso, planificación, diseño y seguimiento.

## Inicio de sesión
1. El usuario indicará un sprint (p. ej. «sprint 3»). Leer `brain/00 - Índice.md` y `brain/04 Sprints/Sprint NN.md`.
2. Leer los RF/RNF enlazados en `brain/01 Requisitos/` y los UC/EN del sprint en `brain/02 Casos de uso/Casos de uso.md`.
3. Leer `brain/06 Diseño/` (dominio y clases) y `brain/05 Seguimiento/Registro de iteraciones.md` para saber qué está hecho.
4. No pedir contexto que ya esté en la bóveda.

## Checklist obligatoria (en CADA petición)
Al terminar cada petición, sin que se pida:
1. Crear o actualizar la checklist del sprint en `brain/04 Sprints/Sprint NN.md`: marcar `[x]` solo lo realmente implementado y verificado (implementación, caja blanca, caja negra, cobertura 100 %, integrado). No marcar nada sin evidencia (tests en verde, informe de cobertura).
2. Actualizar la tabla de `brain/05 Seguimiento/Progreso global.md` (hechos/total y estado) y el campo `estado` del frontmatter del sprint (`pendiente` / `en curso` / `completado`).
3. Añadir arriba una entrada en `brain/05 Seguimiento/Registro de iteraciones.md` con: fecha y hora, sprint, petición, lo implementado, nº de pruebas (caja blanca/negra), cobertura y pendiente.
4. Si el código cambia el diseño, actualizar `Diagrama de dominio.md` y `Diagrama de clases.md` en la misma petición.
5. Si algo no se pudo completar, dejarlo sin marcar y anotar el motivo en «Notas de iteración» del sprint.
6. Cerrar la respuesta con el estado de la checklist (hechos/total del sprint).

## Alcance
- Implementar solo los UC/EN del sprint activo. No adelantar trabajo de otros sprints salvo que sea imprescindible; en ese caso anotarlo en las notas del sprint.
- Respetar restricciones: una sola llamada al LLM por turno, orquestador determinista, sin transacciones abiertas durante la llamada al LLM, interfaz y tutor en español, claves fuera del repositorio.

## Pruebas y calidad
- Cada UC/EN con pruebas de caja blanca (cobertura de líneas y ramas, caminos y condiciones) y de caja negra (partición de equivalencia, valores límite, tablas de decisión, pruebas de API/contrato).
- Objetivo de cobertura: 100 % de líneas y ramas del código del sprint, medido con JaCoCo (backend) y la herramienta equivalente en cliente JS. Build falla por debajo del umbral. Exclusiones solo para código generado o de arranque, listadas y justificadas en el informe.
- Pruebas deterministas: el LLM y Symja se simulan en pruebas unitarias; pruebas de integración con Testcontainers (PostgreSQL+pgvector).
- Elemento hecho = criterios de aceptación cumplidos + pruebas automatizadas + integrado en la rama principal.
- Git: commits pequeños por UC/EN, rama por sprint, CI en verde antes de integrar.

## Estilo
- Respuestas breves, resultados directos. Código y documentación en español; identificadores en inglés.
- Ante conflicto entre este archivo y una petición puntual del usuario, prevalece la petición, y se anota en el registro.
