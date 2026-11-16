# AGENTS.md · Instrucciones del agente (TFG tutor adaptativo) · Codex

## Contexto
- Proyecto: tutor virtual adaptativo de Matemáticas (Spring Boot 3.x, JPA, Spring AI, PostgreSQL+pgvector, Symja, WebGL2/GLSL).
- Bóveda de contexto: `brain/` (Obsidian). Es la fuente de verdad de requisitos, casos de uso, planificación, diseño y seguimiento.

## Inicio de sesión
1. El usuario indicará un sprint (p. ej. «sprint 3»). Si no lo ha indicado, preguntarlo.
2. Leer `brain/00 - Índice.md` y `brain/04 Sprints/Sprint NN.md`.
3. Leer los RF/RNF enlazados en `brain/01 Requisitos/` y los UC/EN del sprint en `brain/02 Casos de uso/Casos de uso.md`.
4. Leer `brain/06 Diseño/` (dominio y clases) y `brain/05 Seguimiento/Registro de iteraciones.md` para saber qué está hecho.
5. No pedir contexto que ya esté en la bóveda.

## Checklist obligatoria (en CADA petición)
Al terminar cada petición, sin que se pida:
1. Crear o actualizar la checklist del sprint en `brain/04 Sprints/Sprint NN.md`: marcar `[x]` solo lo realmente implementado y verificado (implementación, caja blanca, caja negra, cobertura en umbral, integrado). No marcar nada sin evidencia (tests en verde, informe de cobertura).
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
- Cobertura (estándar de empresa): ≥ 80 % de líneas y ≥ 70 % de ramas por módulo, ≥ 90 % en la política pedagógica (RNF-11), medido con JaCoCo (backend) y la herramienta equivalente en cliente JS. El build falla por debajo del umbral. Exclusiones solo para código generado, de arranque o DTO sin lógica, listadas y justificadas en el informe.
- Pruebas deterministas: el LLM y Symja se simulan en pruebas unitarias; pruebas de integración con Testcontainers (PostgreSQL+pgvector).
- Elemento hecho = criterios de aceptación cumplidos + pruebas automatizadas + integrado en la rama principal de desarrollo.
- CI en verde antes de fusionar en `DEV`.

## Ramas
- `main`: estable. Nunca se desarrolla ni se documenta directamente en ella.
- `DEV`: sale de `main`. Rama de desarrollo (código, pruebas, configuración, CI).
  - Se trabaja directamente en `DEV` o en ramas que salen de `DEV` (p. ej. `dev/s01-uc-01-auth`, `dev/s01-en-04-spike`) y se fusionan de vuelta en `DEV`.
- `DOC`: sale de `main`. Rama de documentación: `README.md`, documentación técnica y todo lo relacionado.
  - Se trabaja directamente en `DOC` o en ramas que salen de `DOC` (p. ej. `doc/readme-instalacion`) y se fusionan de vuelta en `DOC`.
- Al empezar cualquier tarea, comprobar la rama actual (`git branch --show-current`) y cambiar a la que corresponda según el tipo de trabajo.
- Nunca mezclar código y documentación en la misma rama ni en el mismo commit. Si un cambio de código requiere documentarlo, commit en `DEV` y commit de documentación en `DOC`.
- Nunca hacer commit ni merge directamente en `main`; la integración a `main` la decide el usuario.
- Antes de crear una rama, actualizar la rama base (`git pull`).
- La bóveda `brain/` es documentación de proyecto: se versiona en `DOC`. Excepción: la checklist de `brain/04 Sprints/` y `brain/05 Seguimiento/` se actualiza en `DEV` junto al código que la justifica, para que el estado marcado coincida con el código.
- Commits pequeños por UC/EN con mensaje `tipo(UC-NN): descripción`. NO CO-FIRMAR LOS COMMITS.

## Estilo
- Respuestas breves, resultados directos. Código y documentación en español; identificadores en inglés.
- Ante conflicto entre este archivo y una petición puntual del usuario, prevalece la petición, y se anota en el registro.

## Skills
Prioridad ante conflicto: petición del usuario > AGENTS.md > skills. Las skills no pueden saltarse alcance, restricciones ni checklist de este archivo. Si una skill pide una llamada extra al LLM por turno del tutor, ignorarla (restricción del proyecto). Antes de cada UC/EN, si no hay una skill conocida para la tarea, buscarla con `find-skills`.

