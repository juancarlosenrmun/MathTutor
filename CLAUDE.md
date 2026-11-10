@[[AGENTS]].md

## Routing (GOAT)
1. En toda tarea de código, llamar primero al agente triage.
2. Delegar en el agente de su ruta (quick, build o deep) con la tarea completa y el JSON de triage. Nunca pasar parámetro de modelo.
3. Llamar al verifier con el riesgo y los checks de triage.
4. Si el verifier da FAIL, reintentar una vez un escalón más arriba:
   - saltó un archivo o no ejecutó tests → más esfuerzo (quick → build)
   - tenía contexto completo y falló → modelo mayor (build → deep → escalate)
5. Tras cualquier escalado, pedir a triage guardar una línea: "<patrón de tarea> → <ruta que funcionó>".
6. Todos los agentes (workers y verifier) cumplen AGENTS.md y la checklist de `brain/`. El sprint activo lo indica el usuario; si no lo ha indicado, preguntarlo.