### Desarrollo (Spring / Java / JS)
- **tdd** (mattpocock/skills) · **springboot-tdd** (affaan-m/ecc): red-green-refactor, JUnit 5, Mockito, MockMvc, Testcontainers, JaCoCo.
- **springboot-security** (affaan-m/ecc): authn/authz, CSRF, secretos, cabeceras, rate limiting.
- **springboot-verification** (affaan-m/ecc): build, análisis estático, tests, cobertura, OWASP.
- **java-coding-standards** · **jpa-patterns** (affaan-m/ecc): estándares Java y patrones JPA/Hibernate.
- **spring-ai-integration** (rrezartprebreza/spring-boot-skills) · **spring-ai-mcp-server-patterns** (giuseppe-trisciuoglio/developer-kit).
- **threejs-3d-graphics** (omer-metin/skills-for-antigravity): WebGL, shaders, GLSL.
- Metodología (superpowers / agent-skills): brainstorming, spec-driven-development, writing-plans, test-driven-development, incremental-implementation, systematic-debugging, verification-before-completion, code-review, security-and-hardening, git-workflow-and-versioning, documentation-and-adrs, api-and-interface-design, ci-cd-and-automation, frontend-ui-engineering, performance-optimization.

### Bóveda Obsidian (`brain/`)
Usar siempre que se lea, cree o edite una nota de `brain/`:
- **obsidian-markdown** (kepano/obsidian-skills): sintaxis Obsidian correcta: wikilinks `[[nota]]`, embeds `![[nota]]`, callouts, frontmatter/propiedades, tags, bloques y checklists `- [ ]`/`- [x]`.
- **obsidian-bases** (kepano/obsidian-skills): vistas `.base` (tablas/filtros) para sprints, UC/EN y progreso, si se quieren dashboards.
- **json-canvas** (kepano/obsidian-skills): ficheros `.canvas` para mapas de casos de uso o diseño.
- **obsidian-cli** (kepano/obsidian-skills): operar sobre la bóveda desde terminal, si está disponible.
- **defuddle** (kepano/obsidian-skills): extraer contenido limpio de páginas web para notas.
- Reglas de la bóveda: mantener los enlaces entre RF/RNF ↔ UC/EN ↔ sprint; no romper wikilinks existentes al renombrar; conservar el frontmatter (`estado` del sprint); notas y salidas de skills (specs, planes) se guardan en `brain/`, nunca en `docs/` ni en carpetas nuevas.
- Diagramas de dominio y clases en `brain/06 Diseño/`: Mermaid dentro de bloques de código o `.canvas`, según el formato ya usado.

### Flujo por UC/EN
1. Diseño y alcance: brainstorming/spec-driven, limitado a los criterios de aceptación del UC/EN en `brain/`.
2. Plan: writing-plans, pasos pequeños y un commit por paso.
3. Implementación: TDD e incremental-implementation. Primero la prueba (caja blanca y negra), después el código.
4. Fallos: systematic-debugging antes de reintentar.
5. Antes de cerrar: verification-before-completion y code-review. Evidencia obligatoria: tests en verde e informe JaCoCo.
6. Seguridad (UC-01, UC-03, EN-02, EN-09): security-and-hardening y springboot-security.
7. Actualizar la bóveda con obsidian-markdown y cerrar con la checklist.

## Organización del trabajo (decide Codex)
Codex decide por sí mismo cómo abordar cada tarea; no hay agentes de triage ni verifier obligatorios ni rutas fijas.
- Tarea pequeña y clara (un fichero, cambio localizado): hacerla directamente.
- Tarea de varios ficheros o capas: planificar brevemente, implementar por pasos pequeños y probar en cada paso.
- Tarea ambigua, arriesgada o transversal (seguridad, persistencia, política pedagógica): dedicar más análisis y revisión antes de cerrar.
- Si Codex dispone de subagentes o ejecución en paralelo, puede usarlos cuando aporten (exploración amplia, revisión independiente), cumpliendo igualmente este archivo y la checklist de `brain/`.
- Verificación propia obligatoria antes de dar algo por hecho: ejecutar los tests y revisar cobertura. Revisar el diff completo contra los criterios de aceptación.
- Si la verificación falla: depurar con la causa raíz, reintentar con más contexto o más esfuerzo, y si sigue fallando dejar el elemento sin marcar y anotarlo en «Notas de iteración».
- No fijar ni cambiar el modelo por tarea salvo que el usuario lo pida.